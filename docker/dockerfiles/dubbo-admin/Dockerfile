FROM tomcat:6.0.45-jre8
MAINTAINER Gao <altiplanogao@hotmail.com>

ENV APP_PATH=/dubbo/dubbo-admin
ENV DEPLOY_PATH=/usr/local/tomcat/webapps/ROOT
#VOLUME ["/root/dubbo.properties"]
EXPOSE 8080

RUN mkdir -m 0755 -p $APP_PATH
ADD app $APP_PATH
RUN mv $DEPLOY_PATH /usr/local/tomcat/webapps/ort
RUN ln -s $APP_PATH $DEPLOY_PATH
