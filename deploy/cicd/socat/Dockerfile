FROM alpine:latest
#USER root
#Installing socat
RUN apk update && apk upgrade && apk add bash socat
COPY entrypoint.sh entrypoint.sh
ENTRYPOINT ["sh", "./entrypoint.sh"]
