FROM alpine:3.12.1

RUN apk update && apk add npm

RUN npm install -g migrate-mongo@8.1.4

COPY users-migration/ /users-migration

WORKDIR /

ENTRYPOINT migrate-mongo up -f /users-migration/users-migratation-config.js