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

  // Optionally, handle automatic login
  if (import.meta.client) {
    keycloak
      .init({
        onLoad: "login-required", // or 'check-sso' for silent check
        pkceMethod: "S256",
        flow: "standard",
      })
      .then((authenticated) => {
        if (!authenticated) {
          keycloak.login();
        }
      });
  }
});
