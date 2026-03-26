FROM node:24-alpine

WORKDIR /app

RUN apk add --no-cache python3 make git

# Install dependencies
COPY package.json package-lock.json ./
RUN npm i

# Copy the rest of the application code
COPY . .

ENV PORT=80
ENV HOST=0.0.0.0

EXPOSE 80

CMD ["sh", "-c", "./entrypoint.sh"]
