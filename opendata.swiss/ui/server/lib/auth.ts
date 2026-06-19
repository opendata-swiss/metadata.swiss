import type { NitroRuntimeConfig } from 'nitropack/types'

export async function requestServiceAccountToken(config: NitroRuntimeConfig['oauth']['keycloak'], client: keyof NitroRuntimeConfig['oauth']['keycloak']['clients']) {
  const tokenUrl = new URL(`/realms/${config.realm}/protocol/openid-connect/token`, config.serverUrl)
  const body = new FormData()
  body.append('grant_type', 'client_credentials')
  body.append('client_id', config.clients[client].clientId)
  body.append('client_secret', config.clients[client].clientSecret)

  const res = await fetch(tokenUrl, {
    method: 'POST',
    body,
  })

  if (res.ok) {
    const data = await res.json()
    return data.access_token as string
  }

  throw new Error(`Failed to request service account token: ${res.status} ${res.statusText}`)
}
