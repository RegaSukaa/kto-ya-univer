import { randomInt, randomUUID } from "node:crypto";
import {
  CODE_ALPHABET, MAX_PLAYERS, ROOM_TTL_MS, OFFLINE_MS, validName
} from "./protocol.mjs";

export class RoomError extends Error {
  constructor(code, message = code) {
    super(message);
    this.code = code;
  }
}

function normalizeName(name) {
  return name.trim().toLowerCase();
}

export class RoomStore {
  constructor(now = () => Date.now()) {
    this.rooms = new Map();
    this.now = now;
  }

  newCode() {
    for (let attempt = 0; attempt < 5000; attempt++) {
      let code = "";
      for (let i = 0; i < 6; i++) code += CODE_ALPHABET[randomInt(CODE_ALPHABET.length)];
      if (!this.rooms.has(code)) return code;
    }
    throw new Error("could not allocate room code");
  }

  player(name, isHost) {
    return {
      id: randomUUID(),
      token: randomUUID(),
      name: name.trim(),
      isHost,
      ready: false,
      online: true,
      lastPing: this.now(),
      joinedAt: this.now()
    };
  }

  create({ name, deckSize = 0, deckHash = "" }) {
    if (!validName(name)) throw new RoomError("BAD_NAME");
    const host = this.player(name, true);
    const room = {
      code: this.newCode(),
      hostId: host.id,
      phase: "lobby",
      players: [host],
      round: 0,
      deckSize,
      deckHash,
      seed: 0,
      showMs: 10000,
      startAt: 0,
      results: {},
      createdAt: this.now(),
      lastActivity: this.now()
    };
    this.rooms.set(room.code, room);
    return { room, player: host };
  }

  get(code) {
    const room = this.rooms.get(String(code || "").toUpperCase());
    if (!room) throw new RoomError("ROOM_NOT_FOUND", "room not found");
    if (this.now() - room.createdAt > ROOM_TTL_MS) {
      this.rooms.delete(room.code);
      throw new RoomError("ROOM_NOT_FOUND", "room expired");
    }
    return room;
  }

  join(code, name) {
    if (!validName(name)) throw new RoomError("BAD_NAME");
    const room = this.get(code);
    if (room.phase !== "lobby") throw new RoomError("ALREADY_STARTED");
    if (room.players.length >= MAX_PLAYERS) throw new RoomError("ROOM_FULL");
    if (room.players.some((p) => normalizeName(p.name) === normalizeName(name))) {
      throw new RoomError("NAME_TAKEN");
    }
    const player = this.player(name, false);
    room.players.push(player);
    room.lastActivity = this.now();
    return { room, player };
  }

  rejoin(code, playerId, token) {
    const room = this.get(code);
    const player = room.players.find((p) => p.id === playerId);
    if (!player || player.token !== token) throw new RoomError("ROOM_NOT_FOUND");
    player.online = true;
    player.lastPing = this.now();
    return { room, player };
  }

  setReady(code, playerId, ready) {
    const room = this.get(code);
    const p = room.players.find((x) => x.id === playerId);
    if (!p) throw new RoomError("ROOM_NOT_FOUND");
    p.ready = !!ready;
    return room;
  }

  start(code, playerId, { showMs = 10000, alternateKinds = true } = {}) {
    const room = this.get(code);
    if (room.hostId !== playerId) throw new RoomError("NOT_HOST");
    if (room.phase !== "lobby") throw new RoomError("ALREADY_STARTED");
    if (room.players.length < 2) throw new RoomError("NOT_ENOUGH_PLAYERS");
    room.phase = "countdown";
    room.round = 1;
    room.showMs = Math.max(1000, Math.min(60000, Number(showMs) || 10000));
    room.alternateKinds = !!alternateKinds;
    room.seed = Math.floor(Math.random() * 2 ** 48);
    room.startAt = this.now() + 1500;
    room.results = {};
    room.lastActivity = this.now();
    return room;
  }

  setResult(code, playerId, round, value) {
    const room = this.get(code);
    if (!["guessed", "missed", "skipped"].includes(value)) throw new RoomError("BAD_VALUE");
    if (room.phase === "lobby" || room.phase === "closed") throw new RoomError("ALREADY_STARTED");
    const player = room.players.find((p) => p.id === playerId);
    if (!player) throw new RoomError("ROOM_NOT_FOUND");
    room.results[playerId] = value;
    room.lastActivity = this.now();
    return room;
  }

  allDone(room) {
    return room.players.every((p) => room.results[p.id] !== undefined);
  }

  reveal(code, playerId) {
    const room = this.get(code);
    if (room.hostId !== playerId) throw new RoomError("NOT_HOST");
    room.phase = "reveal";
    room.lastActivity = this.now();
    return room;
  }

  nextRound(code, playerId) {
    const room = this.get(code);
    if (room.hostId !== playerId) throw new RoomError("NOT_HOST");
    if (room.phase === "closed") throw new RoomError("ROOM_NOT_FOUND");
    room.phase = "countdown";
    room.round += 1;
    room.seed = Math.floor(Math.random() * 2 ** 48);
    room.startAt = this.now() + 1500;
    room.results = {};
    room.lastActivity = this.now();
    return room;
  }

  leave(code, playerId) {
    const room = this.rooms.get(String(code || "").toUpperCase());
    if (!room) return null;
    const idx = room.players.findIndex((p) => p.id === playerId);
    if (idx < 0) return room;
    const [gone] = room.players.splice(idx, 1);
    if (room.hostId === gone.id) {
      if (room.phase === "lobby" && room.players.length > 0) {
        const next = room.players.slice().sort((a, b) => a.joinedAt - b.joinedAt)[0];
        next.isHost = true;
        room.hostId = next.id;
      } else {
        room.hostLeftAt = this.now();
      }
    }
    if (room.players.length === 0) this.rooms.delete(room.code);
    return room;
  }

  ping(code, playerId) {
    const room = this.rooms.get(String(code || "").toUpperCase());
    if (!room) return null;
    const p = room.players.find((x) => x.id === playerId);
    if (!p) return null;
    p.lastPing = this.now();
    p.online = true;
    return room;
  }

  sweep() {
    const now = this.now();
    for (const [code, room] of this.rooms) {
      if (now - room.createdAt > ROOM_TTL_MS) {
        this.rooms.delete(code);
        continue;
      }
      for (const p of room.players) {
        if (p.online && now - p.lastPing > OFFLINE_MS) p.online = false;
      }
      if (room.phase !== "lobby" && room.hostLeftAt != null && now - room.hostLeftAt > 60_000) {
        room.phase = "closed";
        this.rooms.delete(code);
      }
    }
    return this.rooms.size;
  }

  snapshot(room) {
    return {
      type: "room",
      code: room.code,
      hostId: room.hostId,
      phase: room.phase,
      players: room.players.map((p) => ({ id: p.id, name: p.name, ready: p.ready, online: p.online })),
      round: room.round,
      deckSize: room.deckSize,
      deckHash: room.deckHash,
      seed: room.seed,
      showMs: room.showMs,
      startAt: room.startAt,
      results: room.results
    };
  }

  countdownPayload(room) {
    return {
      type: "countdown",
      startAt: room.startAt,
      showMs: room.showMs,
      round: room.round,
      seed: room.seed,
      order: []
    };
  }
}
