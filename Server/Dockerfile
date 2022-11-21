FROM node:12-slim

RUN npm install -g forever

RUN mkdir /app
WORKDIR /app
COPY src/ .
ADD node_modules ./node_modules

RUN ls -la

# now, start it up, and keep it running
CMD ["forever", "server.js"]
