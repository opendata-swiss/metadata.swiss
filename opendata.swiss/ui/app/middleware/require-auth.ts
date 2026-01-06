export default defineNuxtRouteMiddleware((from, to) => {
  const { loggedIn } = useUserSession()
  const login = useLoginWithRedirect(to.fullPath)

  if (!loggedIn.value) {
    login()
  }
})
