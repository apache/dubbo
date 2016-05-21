FROM java:8-jdk-alpine
MAINTAINER Gao <altiplanogao@hotmail.com>
RUN apk add --no-cache wget bash supervisor

ENV APP_PATH=/dubbo/monitor-simple
RUN mkdir -m 0755 -p /dubbo/monitor-simple
VOLUME ["/dubbo/monitor-simple/conf"]
EXPOSE 8080

ADD app /dubbo/monitor-simple
COPY supervisord.conf /etc/supervisord.conf

CMD ["/usr/bin/supervisord", "-n"]