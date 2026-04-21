// Usage: node get-github-app-token.js
// Prints the installation access token to stdout

import { App } from '@octokit/app'

const appId = process.env.GITHUB_APP_ID
const installationId = process.env.GITHUB_APP_INSTALLATION_ID
const privateKey = process.env.GITHUB_APP_PRIVATE_KEY

if (!appId || !installationId || !privateKey) {
  console.error('Missing required environment variables: GITHUB_APP_ID, GITHUB_APP_INSTALLATION_ID, GITHUB_APP_PRIVATE_KEY')
  console.error('Received:', { appId, installationId, privateKeyPresent: !!privateKey })
  process.exit(1)
}

try {
  const app = new App({ appId, privateKey })
  const octokit = await app.getInstallationOctokit(installationId)
  const { token } = await octokit.auth({ type: 'installation' })
  console.log(token)
}
catch (e) {
  console.error('Error creating App or fetching token:', e)
  console.error('Env:', { appId, installationId, privateKeyPresent: !!privateKey })
  process.exit(1)
}
