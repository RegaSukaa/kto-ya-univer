export const PROTO = 1;
export const MAX_PLAYERS = 10;
export const ROOM_TTL_MS = 90 * 60 * 1000;
export const HOST_GRACE_MS = 60 * 1000;
export const OFFLINE_MS = 60 * 1000;
export const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
export const CODE_RE = /^[A-HJ-NP-Z2-9]{6}$/;
export const MAX_PAYLOAD = 8192;

export function validName(name) {
  return typeof name === "string" && name.trim().length > 0 && name.trim().length <= 16;
}

export function parseMessage(raw) {
  if (typeof raw !== "string" || raw.length > 4096) return null;
  try {
    const obj = JSON.parse(raw);
    if (!obj || typeof obj.type !== "string") return null;
    return obj;
  } catch {
    return null;
  }
}

export function errorMessage(code, message) {
  return { type: "error", code, message };
}
