FROM oven/bun:1-alpine
WORKDIR /app

COPY package.json ./
RUN bun install --production

COPY tsconfig.json ./
COPY src ./src

# The sqlite dataset (keywords/triggers) lives here; docker-compose mounts a host volume over
# this path so keyword edits made through the API survive `docker compose up --build`.
RUN mkdir -p data

EXPOSE 8080
# Bun runs TypeScript directly — no separate build/tsc step needed.
CMD ["bun", "run", "src/index.ts"]
