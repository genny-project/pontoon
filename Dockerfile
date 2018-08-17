FROM  openjdk:8u151-jre-alpine3.7
RUN echo http://mirror.yandex.ru/mirrors/alpine/v3.7/main > /etc/apk/repositories; \
    echo http://mirror.yandex.ru/mirrors/alpine/v3.7/community >> /etc/apk/repositories

RUN apk update && apk add jq && apk add curl && apk add bash

ADD target/pontoon-0.0.1-SNAPSHOT-fat.jar /service.jar
#ADD cluster.xml /cluster.xml

RUN mkdir /realm
ADD realm /opt/realm
ADD docker-entrypoint.sh /docker-entrypoint.sh

WORKDIR /

EXPOSE 5701
EXPOSE 5702
EXPOSE 5703
EXPOSE 5704
EXPOSE 5705
EXPOSE 5706
EXPOSE 8088
EXPOSE 15701

HEALTHCHECK --interval=10s --timeout=3s --retries=5 CMD curl -f / http://localhost:8088/version || exit 1

#CMD ["java"]
ENTRYPOINT [ "/docker-entrypoint.sh" ]

