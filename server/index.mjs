import http from "node:http";
import os from "node:os";
import { WebSocketServer } from "ws";
import { RoomStore, RoomError } from "./rooms.mjs";
import { MAX_PAYLOAD, PROTO, errorMessage, parseMessage, validName } from "./protocol.mjs";

const PORT = Number(process.env.PORT || 8080);
const store = new RoomStore();
const sockets = new Map(); // playerId -> ws

const server = http.createServer((req, res) => {
  if (req.url === "/health" || req.url === "/api/config") {
    res.writeHead(200, { "content-type": "application/json", "access-control-allow-origin": "*" });
    res.end(JSON.stringify({ ok: true, proto: PROTO, rooms: store.rooms.size, now: Date.now() }));
    return;
  }
  res.writeHead(404, { "content-type": "text/plain" });
  res.end("kto-ya-univer room server");
});

const wss = new WebSocketServer({ server, path: "/ws", maxPayload: MAX_PAYLOAD });

function send(ws, obj) {
  if (ws && ws.readyState === 1) ws.send(JSON.stringify(obj));
}

function broadcast(room) {
  const snap = store.snapshot(room);
  for (const p of room.players) send(sockets.get(p.id), snap);
}

wss.on("connection", (ws) => {
  ws.playerId = null;
  ws.roomCode = null;
  send(ws, { type: "hello_ok", serverTime: Date.now(), proto: PROTO });

  ws.on("message", (raw) => {
    const msg = parseMessage(raw.toString());
    if (!msg) return send(ws, errorMessage("BAD_MESSAGE", "cannot parse"));
    try {
      switch (msg.type) {
        case "hello": {
          if (msg.proto !== PROTO) return send(ws, errorMessage("BAD_VERSION", "proto mismatch"));
          break;
        }
        case "create": {
          const { room, player } = store.create({
            name: msg.name, deckSize: msg.deckSize, deckHash: msg.deckHash
          });
          ws.playerId = player.id;
          ws.roomCode = room.code;
          sockets.set(player.id, ws);
          send(ws, { type: "joined", code: room.code, playerId: player.id, token: player.token, hostId: room.hostId });
          broadcast(room);
          break;
        }
        case "join": {
          if (!validName(msg.name)) return send(ws, errorMessage("BAD_NAME", "bad name"));
          const { room, player } = store.join(msg.code, msg.name);
          ws.playerId = player.id;
          ws.roomCode = room.code;
          sockets.set(player.id, ws);
          send(ws, { type: "joined", code: room.code, playerId: player.id, token: player.token, hostId: room.hostId });
          broadcast(room);
          break;
        }
        case "rejoin": {
          const { room, player } = store.rejoin(msg.code, msg.playerId, msg.token);
          ws.playerId = player.id;
          ws.roomCode = room.code;
          sockets.set(player.id, ws);
          send(ws, { type: "joined", code: room.code, playerId: player.id, token: player.token, hostId: room.hostId });
          broadcast(room);
          break;
        }
        case "ready": {
          const room = store.setReady(ws.roomCode, ws.playerId, msg.ready);
          broadcast(room);
          break;
        }
        case "start": {
          const room = store.start(ws.roomCode, ws.playerId, { showMs: msg.showMs, alternateKinds: msg.alternateKinds, filter: msg.filter });
          broadcast(room);
          for (const p of room.players) send(sockets.get(p.id), store.countdownPayload(room));
          break;
        }
        case "result": {
          const room = store.setResult(ws.roomCode, ws.playerId, msg.round, msg.value);
          broadcast(room);
          break;
        }
        case "reveal": {
          const room = store.reveal(ws.roomCode, ws.playerId);
          broadcast(room);
          break;
        }
        case "next_round": {
          const room = store.nextRound(ws.roomCode, ws.playerId);
          broadcast(room);
          for (const p of room.players) send(sockets.get(p.id), store.countdownPayload(room));
          break;
        }
        case "leave": {
          const room = store.leave(ws.roomCode, ws.playerId);
          if (room) broadcast(room);
          sockets.delete(ws.playerId);
          ws.playerId = null;
          ws.roomCode = null;
          break;
        }
        case "ping": {
          store.ping(ws.roomCode, ws.playerId);
          send(ws, { type: "pong", t: msg.t });
          break;
        }
        default:
          send(ws, errorMessage("BAD_MESSAGE", `unknown type ${msg.type}`));
      }
    } catch (e) {
      if (e instanceof RoomError) send(ws, errorMessage(e.code, e.message));
      else send(ws, errorMessage("SERVER_ERROR", String(e && e.message ? e.message : e)));
    }
  });

  ws.on("close", () => {
    if (ws.playerId) {
      const room = store.leave(ws.roomCode, ws.playerId);
      sockets.delete(ws.playerId);
      if (room) broadcast(room);
    }
  });
});

setInterval(() => store.sweep(), 15_000).unref?.();

server.listen(PORT, "0.0.0.0", () => {
  console.log(`[kto-ya] room server on http://0.0.0.0:${PORT}  ws://<ip>:${PORT}/ws`);
  const nets = os.networkInterfaces();
  for (const [name, list] of Object.entries(nets)) {
    for (const n of list || []) {
      if (n.family === "IPv4" && !n.internal) console.log(`  LAN ${name}: ws://${n.address}:${PORT}/ws`);
    }
  }
});
