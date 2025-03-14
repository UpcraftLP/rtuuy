# escape=\
# syntax=docker/dockerfile:1

FROM gcr.io/distroless/java21-debian12

WORKDIR /bot

VOLUME [ "/bot/data" ]
VOLUME [ "/bot/plugins" ]

EXPOSE 3000/tcp

# copy base artifact
COPY [ "build/libs/rtuuy-*-all.jar", "/bot/rtuuy.jar" ]

CMD [ "/bot/rtuuy.jar" ]
