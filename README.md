# kabara
Sync a Timbuctoo dataset with a triple store

__You need:__
- Java 1.8
- SparQL (virtuoso) (https://github.com/openlink/virtuoso-opensource)
- ResourceSync (https://github.com/knaw-huc/resourcesync.git)
- Timbuctoo (https://github.com/HuygensING/timbuctoo.git) (to use kabara locally)
- Kabara (https://github.com/knaw-huc/kabara.git)

__Preparations:__
- mvn clean install ResourceSync (add ResourceSync to local mmeaven);
- compile Kabara (to a jar if you want to run kabara from a prompt)
- make sure Timbuctoo and SparQl are running (see their repective manuals)

__How to use Kabara:__

- Start Kabara: ./[path]/kabara.jar server kabara.yml

  It will wait for a curl POST to start work.
  
  Kabara can run as a docker container, see below.

- File kabara.yml contains: ports for application and admin connectors (adjust them if your local timbuctoo and/or
sparql use the same ports).

- Adjust config.xml (it can be found in: src/test/resources/config.xml, but
  can be placed anywhere).

- config.xml  contains tags for:
   - resourcesync: where to get the data; in this example the locally running
    Timbuctoo (exampl. http://[timbuctoo_domein]/v5/resourcesync/[user_id]/[dataset_id]/capabilitylist.xml)
   - timeout: in milliseconds (in the example on 15000; can be adjusted to match the need)
   - trimplestore endpoint: where to send the data; in this example the locally
    running Virtuoso/SparQL
   - user and pass for the SparQL
   - dataset: how to name the dataset in SparQL
   - synced: date and time of last run

Kabara is run by sending a curl command, for example:
```
curl -X POST -d '{ "dataSet": "https://repository.huygens.knaw.nl/v5/resourcesync/u74ccc032adf8422d7ea92df96cd4783f0543db3b/dwc/capabilitylist.xml1" }' -H 'content-type: application/json' http://localhost:9000/kabara
```
After a successful run synced is set to the date and time of this run and
config.xml is saved.

If synced is empty (for example at the first run) it is presumed a new SparQL
db is to be made. If synced is not empty only data changed (in Timbuctoo)
since the last run is sent to SparQl.

With a `GET` you can check if Kabara is properly started: `localhost:9000/kabara`.

__Kabara in docker__

Dockerize Kabara, for example: `docker build --tag=kabara:local .`

Run Kabara, for example: `docker run -p9000:9000 -p9001:9001 kabara:local`

The `-p`'s in the above line are necessary to make the exposed ports in your 
`Dockerfile` communicate with the outside world.

Also possible:

`docker-compose up`

Don't forget to change the port numbers in `docker-compose.yaml` if necessary!
