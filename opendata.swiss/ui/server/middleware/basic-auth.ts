const users: Record<string, { password: string, email: string }> = {
  'api-tuner': {
    password: 'e2e-tests',
    email: 'john@example.com',
  },
  'jane': {
    password: 'foobar',
    email: 'jane@example.com',
  },
}

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
    console.warn('Authenticating API with basic auth')

    const authHeader = event.node.req.headers['authorization'] || ''
    const encodedCredentials = authHeader.split(' ')[1] || ''
    const decodedCredentials = Buffer.from(encodedCredentials, 'base64').toString('utf-8')
    const [username, password] = decodedCredentials.split(':')

    if (username && users[username]?.password === password) {
      user = {
        name: username,
        email: users[username]!.email,
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
