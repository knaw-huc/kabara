# kabara
Sync a Timbuctoo dataset with a triple store

__You need:__
- Java 1.8
- SparQL (virtuoso) (https://github.com/openlink/virtuoso-opensource)
- ResourceSync (https://github.com/knaw-huc/resourcesync.git)
- Timbuctoo (https://github.com/HuygensING/timbuctoo.git) (to use kabara locally)
- Kabara (https://github.com/knaw-huc/kabara.git)

__Preparations:__
- compile ResourceSync to get a jar;
- add this jar to the kabara pom.xml
- compile Kabara (to a jar if you want to run kabara from a prompt)
- make sure Timbuctoo and SparQl are running (see their repective manuals)

__How to use Kabara:__

- Adjust config.xml (it can be found in: src/test/resources/config.xml, but
  can be placed anywhere).

- config.xml  contains tags for:
   - resourcesync: where to get the data; in this example the locally running
    Timbuctoo
   - trimplestore endpoint: where to send the data; in this example the locally
    running Virtuoso/SparQL
   - user and pass for the SparQL
   - dataset: how to name the dataset in SparQL
   - synced: date and time of last run

Kabara is run with config.xml as parameter (complete or relative path).
After a succesfull run synced in set to the date and time of this run and
config.xml is saved.

If synced is empty (for example at the first run) it is presumed a new SparQL
db is to be made. If synced is not empty only data changed (in Timbuctoo)
since the last run is sent to SparQl.
