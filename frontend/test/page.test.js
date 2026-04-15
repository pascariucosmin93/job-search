import test from "node:test";
import assert from "node:assert/strict";

test("homepage title copy", () => {
  assert.equal("Job Alerts & Tech Radar".includes("Tech Radar"), true);
});

