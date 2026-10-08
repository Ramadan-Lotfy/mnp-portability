# ---------- Stage 1: build ----------
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies first: this layer is reused until package files change.
# The cache mount keeps downloaded packages between builds, and the retry/timeout flags
# make installs survive a slow or flaky connection.
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --prefer-offline --no-audit --no-fund \
        --fetch-retries=5 \
        --fetch-retry-mintimeout=20000 \
        --fetch-retry-maxtimeout=120000 \
        --fetch-timeout=300000

COPY . .
RUN npm run build

# ---------- Stage 2: serve ----------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Angular's application builder writes to dist/<project-name>/browser.
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
