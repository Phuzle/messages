import express from "express";
import classifyRouter from "./routes/classify";
import remindersRouter from "./routes/reminders";
import { requestLogger } from "./middleware/logging";

const app = express();
app.use(express.json({ limit: "16kb" }));
app.use(requestLogger);

app.get("/healthz", (_req, res) => res.json({ status: "ok" }));
app.use("/v1/classify", classifyRouter);
app.use("/v1/reminders", remindersRouter);

app.use((_req, res) => res.status(404).json({ error: "not found" }));

const PORT = Number(process.env.PORT ?? 8080);
// Bind all interfaces, not just localhost — the Android emulator reaches this via 10.0.2.2 and a
// real device over wifi debugging reaches it via the host machine's LAN IP; binding to 127.0.0.1
// would refuse both.
const HOST = "0.0.0.0";

app.listen(PORT, HOST, () => {
  console.log(`phuzle-messages-server listening on http://${HOST}:${PORT}`);
});
