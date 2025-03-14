# escape=\
# syntax=docker/dockerfile:1

FROM gcr.io/distroless/java21-debian12

WORKDIR /bot

COPY [ "build/libs/rtuuy-*-all.jar", "/bot/rtuuy.jar" ]

VOLUME [ "/bot/data" ]
VOLUME [ "/bot/plugins" ]

CMD [ "/bot/rtuuy.jar" ]
