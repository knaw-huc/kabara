FROM openjdk:11-jdk as builder

RUN apt-get update && apt-get install -y curl tar

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL https://apache.osuosl.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz \
    | tar -xzC /usr/share/maven --strip-components=1 \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

RUN mkdir -p /build/kabara
WORKDIR /build/kabara

COPY . /build/kabara
RUN mvn clean package

FROM openjdk:11-jre-slim

COPY --from=builder  /build/kabara/target/appassembler /app
COPY --from=builder /build/kabara/kabara.yml /app/kabara.yml

WORKDIR /app

EXPOSE 9000
EXPOSE 9001

CMD ["./bin/kabara", "server", "kabara.yml"]
