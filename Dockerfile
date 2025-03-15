# escape=\
# syntax=docker/dockerfile:1

ARG JAVA_VERSION=21
FROM eclipse-temurin:${JAVA_VERSION}-jdk-alpine

WORKDIR /bot

VOLUME [ "/bot/data" ]
VOLUME [ "/bot/plugins" ]

EXPOSE 3000/tcp

COPY [ "docker/healthcheck.sh", "/bot/healthcheck.sh" ]
RUN ["chmod", "+x", "/bot/healthcheck.sh"]

HEALTHCHECK --start-period=30s --start-interval=3s --interval=30s --timeout=5s --retries=3 \
	CMD [ "/bot/healthcheck.sh" ]

COPY [ "build/install/rtuuy", "." ]
RUN ["chmod", "+x", "/bot/bin/rtuuy"]

ENTRYPOINT [ "/bot/bin/rtuuy" ]
