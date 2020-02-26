# !/bin/bash

mvn clean package

set VIRTUOSO_HOST="http://localhost:8890"

docker run --name my-virtuoso \
    -p 8890:8890 -p 1111:1111 \
    -e DBA_PASSWORD=mysecret \
    -e SPARQL_UPDATE=true \
    -e DEFAULT_GRAPH=http://www.example.com/my-graph \
    -v ./data/virtuoso:/data \
    -d virtuoso:1.3.1-virtuoso7.2.2

./target/appassembler/bin/kabara server kabara.yml
