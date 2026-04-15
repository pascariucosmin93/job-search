import express from "express";

const app = express();
const port = process.env.PORT || 8080;

app.get("/health", (_req, res) => {
  res.json({ status: "UP" });
});

app.get("/ready", (_req, res) => {
  res.json({ status: "READY" });
});

app.get("/api/radar", (_req, res) => {
  res.json({
    adopt: ["Kubernetes", "GitOps"],
    trial: ["eBPF", "Backstage"],
    assess: ["WebAssembly"],
    hold: ["Manual Deployments"]
  });
});

app.listen(port, () => {
  console.log(`analytics-service listening on ${port}`);
});

export default app;

