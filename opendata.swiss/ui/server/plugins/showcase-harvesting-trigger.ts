import { App } from '@octokit/app'
import { defineNitroPlugin } from '#imports'

const executed = new WeakSet()

export default defineNitroPlugin(async (nitro) => {
  if (import.meta.dev) {
    console.warn('Skipping Showcase Harvesting trigger in development mode')
    return
  }

  if (executed.has(nitro) || process.env.DISABLE_SHOWCASE_HARVESTING === 'true') {
    return
  }

  executed.add(nitro)

  const appId = process.env.GITHUB_APP_ID
  const installationId = process.env.GITHUB_APP_INSTALLATION_ID
  const privateKey = process.env.GITHUB_APP_PRIVATE_KEY
  const githubOrg = process.env.GITHUB_OWNER
  const cmsRepo = process.env.GITHUB_CMS_REPO
  const appRepo = process.env.GITHUB_APP_REPO

  if (!appId || !installationId || !privateKey) {
    console.error('Missing required environment variables for GitHub App authentication')
    return
  }

  try {
    const app = new App({ appId, privateKey })
    const octokit = await app.getInstallationOctokit(Number.parseInt(installationId))
    const { token } = await octokit.auth({ type: 'installation' }) as { token: string }

    let environment = 'TEST'
    if (cmsRepo === 'opendata-swiss-cms-content-int') {
      environment = 'INT'
    }
    else if (cmsRepo === 'opendata-swiss-cms-content') {
      environment = 'PROD'
    }

    const response = await fetch(`https://api.github.com/repos/${githubOrg}/${appRepo}/actions/workflows/script-manual-trigger.yaml/dispatches`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/vnd.github+json',
        'Content-Type': 'application/json',
        'User-Agent': 'Nuxt-Nitro-Plugin',
      },
      body: JSON.stringify({
        ref: 'main',
        inputs: {
          environment,
          action: 'HARVEST_SHOWCASES',
        },
      }),
    })

    if (!response.ok) {
      const errorText = await response.text()
      console.error(`Failed to trigger GitHub Action: ${response.status} ${response.statusText}`, errorText)
    }
    else {
      console.info(`Successfully triggered GitHub Action for environment: ${environment}`)
    }
  }
  catch (e) {
    console.error('Error triggering GitHub Action:', e)
  }
})
