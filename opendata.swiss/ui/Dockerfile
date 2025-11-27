FROM node:22-alpine AS builder

WORKDIR /app

# Install dependencies
COPY package.json package-lock.json ./
RUN npm ci --omit dev

# Copy the rest of the application code and build
COPY . .
RUN ls -la content/pages
RUN npm run build

# Serve the build
FROM node:22-alpine
WORKDIR /app
COPY --from=builder /app/.output /app

ENV PORT=80
ENV HOST=0.0.0.0

EXPOSE 80

CMD ["node", "/app/server/index.mjs"]
