import { Router } from "express";
import { extractReminder } from "../classifier/reminderExtractor";

const router = Router();

router.post("/extract", (req, res) => {
  const { body, receivedAtEpochMillis, timezoneOffsetMinutes } = req.body ?? {};
  if (typeof body !== "string" || body.trim().length === 0) {
    res.status(400).json({ error: "body (string) is required" });
    return;
  }
  const receivedAt = typeof receivedAtEpochMillis === "number" ? receivedAtEpochMillis : Date.now();
  const offset = typeof timezoneOffsetMinutes === "number" ? timezoneOffsetMinutes : 0;
  res.json(extractReminder(body, receivedAt, offset));
});

export default router;
