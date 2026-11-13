// Ручной smoke онлайн-режима: поднимает сервер и проходит сценарий двумя клиентами.
// Запуск: node smoke.mjs
import { spawn } from "node:child_process";
import WebSocket from "ws";

const PORT = 8099;
const URL = `ws://127.0.0.1:${PORT}/ws`;

const server = spawn(process.execPath, ["index.mjs"], {
  env: { ...process.env, PORT: String(PORT) },
  stdio: ["ignore", "pipe", "pipe"]
});
server.stdout.on("data", (d) => process.stdout.write("[server] " + d));
server.stderr.on("data", (d) => process.stderr.write("[server:err] " + d));

const wait = (ms) => new Promise((r) => setTimeout(r, ms));

function client() {
  const ws = new WebSocket(URL);
  const inbox = [];
  ws.on("message", (raw) => inbox.push(JSON.parse(raw.toString())));
  return new Promise((resolve) => ws.on("open", () => resolve({
    ws,
    inbox,
    send: (obj) => ws.send(JSON.stringify(obj)),
    last: (type) => [...inbox].reverse().find((m) => m.type === type)
  })));
}

const results = [];
const check = (name, ok, extra = "") => {
  results.push({ name, ok });
  console.log(`${ok ? "OK  " : "FAIL"} ${name} ${extra}`);
};

try {
  await wait(700);
  const host = await client();
  const guest = await client();
  host.send({ type: "hello", proto: 1, clientId: "smoke-host", appVersion: "1.0", lang: "ru" });
  guest.send({ type: "hello", proto: 1, clientId: "smoke-guest", appVersion: "1.0", lang: "ru" });
  await wait(200);
  check("hello_ok", !!host.last("hello_ok") && !!guest.last("hello_ok"));

  host.send({ type: "create", name: "Рустам", deckSize: 12, deckHash: "sha256:test" });
  await wait(200);
  const joined = host.last("joined");
  check("create returns 6-char code", !!joined && /^[A-HJ-NP-Z2-9]{6}$/.test(joined.code), joined?.code);
  const code = joined.code;

  guest.send({ type: "join", code, name: "Аня" });
  await wait(200);
  check("join works", !!guest.last("joined"));
  const room = host.last("room");
  check("lobby has two players", room?.players.length === 2);

  guest.send({ type: "join", code, name: "аня" });
  await wait(100);
  // второй клиент уже в комнате; проверяем дубликат через unit-тест, здесь — ошибку имени отнимает сервер
  const dupErr = guest.inbox.find((m) => m.type === "error" && m.code === "NAME_TAKEN");
  check("duplicate name rejected", !!dupErr);

  guest.send({ type: "start", showMs: 10000 });
  await wait(100);
  check("guest cannot start", !!guest.inbox.find((m) => m.type === "error" && m.code === "NOT_HOST"));

  host.send({ type: "start", showMs: 10000, alternateKinds: true });
  await wait(300);
  const cd = host.last("countdown");
  check("countdown delivered", !!cd && cd.startAt > Date.now() - 1000 && typeof cd.seed === "number");
  check("countdown has no photos", cd && JSON.stringify(cd).indexOf("photo") === -1);

  host.send({ type: "result", round: 1, value: "guessed" });
  guest.send({ type: "result", round: 1, value: "missed" });
  await wait(200);
  const withResults = host.last("room");
  check("results collected", Object.keys(withResults.results).length === 2, JSON.stringify(withResults.results));

  host.send({ type: "reveal", round: 1 });
  await wait(200);
  check("reveal phase", host.last("room").phase === "reveal");

  host.send({ type: "next_round" });
  await wait(200);
  const r2 = host.last("room");
  check("next round resets results", r2.round === 2 && Object.keys(r2.results).length === 0);

  guest.send({ type: "leave" });
  await wait(200);
  check("leave removes player", host.last("room").players.length === 1);

  host.ws.close();
  guest.ws.close();
} catch (e) {
  console.error(e);
  results.push({ name: "exception", ok: false });
} finally {
  await wait(300);
  server.kill();
  const failed = results.filter((r) => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} checks passed`);
  process.exit(failed.length ? 1 : 0);
}
