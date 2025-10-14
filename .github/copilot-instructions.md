
# Copilot Instructions for metadata.swiss

## Big Picture Architecture

- The repository is organized into two main domains:
	- `opendata.swiss/metadata/`: Data transformation, harvesting, and orchestration scripts/resources.
	- `opendata.swiss/ui/`: Nuxt 4 Vue.js frontend for catalog browsing and management.
	- `piveau_modules/`: Java modules for metadata harvesting (e.g., CSW import).

## Developer Workflows

- **Local Stack**: Use Docker Compose (`docker compose up`) from `metadata/` to launch all services. Requires a `.env` file (see `.env.example`).
- **Scripts**: Run shell scripts from `metadata/scripts/` for catalogues, vocabularies, and harvesting. Example: `./scripts/catalogues.sh`.
- **UI Development**: In `ui/`, install dependencies (`npm install`) and run the dev server (`npm run dev`). Environment variables are required for API endpoints and GitHub integration.
- **Java Modules**: Build with Maven (`mvn clean install`). To rebuild Docker images, use `docker compose -f docker-compose-consus.yml build --no-cache piveau-consus-importing-csw`.

## Project-Specific Conventions

- **Nuxt UI**:
	- Use `<script setup lang="ts">` and the Composition API.
	- Prefer composables, auto-imported components, and Nuxt features (pages, layouts, middleware).
	- Use SCSS for styling, following the design system.
	- Avoid hardcoding; use config files and environment variables.
	- Props should use `defineProps` with explicit types.
	- Data fetching: Use Nuxt's `useFetch` and `useAsyncData`.
    - We use Nuxt 4 with TypeScript
- **Metadata Scripts**:
	- All shell scripts must be run from the `scripts/` directory.
	- Catalogues and vocabularies are managed via scripts and can be deleted with corresponding `_delete.sh` scripts.
- **Integration Points**:
	- UI communicates with Piveau Hub via environment-configured URLs.
	- Metadata harvesting modules interact via Docker Compose pipes and catalogues.

## Key Files & Directories

- `opendata.swiss/metadata/README.md`: Overview of orchestration and scripts.
- `opendata.swiss/ui/README.md`: UI setup, environment, and development instructions.
- `opendata.swiss/ui/nuxt.config.ts`: Nuxt configuration and module setup.
- `opendata.swiss/ui/app/components/`: Shared Vue components.
- `opendata.swiss/metadata/scripts/`: Shell scripts for data operations.
- `piveau_modules/piveau-consus-importing-csw/README.md`: Java module build and Docker instructions.

## Examples

- To start the stack and populate catalogues:
	```sh
	docker compose up
	./scripts/catalogues.sh
	./scripts/vocabularies.sh
	./scripts/harvest.sh
	```
- To run the UI locally:
	```sh
	cd opendata.swiss/ui
	npm install
	npm run dev
	```

