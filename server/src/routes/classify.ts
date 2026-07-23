import { Hono } from "hono";
import { classify } from "../classifier/categoryClassifier";

const app = new Hono();

/**
 * Layer 3 fallback classification. The client only ever calls this for a message its own Layer 1
 * regex classifier landed on Unknown for, and only after redacting PII from the body.
 */
app.post("/", async (c) => {
  const payload = await c.req.json().catch(() => null);
  const body = payload?.body;
  if (typeof body !== "string" || body.trim().length === 0) {
    return c.json({ error: "body (string) is required" }, 400);
  }
  return c.json(classify(body));
});

export default app;
