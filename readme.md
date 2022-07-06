# Kabara

Sync Timbuctoo datasets with a SPARQL-enabled triple store.

## Requirements

* JDK 11
* [Timbuctoo](https://github.com/HuygensING/timbuctoo.git)
* SPARQL store
    * [Virtuoso](https://github.com/openlink/virtuoso-opensource)
    * [Blazegraph](https://blazegraph.com)

## Getting started

### Run Kabara locally

1. Compile Kabara: `mvn clean package`
2. Make sure Timbuctoo and the SPARQL endpoint are running (see their respective manuals)
3. Start Kabara: `./target/appassembler/bin/kabara server kabara.yml`
4. Check if Kabara is running: `curl localhost:9000/kabara`

### Run Kabara with Docker

1. Depending on which SPARQL store to use, choose a Docker compose file:
    * Virtuoso: `docker-compose-virtuoso.yaml`
    * Blazegraph: `docker-compose-blazegraph.yaml`
2. Run Kabara and Virtuoso: `docker-compose up -f <docker-compose-file.yaml>`

## Configuration

The file `kabara.yml` contains the configuration. Either change the configuration file or use the environment variables
to update the configuration of Kabara:

* `DATA_PATH`: Where Kabara stores it state
* `TRIPLE_STORE_CLASS`: The Java class to use to configure the triple store
* `TRIPLE_STORE_URL`: The URL of the triple store
* `TRIPLE_STORE_SPARQL_PATH`: The path to the SPARQL endpoint
* `TRIPLE_STORE_BATCH_SIZE`: The batch size of the `INSERT/DELETE` commands to timbuctooSync the RDF data
* `RESOURCE_SYNC_TIMEOUT`: Timeout to read from the Timbuctoo ResourceSync endpoint
* `PUBLIC_URL`: Public URL of the Kabara application

## Synchronizing data

_We assume Kabara is running on http://localhost:9000_

Configure a dataset for synchronization from Timbuctoo to the configured SPARQL store:

```console
curl -v -XPOST http://localhost:9000/<endpoint>/<user_id>/<dataset_name>
```

The `endpoint` is the id to the Timbuctoo instance. These are specified in the configuration file `kabara.yml`.

You can also specify an alternative graph URI and whether to automatically timbuctooSync the data when the data changes in
Timbuctoo:

```console
curl -v -d "graphUri=<alternative_graph_uri>&autoSync=<true_or_false>" http://localhost:9000/<endpoint>/<user_id>/<dataset_name>
```

You can track progress and the configuration of a dataset:

```console
curl http://localhost:9000/<endpoint>/<user_id>/<dataset_name>
```

To request a timbuctooSync, call:

```console
curl -v -XPOST http://localhost:9000/<endpoint>/<user_id>/<dataset_name>/timbuctooSync
```
