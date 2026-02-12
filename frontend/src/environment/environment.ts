/**
 * Development environment configuration.
 * Each microservice has its own base URL for local development.
 */
export const environment = {
  production: false,
  documentsApiBaseUrl: 'http://localhost:8081',
  suspectApiBaseUrl: 'http://localhost:8082',
  /** Identity and Authorization microservice (identity-auth-service). No trailing slash. */
  identityApiBaseUrl: 'http://localhost:8083',
  /** Compliance event microservice (compliance-event-service). No trailing slash. */
  complianceApiBaseUrl: 'http://localhost:8085',

  /**
   * Geoapify API key used for location autocomplete in the CTR transaction form.
   * Get a free key at Geoapify and paste it here for local dev.
   */
  // NOTE: For production, do NOT hardcode this in your repo. Use a backend proxy or CI-injected build env.
  geoapifyApiKey: '21f203e496c149e6a87c7c90cc4f6a5f',
};
