# Metadata

## Structure

### Piveau resources

- `piveau_catalogues/`: Piveau catalogues
- `piveau_pipes/`: Piveau pipes
- `piveau_scripts/`: Piveau scripts
- `piveau_vocabularies/`: Piveau vocabularies

### Other resources

- `scripts/`: Various scripts, needs to be run from this directory (`./scripts/your_script.sh`)
- `.env`: Configure some environment variables (not in git)

## Local Stack

It is possible to start a local stack, in order to quickly try changes locally.

For this, you need to have Docker and Docker Compose installed.

Then, you will need to create a `.env` file, based on the `.env.example` file, and fill in the secrets.

Finally, you can start the stack with:

```sh
docker-compose up -d # You can ignore the `-d` to see the logs in real time
```

You can open the UI at [http://localhost:8080](http://localhost:8080).

To create the catalogues, run:

```sh
./scripts/catalogues.sh
```

To install the default vocabularies, open the shell at [http://localhost:8085/shell.html](http://localhost:8085/shell.html) and run `installVocabularies`.

To add custom vocabularies, run:

```sh
./scripts/vocabularies.sh
```


And to trigger a harvest, run:

```sh
./scripts/harvest.sh
```

If you open the UI at [http://localhost:8080](http://localhost:8080), you should see that the catalogues and datasets are now visible.

To remove the catalogues, run:

```sh
./scripts/catalogues_delete.sh
```

To stop the stack, run:

```sh
docker-compose down
```
