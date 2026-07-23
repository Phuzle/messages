import { Hono } from "hono";
import { db } from "../db/database";

/** CRUD over the reminder extractor's trigger-phrase dataset — same idea as ../routes/keywords.ts
 * but for the phrases that gate whether a message is even considered for due-date extraction. */
const app = new Hono();

app.get("/", (c) => {
  const rows = db.query("SELECT id, phrase FROM trigger_phrases ORDER BY phrase").all();
  return c.json(rows);
});

app.post("/", async (c) => {
  const payload = await c.req.json().catch(() => null);
  const phrase = typeof payload?.phrase === "string" ? payload.phrase.trim().toLowerCase() : "";
  if (phrase.length === 0) return c.json({ error: "phrase (non-empty string) is required" }, 400);

  db.query("INSERT OR IGNORE INTO trigger_phrases (phrase) VALUES (?)").run(phrase);
  const row = db.query("SELECT id, phrase FROM trigger_phrases WHERE phrase = ?").get(phrase);
  return c.json(row, 201);
});

app.delete("/:id", (c) => {
  const id = Number(c.req.param("id"));
  if (!Number.isFinite(id)) return c.json({ error: "invalid id" }, 400);
  db.query("DELETE FROM trigger_phrases WHERE id = ?").run(id);
  return c.body(null, 204);
});

export default app;
