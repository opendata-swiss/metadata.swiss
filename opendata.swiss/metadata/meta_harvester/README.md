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
