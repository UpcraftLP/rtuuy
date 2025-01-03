# escape=\
# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine

RUN mkdir -p /bot/plugins
RUN mkdir -p /bot/data

COPY [ "build/libs/rtuuy-*-all.jar", "/bot/bot.jar" ]

VOLUME [ "/bot/data" ]
VOLUME [ "/bot/plugins" ]

WORKDIR /bot

ENTRYPOINT [ "java", "-Xms2G", "-Xmx2G", "-jar", "/bot/bot.jar" ]
