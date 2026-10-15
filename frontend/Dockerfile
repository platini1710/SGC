# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM node:20-alpine AS build

WORKDIR /app

# Cache de dependencias npm
COPY package*.json ./
RUN npm ci --silent

# Build de producción (usa environment.prod.ts vía fileReplacements)
COPY . .
RUN npx ng build --configuration production

# ── Stage 2: Servir con Nginx ────────────────────────────────────────────────
FROM nginx:1.25-alpine

# Quitar config por defecto y usar la nuestra
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar el bundle compilado
COPY --from=build /app/dist/school-schedule-frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
