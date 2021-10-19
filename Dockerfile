FROM openjdk:11-jdk

RUN apt-get update && apt-get install -y curl tar

ARG MAVEN_VERSION=3.3.9
ARG USER_HOME_DIR="/root"

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz \
    | tar -xzC /usr/share/maven --strip-components=1 \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

RUN echo -e '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">\n  <localRepository>/usr/share/maven/ref/repository</localRepository>\n</settings>' > /usr/share/maven/ref/settings-docker.xml

WORKDIR /build/kabara

COPY ./ ./
COPY ./pom.xml ./pom.xml

RUN mvn clean package

RUN chmod +x ./target/appassembler/bin/kabara

COPY ./kabara.yml ./kabara.yml

# example! Make sure these are the same as in kabara.yml
# for GET
EXPOSE 9000
# for POST
EXPOSE 9001

CMD ["./target/appassembler/bin/kabara", "server", "kabara.yml"]

