# Nuxt Minimal Starter

Look at the [Nuxt documentation](https://nuxt.com/docs/getting-started/introduction) to learn more.

## Setup

Make sure to install dependencies:

```bash
# npm
npm install

# pnpm
pnpm install

# yarn
yarn install

# bun
bun install
```

Create a `.env` file with required variables:

```bash
NUXT_OAUTH_KEYCLOAK_CLIENT_SECRET:
```

and optionally:

```bash
NUXT_PUBLIC_PIVEAU_HUB_SEARCH_URL=http://localhost:8084
NUXT_PUBLIC_PIVEAU_HUB_REPO_URL=http://localhost:8081
GITHUB_OWNER=zazukoians
GITHUB_REPO=metadata.swiss
GITHUB_BASE_BRANCH=main
GITHUB_TOKEN=
```

The `*PIVEAU_HUB*` can be overridden to point to another Piveau instance. 
By default, `https://piveau-hub-repo.test.ods.zazukoians.org` is used.

The personal access token requires contents and pull request permissions.

## Development Server

Start the development server on `http://localhost:3000`:

```bash
# npm
npm run dev

# pnpm
pnpm dev

# yarn
yarn dev

# bun
bun run dev
```

### API tests

To run API tests, set the environment variable below to enable basic auth for the API endpoints:

```bash
NUXT_API_TUNER_TESTS=true
```

Then, run the tests with:

```bash
npm run dev
npm test
```

## Production

Set the environment variables as needed:

- `NUXT_PUBLIC_PIVEAU_HUB_SEARCH_URL`
- `NUXT_PUBLIC_PIVEAU_HUB_REPO_URL`
- `GITHUB_APP_ID`
- `GITHUB_APP_PRIVATE_KEY`
- `GITHUB_APP_INSTALLATION_ID`
- `GITHUB_OWNER`
- `GITHUB_REPO`
- `GITHUB_BASE_BRANCH`
- `NUXT_OAUTH_KEYCLOAK_CLIENT_ID`
- `NUXT_OAUTH_KEYCLOAK_CLIENT_SECRET`
- `NUXT_OAUTH_KEYCLOAK_REALM`

The value for `GITHUB_APP_*` must be that of a GitHub App, installed in the organisation with access to the correct repository.  

Build the application for production:

```bash
# npm
npm run build

# pnpm
pnpm build

# yarn
yarn build

# bun
bun run build
```

Locally preview production build:

```bash
# npm
npm run preview

# pnpm
pnpm preview

# yarn
yarn preview

# bun
bun run preview
```

Check out the [deployment documentation](https://nuxt.com/docs/getting-started/deployment) for more information.
