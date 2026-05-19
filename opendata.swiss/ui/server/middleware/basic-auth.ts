const BASIC_AUTH_USERNAME = 'api-tuner'
const BASIC_AUTH_PASSWORD = 'e2e-tests'

declare module 'nitropack/types' {
  interface NitroRouteRules {
    basicAuth?: boolean
  }
}

export default defineEventHandler(async (event) => {
  const { apiTunerTests } = useRuntimeConfig()

  const routeRules = getRouteRules(event)
  if (!routeRules.basicAuth || !isMethod(event, 'POST')) {
    return
  }

  let user
  if (apiTunerTests) {
    const authHeader = event.node.req.headers['authorization'] || ''
    const encodedCredentials = authHeader.split(' ')[1] || ''
    const decodedCredentials = Buffer.from(encodedCredentials, 'base64').toString('utf-8')
    const [username, password] = decodedCredentials.split(':')

    if (username === BASIC_AUTH_USERNAME && password === BASIC_AUTH_PASSWORD) {
      user = {
        name: username,
        email: 'john@example.com',
      }
    }
    else {
      throw createError({ statusCode: 401, statusMessage: 'Unauthorized' })
    }
  }
  else {
    ({ user } = await requireUserSession(event))
  }

  event.context.user = user
})
