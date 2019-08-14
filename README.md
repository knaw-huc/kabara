# kabara
Sync a Timbuctoo dataset with a triple store

__You need:__
- Java 1.8
- SparQL (virtuoso) (https://github.com/openlink/virtuoso-opensource)
- ResourceSync (https://github.com/knaw-huc/resourcesync.git)
- Timbuctoo (https://github.com/HuygensING/timbuctoo.git) (to use kabara locally)
- Kabara (https://github.com/knaw-huc/kabara.git)

__Preparations:__
- mvn clean install ResourceSync (add ResourceSync to local maven);
- compile Kabara (to a jar if you want to run kabara from a prompt)
- make sure Timbuctoo and SparQl are running (see their repective manuals)

__How to use Kabara:__

- Start Kabara: ./[path]/kabara.jar server kabara.yml

  It will wait for a curl POST to start work.

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
`curl -X POST -d config=/Users/meindertkroese/git/kabara/src/test/resources/config.xml http://localhost:9001/tasks/runKabara`
 with config.xml as parameter (absolute path). The 9001 in this example is the adminConnector as set in kabara.yml.
After a succesfull run synced is set to the date and time of this run and
config.xml is saved.

If synced is empty (for example at the first run) it is presumed a new SparQL
db is to be made. If synced is not empty only data changed (in Timbuctoo)
since the last run is sent to SparQl.
