# Meta-Harvester CLI

This tool provides a command-line interface to manage the lifecycle of piveau pipes and catalogues based on harvesters from a CKAN instance.

## Setup

### 1. Install Dependencies
This project uses Poetry for dependency management.

First, activate your venv
`eval $(poetry env activate)`

Check what what are you using
`poetry env info`

To install the required packages, run:
`poetry install`


### 2. Configure Environment Variables

The application requires several environment variables to connect to the CKAN and piveau instances. Create a `.env` file in the root of the `meta_harvester` directory:

```bash
touch .env
```

Add the following variables to the `.env` file, replacing the placeholder values with your actual endpoints and API keys:

```env
# .env

# API Key for the piveau-hub-repo
API_KEY_HUB="your-piveau-hub-api-key"

# Endpoint for the piveau-hub-repo (for creating/deleting catalogues)
HUB_REPO_ENDPOINT="http://localhost:8081"

# Endpoint for the piveau-pipes instance (for triggering pipes)
PIVEAU_PIPES_ENDPOINT="http://localhost:8090"
```

The script will automatically load these variables when executed.


## CLI Commands

### `generate`

Fetches all geoharvesters from the configured CKAN instance, generates the necessary pipe and catalogue files, and uploads the catalogue metadata to the hub.

**Usage:**

```bash
python -m meta_harvester generate
```

### `generate-all-pipes`

Fetches all geoharvesters from CKAN and generates just the pipe definition files (e.g., `aargau-kt-geocat-harvester.yaml`) in the `piveau_pipes/` directory. This does not create or upload catalogue metadata.

**Usage:**

```bash
python -m meta_harvester generate-all-pipes
```

### `run-pipes`

Triggers one or more piveau pipes to execute immediately.

**Usage:**
```bash
python -m meta_harvester run-pipes aargau-kt-geocat-harvester bs-geocat-harvester
```

### Catalogue Management

#### `create-catalogues`
Creates or recreates one or more catalogues in the piveau-hub. The command finds the corresponding `.ttl` file in the `piveau_catalogues/` directory based on the provided name.

**Usage:**
```bash
python -m meta_harvester create-catalogues <name1> <name2> ...
```

#### `create-all-catalogues`
Scans the `piveau_catalogues/` directory and creates or recreates every catalogue for which a `.ttl` file is found.

**Usage:**
```bash
python -m meta_harvester create-all-catalogues
```

#### `delete-catalogues`
Deletes one or more catalogues from the piveau-hub.

**Usage:**
```bash
python -m meta_harvester delete-catalogues <name1> <name2> ...
```

## TODO:


Handle this error in java code: ---------> else it gets called multiple times - no point in it
1. If NoApplicableCode exception/if no children ...  -> report warning and continue
2. Ask Kim what it means

piveau-consus-importing-csw-1    | 13:00:23.200 [vert.x-worker-thread-2] INFO  i.piveau.importing.csw.MainVerticle - https://www.geocat.ch/geonetwork/jura/ger/csw?service=CSW&version=2.0.2&request=GetRecords&elementsetname=full&resultType=results&typeNames=dcat&startPosition=61
piveau-consus-importing-csw-1    | 13:00:23.385 [vert.x-worker-thread-2] INFO  i.piveau.importing.csw.MainVerticle - Successfully fetched XML:
piveau-consus-importing-csw-1    | 13:00:23.385 [vert.x-worker-thread-2] INFO  i.piveau.importing.csw.MainVerticle - -------------------------
piveau-consus-importing-csw-1    | 13:00:23.386 [vert.x-worker-thread-2] INFO  i.piveau.importing.csw.MainVerticle - Found 98 records to forward.
piveau-consus-importing-csw-1    | 13:00:23.386 [vert.x-worker-thread-2] ERROR i.piveau.importing.csw.MainVerticle - An error occurred during the HTTP request:
piveau-consus-importing-csw-1    | java.lang.NullPointerException: Cannot invoke "org.jdom2.Element.getChildren(String, org.jdom2.Namespace)" because "records" is null
piveau-consus-importing-csw-1    | 	at io.piveau.importing.csw.MainVerticle.handlePipe(MainVerticle.java:123)



* test on scale - run all pipes
* test cli commands
* add functionality: delete catalogues - delete all catalogues in piveau
* add functionality: load catalogue, and execute pipe -> for 1x and for all


*add cli command for creating catalogue AND providing a file (optional)

* add unit tests
* add publisher name and website ---> needs fetching it from organizations api (["result"]["url"]): https://ckan.opendata.swiss/api/3/action/organization_show?id=cnai

* clean code - remove overwritten pipes and catalogues
* clean code - logger instead of print
* clean code - type hints




DEMO:
- creating catalogues (in cli and in piveau-hub)





