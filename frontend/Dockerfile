FROM node:22-alpine AS build

WORKDIR /app
RUN corepack enable && corepack prepare pnpm@10.6.5 --activate

COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .

ARG VITE_USER_API_URL=
ARG VITE_PRODUCT_API_URL=
ARG VITE_CART_API_URL=
ENV VITE_USER_API_URL=$VITE_USER_API_URL
ENV VITE_PRODUCT_API_URL=$VITE_PRODUCT_API_URL
ENV VITE_CART_API_URL=$VITE_CART_API_URL
RUN pnpm build

FROM nginx:1.29-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx-spa.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1
