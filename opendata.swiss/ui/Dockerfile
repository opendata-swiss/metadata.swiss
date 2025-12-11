FROM node:24-alpine AS builder

WORKDIR /app

RUN apk add --no-cache python3 make

# Install dependencies
COPY package.json package-lock.json ./
RUN npm ci --omit dev

# Copy the rest of the application code and build
COPY . .
RUN npm run build

# Serve the build
FROM node:24-alpine
WORKDIR /app
COPY --from=builder /app/.output /app

ENV PORT=80
ENV HOST=0.0.0.0

EXPOSE 80

CMD ["node", "/app/server/index.mjs"]
