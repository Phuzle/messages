import { NextFunction, Request, Response } from "express";

/** Minimal request logging — no request bodies logged, since those are (already scrubbed) message text. */
export function requestLogger(req: Request, res: Response, next: NextFunction): void {
  const start = Date.now();
  res.on("finish", () => {
    console.log(`${req.method} ${req.originalUrl} -> ${res.statusCode} (${Date.now() - start}ms)`);
  });
  next();
}
