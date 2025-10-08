# Setup project

First, activate your venv
`eval $(poetry env activate)`

Check what what are you using
`poetry env info`

Install dependencies
`poetry install`

# Run it
This code generates pipes for all geoharvesters on CKAN repository.

`python -m meta_harvester`

Or, if you need to define environment variables:

`API_KEY_HUB=yourRepoApiKey HUB_REPO_ENDPOINT=http://localhost:8081 python -m meta_harvester`

By default, `API_KEY_HUB=yourRepoApiKey` and  `HUB_REPO_ENDPOINT=http://localhost:8081`

TODO:
* test on scale - run all pipes

* add cli command (within main.py) for:
- creating all catalogues
- creating all pipes
- creating single catalogue
- deleting all catalogues
- deleting single catalogue

* add unit tests
* add publisher website ---> needs fetching it from organizations api (["result"]["url"]): https://ckan.opendata.swiss/api/3/action/organization_show?id=cnai

* clean code - remove overwritten pipes and catalogues
* clean code - logger instead of print
* clean code - type hints

DEMO:
- creating catalogues (in cli and in piveau-hub)





