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

# The maximum number of pipes to run concurrently
MAX_CONCURRENT_RUNS=5
```

The script will automatically load these variables when executed.


## CLI Commands

All commands should be run using `python -m meta_harvester <command>`.

---

### `generate`

Fetches all geoharvesters from the configured CKAN instance, generates the necessary pipe and catalogue files, and uploads the catalogue metadata to the hub. This is a high-level command for a full setup.

**Usage:**
```bash
python -m meta_harvester generate
```

---

### `generate-all-pipes`

Fetches all geoharvesters from CKAN and generates just the pipe definition files (e.g., `aargau-kt-geocat-harvester.yaml`) in the `piveau_pipes/` directory. This does not create or upload catalogue metadata.

**Usage:**
```bash
python -m meta_harvester generate-all-pipes
```

---

### `run-pipes`

Triggers one or more piveau pipes to execute. By default, this command first creates/updates the corresponding catalogue for each pipe.

**Modes of Operation:**

*   **Run specific pipes:** Provide one or more pipe names.
    ```bash
    python -m meta_harvester run-pipes aargau-kt-geocat-harvester bs-geocat-harvester
    ```
*   **Run all pipes:** If no names are provided, the command discovers and runs all pipes in the `piveau_pipes/` directory.
    ```bash
    python -m meta_harvester run-pipes
    ```
*   **Skip catalogue creation:** Use the `--skip-catalogue` flag to run the pipe(s) without touching the catalogue first.
    ```bash
    python -m meta_harvester run-pipes aargau-kt-geocat-harvester --skip-catalogue
    ```

---

### Catalogue Management

#### `create-catalogues`

Creates or recreates catalogues from `.ttl` files located in the default `piveau_catalogues/` directory.

*   **Create all catalogues:** If run with no arguments, it scans the directory and creates a catalogue for every `.ttl` file found.
    ```bash
    python -m meta_harvester create-catalogues
    ```
*   **Create specific catalogues:** Provide one or more names to create only those catalogues from their corresponding files in the default directory.
    ```bash
    python -m meta_harvester create-catalogues stadt-bern glarus-kt
    ```

#### `create-single-catalogue`

Creates or updates a single catalogue from a `.ttl` file. This is useful for targeting a single catalogue, with the option to specify a custom file path.

*   **From default location:**
    ```bash
    python -m meta_harvester create-single-catalogue my-catalogue-name
    ```
*   **From a specific file:** Use the `--file` flag to provide a custom path.
    ```bash
    python -m meta_harvester create-single-catalogue my-catalogue-name --file /path/to/custom/metadata.ttl
    ```


#### `delete-catalogues`

Deletes catalogues from the piveau-hub.

*   **Delete specific catalogues:**
    ```bash
    python -m meta_harvester delete-catalogues stadt-bern glarus-kt
    ```
*   **Delete ALL catalogues:** If run with no arguments, it will delete every catalogue on the server. **Use with caution!**
    ```bash
    python -m meta_harvester delete-catalogues
    ```


## TODO:
* add publisher name ---> needs fetching from pipe config parameters

* add unit tests

* clean code - type hints, function headers



DEMO:
- creating catalogues (in cli and in piveau-hub)





