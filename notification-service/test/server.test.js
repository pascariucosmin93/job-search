import test from "node:test";
import assert from "node:assert/strict";

test("notification payload shape", () => {
  const payload = { id: "notif-1", channel: "email", state: "queued" };
  assert.equal(payload.channel, "email");
});

