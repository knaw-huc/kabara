FROM maven:3-openjdk-17-slim AS builder

RUN mkdir -p /build/kabara
WORKDIR /build/kabara

COPY . /build/kabara
RUN mvn clean package

FROM openjdk:17-jre-slim

COPY --from=builder /build/kabara/target/appassembler /app
COPY --from=builder /build/kabara/kabara.yml /app/kabara.yml

WORKDIR /app

EXPOSE 9000
EXPOSE 9001

CMD ["./bin/kabara", "server", "kabara.yml"]
