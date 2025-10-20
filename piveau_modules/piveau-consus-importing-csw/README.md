# Piveau Consus - Importing CSW

This module harvests XML-based metadata.

It traverses all CSW records (<http://www.opengis.net/cat/csw/2.0.2/Record>), and passes them to the next steps in the pipe.

## Installing

To compile the module:

```sh
mvn clean install
```

To compile it without running tests:

```sh
mvn -Dmaven.test.skip=true install
```

To build the container with newly compiled module:

```sh
cd ../../opendata.swiss/metadata
docker compose -f docker-compose-custom.yaml build --no-cache piveau-consus-importing-csw
```
