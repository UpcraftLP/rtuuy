# escape=\
# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine

WORKDIR /bot

COPY [ "build/libs/rtuuy-*-all.jar", "/bot/rtuuy.jar" ]

VOLUME [ "/bot/data" ]
VOLUME [ "/bot/plugins" ]

ENTRYPOINT [ "java", "-XshowSettings:vm", "-XX:MaxRAMPercentage=90", "-jar", "/bot/rtuuy.jar" ]
