import type { MiddlewareHandler } from "hono";

/** Minimal request logging — no request bodies logged, since those are (already scrubbed) message text. */
export const requestLogger: MiddlewareHandler = async (c, next) => {
  const start = Date.now();
  await next();
  console.log(`${c.req.method} ${c.req.path} -> ${c.res.status} (${Date.now() - start}ms)`);
};
