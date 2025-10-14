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



### Generate catalogues and pipes

Fetches all geoharvesters from the configured CKAN instance, generates the necessary configuration files, and uploads the catalogue metadata.

**Usage:**

```bash
python -m meta_harvester generate
```

Or, if you need to define environment variables:
```bash
API_KEY_HUB=yourRepoApiKey HUB_REPO_ENDPOINT=http://localhost:8081 python -m meta_harvester
```

### Run pipes

Triggers one or more piveau pipes to execute immediately.

**Usage:**

**1. Run all pipes:**
If no specific pipe names are provided, the script will discover all `.yaml` files in the `piveau_pipes/` directory and trigger each one.

```bash
python -m meta_harvester run-pipes
```

**2. Run specific pipes:**
You can provide a space-separated list of pipe names to trigger only those specific pipes.

```bash
python -m meta_harvester run-pipes <pipe-name-1> <pipe-name-2> ...
```

**Example:**

```bash
python -m meta_harvester run-pipes aargau-kt-geocat-harvester bs-geocat-harvester
```


## TODO:
* test on scale - run all pipes
* test cli commands

*add cli command for creating catalogue AND providing a file (optional)

* add unit tests
* add publisher name and website ---> needs fetching it from organizations api (["result"]["url"]): https://ckan.opendata.swiss/api/3/action/organization_show?id=cnai

* clean code - remove overwritten pipes and catalogues
* clean code - logger instead of print
* clean code - type hints

DEMO:
- creating catalogues (in cli and in piveau-hub)





