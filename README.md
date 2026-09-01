# metadata.swiss

Monorepo for the [opendata.swiss](https://opendata.swiss) metadata infrastructure, including the portal frontend application and custom Piveau Consus modules.

## Projects Overview

The repository is organized as an npm workspace managing both JavaScript/TypeScript applications and Java/Maven modules:

| Package / Artifact Name | Directory Path | Tech Stack | Description |
| :--- | :--- | :--- | :--- |
| `piveau-opendata-swiss` | `opendata.swiss/ui` | Nuxt 4, Vue 3, TypeScript | The main web portal user interface for opendata.swiss |
| `piveau-consus-filter` | `opendata.swiss/piveau_modules/piveau-consus-filter` | Java 17, Vert.x, Maven | Piveau Consus pipe module for filtering RDF datasets |
| `piveau-consus-importing-csw` | `opendata.swiss/piveau_modules/piveau-consus-importing-csw` | Java 17, Vert.x, Maven | Piveau Consus pipe module for importing data via CSW |
| `piveau-consus-importing-showcases` | `opendata.swiss/piveau_modules/piveau-consus-importing-showcases` | Java 17, Vert.x, Maven | Piveau Consus pipe module for harvesting showcases |
| `piveau-consus-patching` | `opendata.swiss/piveau_modules/piveau-consus-patching` | Java 17, Vert.x, Maven | Piveau Consus pipe module for metadata and catalogue patching |

Additional components in the repository:
* `opendata.swiss/metadata`: Metadata catalogues, pipelines, system tests, and harvester scripts.
* `opendata.swiss/archimate`: Architecture models and documentation.

---

## Versioning with Changesets

This monorepo uses [Changesets](https://github.com/changesets/changesets) (`@changesets/cli`) to manage semantic versioning, changelogs, and release tracking across all projects.

### How Changesets Versions JS and Maven Projects

Changesets natively operates on npm workspaces configured in the root `package.json`:

```json
{
  "workspaces": [
    "opendata.swiss/ui",
    "opendata.swiss/piveau_modules/*"
  ]
}
```

* **UI Application (`opendata.swiss/ui`)**: Uses standard `package.json` metadata for the Nuxt/Vue frontend.
* **Piveau Maven Modules (`opendata.swiss/piveau_modules/*`)**: Each Java module contains both a Maven `pom.xml` and a minimal `package.json` defining `"name"` and `"version"`. This lightweight bridge allows `@changesets/cli` and GitHub Actions to treat Java/Maven projects as standard workspace packages, enabling unified changelog tracking and version management.

### Configuration

Changesets settings are defined in `.changeset/config.json`:
* `baseBranch`: `main`
* `changelog`: Uses `@changesets/changelog-github` linked to `opendata-swiss/metadata.swiss`.
* `commit`: Enabled for automated version commit generation.

---

## Developer Workflow

### 1. Adding a Changeset

When submitting a pull request that modifies one or more projects, add a changeset entry:

```bash
npx changeset
```

1. Select the packages changed (e.g., `piveau-opendata-swiss`, `piveau-consus-filter`).
2. Choose the SemVer bump type for each selected package:
   * **patch**: Bug fixes, minor adjustments, dependencies.
   * **minor**: New features or backward-compatible additions.
   * **major**: Breaking changes.
3. Provide a clear summary description of the changes.
4. Commit the newly generated file in `.changeset/*.md` to your branch and include it in your Pull Request.

### 2. Automated Release Workflow

Releases are handled automatically via GitHub Actions:

1. **Push to `main`**: When PRs with changesets are merged into `main`, the `.github/workflows/release.yaml` workflow runs `changesets/action@v2`.
2. **Version PR**: Changesets opens or updates a pull request titled `"Version Packages"`. This PR increments versions in `package.json` and updates `CHANGELOG.md` files for affected packages.
3. **Release & Tagging**: When the `"Version Packages"` PR is merged into `main`, releases and git tags are created automatically.
