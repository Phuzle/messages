import { Hono } from "hono";
import { extractReminder } from "../classifier/reminderExtractor";
import triggersRoute from "./triggers";

const app = new Hono();

app.post("/extract", async (c) => {
  const payload = await c.req.json().catch(() => null);
  const body = payload?.body;
  if (typeof body !== "string" || body.trim().length === 0) {
    return c.json({ error: "body (string) is required" }, 400);
  }
  const receivedAt = typeof payload?.receivedAtEpochMillis === "number" ? payload.receivedAtEpochMillis : Date.now();
  const offset = typeof payload?.timezoneOffsetMinutes === "number" ? payload.timezoneOffsetMinutes : 0;
  return c.json(extractReminder(body, receivedAt, offset));
});

// /v1/reminders/triggers — CRUD over the trigger-phrase dataset (see ./triggers.ts)
app.route("/triggers", triggersRoute);

export default app;
