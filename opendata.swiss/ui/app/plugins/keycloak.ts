import Keycloak from "keycloak-js";
import { defineNuxtPlugin } from "#app";

export default defineNuxtPlugin((nuxtApp) => {
  // Configure your Keycloak instance
  const keycloak = new Keycloak({
    url: "https://keycloak.zazukoians.org/",
    realm: "lindas-next",
    clientId: "piveau-hub-ui",
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
