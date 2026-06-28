# Multi-stage build: Node only exists inside the "build" stage, and the final image is
# just static files served by nginx -- nothing needs to be installed on the host.

FROM node:20-alpine AS build
WORKDIR /app

COPY package.json ./
RUN npm install

COPY . .

# Vite bakes VITE_* vars into the built JS at build time, not at container start time, so
# this has to be a build arg rather than a plain runtime environment variable. It defaults
# to the backend's published host port, since the browser (not this container) is what
# actually calls the API.
ARG VITE_API_BASE=http://localhost:8080
ENV VITE_API_BASE=$VITE_API_BASE

RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
