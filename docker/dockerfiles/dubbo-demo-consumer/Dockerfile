FROM java:8-jdk-alpine
MAINTAINER Gao <altiplanogao@hotmail.com>
ENV APP_PATH=/dubbo/demo-consumer
#RUN apt-get update
RUN apk add --no-cache wget bash

RUN mkdir -m 0755 -p /dubbo/demo-consumer
VOLUME ["/dubbo/demo-consumer/conf"]

ADD app /dubbo/demo-consumer

ENTRYPOINT ["/dubbo/demo-consumer/bin/start.sh"]