# Meta-Harvester CLI

This tool provides a command-line interface to generate piveau pipes and catalogues based on harvesters from a CKAN instance.

## Setup

### Install Dependencies
This project uses Poetry for dependency management.

First, activate your venv
`eval $(poetry env activate)`

Check what what are you using
`poetry env info`

To install the required packages, run:
`poetry install`


## CLI Commands

All commands should be run using `python -m meta_harvester <command>`.

---

### `generate`

Fetches all harvesters from the configured CKAN instance, and generates the necessary pipe and catalogue files.

**Usage:**
```bash
python -m meta_harvester generate
```

---

### `generate-all-catalogues`

Fetches all harvesters from CKAN and generates only catalogue metadata files (e.g., `bfs-dcat-harvester.ttl`) in the `piveau_catalogues/` directory.
To upload catalog metadata, use the ``catalogues.sh` script in the `scripts/` directory.

**Usage:**
```bash
python -m meta_harvester generate-all-catalogues
```

---

### `generate-all-pipes`

Fetches all harvesters from CKAN and generates only the pipe definition files (e.g., `bfs-dcat-harvester.json`) in the `piveau_pipes/` directory. 
It also generates the file `bulk.json` in the `piveau_triggers/` directory, for scheduling pipe runs.

**Usage:**
```bash
python -m meta_harvester generate-all-pipes
```

---


