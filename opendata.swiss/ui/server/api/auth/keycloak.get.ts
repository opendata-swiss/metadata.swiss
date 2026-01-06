export default defineOAuthKeycloakEventHandler({
  async onSuccess(event, { user }) {
    await setUserSession(event, {
      user: {
        name: user.name || user.preferred_username || user.email || user.sub,
      },
    })

    const returnTo = getCookie(event, 'auth-return-to') || '/'
    deleteCookie(event, 'auth-return-to')

    return sendRedirect(event, returnTo)
  },
  onError(event, { message }) {
    return sendRedirect(event, '/login?error=' + encodeURIComponent(message || 'OAuth Error'))
  },
})
