FROM mongo:4.0.21
LABEL Descriprion="Database for users"

RUN mkdir /var/lib/mongodb

ENTRYPOINT mongod --dbpath --auth /var/lib/mongodb --bind_ip_all