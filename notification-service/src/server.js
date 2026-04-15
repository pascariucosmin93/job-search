import express from "express";

const app = express();
const port = process.env.PORT || 8080;
const postgresHost = process.env.POSTGRES_HOST || "postgres.postgres.svc.cluster.local";

app.get("/health", (_req, res) => {
  res.json({ status: "UP" });
});

app.get("/ready", (_req, res) => {
  res.json({ status: "READY", postgresHost });
});

app.get("/api/notifications", (_req, res) => {
  res.json([
    { id: "notif-1", channel: "email", state: "queued" },
    { id: "notif-2", channel: "slack", state: "sent" }
  ]);
});

app.listen(port, () => {
  console.log(`notification-service listening on ${port}`);
});

export default app;

