export function useLoginWithRedirect(returnTo: string) {
  const authReturnTo = useCookie('auth-return-to')

  return () => {
    authReturnTo.value = returnTo
    window.location.href = '/api/auth/keycloak'
  }
}

export const useLogin = useLoginWithRedirect.bind(null, '/')
