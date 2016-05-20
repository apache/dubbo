FROM java:8-jdk-alpine
MAINTAINER Gao <altiplanogao@hotmail.com>
ENV APP_PATH=/dubbo/demo-provider
#RUN apt-get update
RUN apk add --no-cache wget bash

RUN mkdir -m 0755 -p /dubbo/demo-provider
VOLUME ["/dubbo/demo-provider/conf"]

ADD app /dubbo/demo-provider

ENTRYPOINT ["/dubbo/demo-provider/bin/start.sh"]