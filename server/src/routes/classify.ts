import { Router } from "express";
import { classify } from "../classifier/categoryClassifier";

const router = Router();

/**
 * Layer 3 fallback classification. The client only ever calls this for a message its own Layer 1
 * regex classifier landed on Unknown for, and only after redacting PII from the body.
 */
router.post("/", (req, res) => {
  const body = req.body?.body;
  if (typeof body !== "string" || body.trim().length === 0) {
    res.status(400).json({ error: "body (string) is required" });
    return;
  }
  res.json(classify(body));
});

export default router;
