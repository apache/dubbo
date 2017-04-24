FROM java:8-jdk-alpine
MAINTAINER Gao <altiplanogao@hotmail.com>
RUN apk add --no-cache wget bash supervisor

ENV APP_PATH=/dubbo/demo-consumer
RUN mkdir -m 0755 -p /dubbo/demo-consumer
VOLUME ["/dubbo/demo-consumer/conf"]

ADD app /dubbo/demo-consumer
COPY supervisord.conf /etc/supervisord.conf

CMD ["/usr/bin/supervisord", "-n"]