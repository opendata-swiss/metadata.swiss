# Metadata

## Structure

### Piveau resources

- `piveau_catalogues/`: Piveau catalogues
- `piveau_pipes/`: Piveau pipes
- `piveau_profile/`: Piveau profile
- `piveau_scripts/`: Piveau scripts
- `piveau_vocabularies/`: Piveau vocabularies

### Other resources

- `scripts/`: Various scripts, needs to be run from this directory (`./scripts/your_script.sh`)
- `.env`: Configure some environment variables (not in git)

## Local Stack

### Start the stack

It is possible to start a local stack, in order to quickly try changes locally.

For this, you need to have Docker and Docker Compose installed.

Then, you will need to create a `.env` file, based on the `.env.example` file, and fill in the secrets.

On Linux, you will need to manually create the volumes before starting the stack:

```sh
mkdir -p volumes/{graphdb,elasticsearch}-data
```

Finally, you can start the stack with:

```sh
docker compose up -d # You can ignore the `-d` to see the logs in real time
```

> [!TIP]
> In case your local device is low on disk space, you may want to skip the low space checks of ElasticSearch, by running the following command:
>
> ```sh
> ./scripts/disable_es_disk_check.sh
> ```

You can open the UI at [http://localhost:8080](http://localhost:8080).

### Create resources

#### Vocabularies

To add the custom vocabularies, run:

```sh
./scripts/vocabularies.sh
```

In case you want to remove them, run:

```sh
./scripts/vocabularies_delete.sh
```

To install the default vocabularies, open the shell at [http://localhost:8085/shell.html](http://localhost:8085/shell.html) and run `installVocabularies`.
This could take some minutes.

#### Catalogues and Harvesting

To create the catalogues, run:

```sh
./scripts/catalogues.sh
```

And to trigger a harvest, run:

```sh
./scripts/harvest.sh
```

Finally, to trigger CMS harvest, ensure that the new UI is ready by opening http://localhost:8008/.

If this is the case, run:

```sh
./scripts/harvest_showcases.sh
```

You can also harvest from local environment by modifying the [`harvest_showcases.sh` script](./scripts/harvest_showcases.sh) to use `http://host.docker.internal:3000` as Piveau endpoint.

If you open the UI at http://localhost:8080, you should see that the catalogues and datasets are now visible.

If you open the new UI at http://localhost:8008, you should also see all data.

To remove the catalogues, run:

```sh
./scripts/catalogues_delete.sh
```

To stop the stack, run:

```sh
docker compose down
```

## OpenTelemetry

The local stack is configured with OpenTelemetry.
You can access the spans and traces using Grafana at [http://localhost:3001](http://localhost:3001).
The traces could be found in the "Explore" section, using the "Jaeger" data source ([direct link](http://localhost:3001/explore?schemaVersion=1&panes=%7B%22bf6%22:%7B%22datasource%22:%22jaeger%22,%22queries%22:%5B%7B%22refId%22:%22A%22,%22datasource%22:%7B%22type%22:%22jaeger%22,%22uid%22:%22jaeger%22%7D,%22queryType%22:%22search%22,%22service%22:%22piveau-consus-importing-rdf%22%7D%5D,%22range%22:%7B%22from%22:%22now-1h%22,%22to%22:%22now%22%7D,%22compact%22:false%7D%7D&orgId=1)).

You can then search for traces of a specific service, e.g., `piveau-consus-importing-rdf`, and explore the spans.
You will need to create the catalogues and trigger a harvest first to see some traces.
