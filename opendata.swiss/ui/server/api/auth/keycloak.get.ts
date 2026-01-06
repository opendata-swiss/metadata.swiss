export default defineOAuthKeycloakEventHandler({
  async onSuccess(event, { user }) {
    await setUserSession(event, {
      user: {
        name: user.name || user.preferred_username || user.email || user.sub,
      },
    })

    return sendRedirect(event, '/')
  },
  onError(event, { message }) {
    return sendRedirect(event, '/login?error=' + encodeURIComponent(message || 'OAuth Error'))
  },
})
