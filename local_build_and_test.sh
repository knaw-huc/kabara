# !/bin/bash

mvn clean package

set VIRTUOSO_HOST="http://localhost:8890"

docker run -p 8890:8890 -p 1111:1111 \
    -e DBA_PASSWORD=mysecret \
    -e SPARQL_UPDATE=true \
    -e DEFAULT_GRAPH=http://www.example.com/my-graph \
    -d tenforce/virtuoso:1.3.1-virtuoso7.2.2

JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

export JAVA_OPTS

./target/appassembler/bin/kabara server kabara.yml
