import test from "node:test";
import assert from "node:assert/strict";
import { RoomStore, RoomError } from "../rooms.mjs";
import { CODE_RE, MAX_PLAYERS } from "../protocol.mjs";

function storeAt(t) {
  let now = t;
  const store = new RoomStore(() => now);
  return { store, tick: (ms) => { now += ms; }, get now() { return now; } };
}

test("create gives a 6-char unambiguous code", () => {
  const { store } = storeAt(1000);
  const { room } = store.create({ name: "Рустам", deckSize: 12, deckHash: "sha256:x" });
  assert.equal(room.code.length, 6);
  assert.match(room.code, CODE_RE);
  assert.equal(store.rooms.size, 1);
});

test("join adds a player and rejects duplicate names case-insensitively", () => {
  const { store } = storeAt(1000);
  const { room } = store.create({ name: "Аня" });
  store.join(room.code, "Дима");
  assert.equal(room.players.length, 2);
  assert.throws(() => store.join(room.code, "аня"), (e) => e instanceof RoomError && e.code === "NAME_TAKEN");
});

test("room full at 10 players", () => {
  const { store } = storeAt(1000);
  const { room } = store.create({ name: "p0" });
  for (let i = 1; i < MAX_PLAYERS; i++) store.join(room.code, `p${i}`);
  assert.throws(() => store.join(room.code, "лишний"), (e) => e.code === "ROOM_FULL");
});

test("only host can start, second start is rejected", () => {
  const { store } = storeAt(1000);
  const { room, player: host } = store.create({ name: "host" });
  const { player: guest } = store.join(room.code, "guest");
  assert.throws(() => store.start(room.code, guest.id, {}), (e) => e.code === "NOT_HOST");
  store.start(room.code, host.id, { showMs: 10000 });
  assert.equal(room.phase, "countdown");
  assert.throws(() => store.start(room.code, host.id, {}), (e) => e.code === "ALREADY_STARTED");
});

test("rejoin restores the same player, bad token fails", () => {
  const { store } = storeAt(1000);
  const { room, player } = store.create({ name: "host" });
  const again = store.rejoin(room.code, player.id, player.token);
  assert.equal(again.player.id, player.id);
  assert.throws(() => store.rejoin(room.code, player.id, "nope"), (e) => e.code === "ROOM_NOT_FOUND");
});

test("room TTL is 90 minutes", () => {
  const { store, tick } = storeAt(0);
  const { room } = store.create({ name: "host" });
  tick(89 * 60 * 1000);
  assert.ok(store.get(room.code));
  tick(2 * 60 * 1000);
  assert.throws(() => store.get(room.code), (e) => e.code === "ROOM_NOT_FOUND");
  assert.equal(store.rooms.size, 0);
});

test("offline after 60s without ping, seat kept", () => {
  const { store, tick } = storeAt(0);
  const { room, player } = store.create({ name: "host" });
  store.join(room.code, "guest");
  tick(61_000);
  store.sweep();
  assert.equal(room.players[0].online, false);
  assert.equal(room.players.length, 2);
  store.ping(room.code, player.id);
  assert.equal(room.players[0].online, true);
});

test("results collect until allDone", () => {
  const { store } = storeAt(0);
  const { room, player: host } = store.create({ name: "host" });
  const { player: guest } = store.join(room.code, "guest");
  store.start(room.code, host.id, {});
  store.setResult(room.code, host.id, 1, "guessed");
  assert.equal(store.allDone(room), false);
  store.setResult(room.code, guest.id, 1, "missed");
  assert.equal(store.allDone(room), true);
});

test("next round clears results and keeps players", () => {
  const { store } = storeAt(0);
  const { room, player: host } = store.create({ name: "host" });
  store.join(room.code, "guest");
  store.start(room.code, host.id, {});
  store.setResult(room.code, host.id, 1, "guessed");
  const before = room.seed;
  store.nextRound(room.code, host.id);
  assert.equal(room.round, 2);
  assert.deepEqual(room.results, {});
  assert.notEqual(room.seed, before);
});

test("host leaves lobby -> host transfers; playing -> room closes after grace", () => {
  const { store, tick } = storeAt(0);
  const { room, player: host } = store.create({ name: "host" });
  const { player: guest } = store.join(room.code, "guest");
  store.leave(room.code, host.id);
  assert.equal(room.hostId, guest.id);

  store.join(room.code, "third");
  store.start(room.code, guest.id, {});
  store.leave(room.code, guest.id);
  tick(61_000);
  store.sweep();
  assert.equal(store.rooms.size, 0);
});

test("deck filter is stored and broadcast", () => {
  const { store } = storeAt(0);
  const { room, player: host } = store.create({ name: "host" });
  store.join(room.code, "guest");
  store.start(room.code, host.id, { filter: "teachers" });
  assert.equal(store.snapshot(room).filter, "teachers");
});
