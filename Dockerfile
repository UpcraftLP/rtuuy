# escape=\
# syntax=docker/dockerfile:1

ARG JAVA_VERSION=21
FROM eclipse-temurin:${JAVA_VERSION}-jdk-alpine

WORKDIR /bot

VOLUME [ "/bot/data" ]
VOLUME [ "/bot/plugins" ]

EXPOSE 3000/tcp

# copy base artifact
COPY [ "build/install/rtuuy", "." ]

RUN ["chmod", "+x", "/bot/bin/rtuuy"]

ENTRYPOINT [ "/bot/bin/rtuuy" ]
