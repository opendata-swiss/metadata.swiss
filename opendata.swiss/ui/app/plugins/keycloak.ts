import Keycloak from "keycloak-js";
import { defineNuxtPlugin } from "#app";


export default defineNuxtPlugin((nuxtApp) => {
  // Get config from runtimeConfig/public with fallback to defaults
  const { keycloakUrl, keycloakRealm, keycloakClientId } = (nuxtApp.$config?.public || {}) as Record<string, string>;
  const keycloak = new Keycloak({
    url: keycloakUrl && typeof keycloakUrl === 'string' && keycloakUrl.length > 0 ? keycloakUrl : "https://keycloak.zazukoians.org/",
    realm: keycloakRealm && typeof keycloakRealm === 'string' && keycloakRealm.length > 0 ? keycloakRealm : "lindas-next",
    clientId: keycloakClientId && typeof keycloakClientId === 'string' && keycloakClientId.length > 0 ? keycloakClientId : "piveau-hub-ui",
  });

  // Optionally, expose Keycloak instance globally
  nuxtApp.provide("keycloak", keycloak);

  // Only check SSO status on page load, do not redirect
  if (import.meta.client) {
    keycloak.init({
      onLoad: "check-sso",
      pkceMethod: "S256",
      flow: "standard",
      silentCheckSsoRedirectUri: window.location.origin + "/silent-check-sso.html",
    });
    // Expose login/logout methods for manual control
    nuxtApp.provide("keycloakLogin", () => keycloak.login());
    nuxtApp.provide("keycloakLogout", () => keycloak.logout());
  }
});
