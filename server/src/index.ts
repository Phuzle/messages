import { Hono } from "hono";
import { requestLogger } from "./middleware/logging";
import classifyRoute from "./routes/classify";
import keywordsRoute from "./routes/keywords";
import remindersRoute from "./routes/reminders";

const app = new Hono();
app.use("*", requestLogger);

app.get("/healthz", (c) => c.json({ status: "ok" }));
app.route("/v1/classify", classifyRoute);
app.route("/v1/reminders", remindersRoute);
app.route("/v1/keywords", keywordsRoute);

app.notFound((c) => c.json({ error: "not found" }, 404));

const port = Number(process.env.PORT ?? 8080);

// Bind all interfaces, not just localhost — the Android emulator reaches this via 10.0.2.2 and a
// real device over wifi debugging reaches it via the host machine's LAN IP; binding to 127.0.0.1
// would refuse both. Bun.serve() picks this up from the default-export shape below.
export default {
  port,
  hostname: "0.0.0.0",
  fetch: app.fetch,
};

console.log(`phuzle-messages-server listening on http://0.0.0.0:${port}`);
