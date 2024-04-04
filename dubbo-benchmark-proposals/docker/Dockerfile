FROM maven:3.6.3-adoptopenjdk-8
RUN sed -i 's/archive.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list
RUN useradd -ms /bin/bash dev
RUN usermod -aG sudo dev
RUN apt-get update && apt-get install -y \
    vim \
    ncat \
    jq \
    mycli

ENV HOME /home/dev
ENV MAVEN_CONFIG /home/dev/.m2
ENV USER_HOME_DIR /home/dev
USER dev
WORKDIR /home/dev
COPY ./settings.xml /usr/share/maven/conf/settings.xml
ENV PATH $JAVA_HOME/bin:$PATH