export default defineNuxtRouteMiddleware((from, to) => {
  const { loggedIn } = useUserSession()
  const login = useLoginWithRedirect()

  if (!loggedIn.value) {
    login(to.fullPath)
  }
})
