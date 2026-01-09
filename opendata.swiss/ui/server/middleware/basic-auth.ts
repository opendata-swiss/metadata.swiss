const BASIC_AUTH_USERNAME = 'api-tuner'
const BASIC_AUTH_PASSWORD = 'e2e-tests'

export default defineEventHandler(async (event) => {
  const { apiTunerTests } = useRuntimeConfig()

  if (!(event.path === '/api/showcases' && event.method === 'POST')) {
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
