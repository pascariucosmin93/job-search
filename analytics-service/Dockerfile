FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json .
RUN npm install --omit=dev && npm cache clean --force

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=deps /app/node_modules node_modules
COPY src src
COPY package.json .
USER node
EXPOSE 8080
CMD ["node", "src/server.js"]
