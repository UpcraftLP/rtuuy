# escape=\
# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine

LABEL \
	org.opencontainers.image.authors = "Up"
	org.opencontainers.image.source = "https://github.com/UpcraftLP/rtuuy"
	# FIXME add license file
	# org.opencontainers.image.licenses = "" \
	org.opencontainers.image.title = "Rtuuy"
	org.opencontainers.image.description = "Discord bot for Rattiest Gang"

COPY [ "build/libs/rtuuy-*-all.jar", "/bot/rtuuy.jar" ]

VOLUME [ "/bot/data" ]
VOLUME [ "/bot/plugins" ]

WORKDIR /bot

ENTRYPOINT [ "java", "-XshowSettings:vm", "-XX:MaxRAMPercentage=90", "-jar", "/bot/rtuuy.jar" ]
