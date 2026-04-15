import test from "node:test";
import assert from "node:assert/strict";

test("radar contains adopt ring", () => {
  const rings = ["adopt", "trial", "assess", "hold"];
  assert.ok(rings.includes("adopt"));
});

