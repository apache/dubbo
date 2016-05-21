FROM java:8-jdk-alpine
MAINTAINER Gao <altiplanogao@hotmail.com>
RUN apk add --no-cache wget bash supervisor

ENV APP_PATH=/dubbo/demo-provider
RUN mkdir -m 0755 -p /dubbo/demo-provider
VOLUME ["/dubbo/demo-provider/conf"]

ADD app /dubbo/demo-provider
COPY supervisord.conf /etc/supervisord.conf

CMD ["/usr/bin/supervisord", "-n"]