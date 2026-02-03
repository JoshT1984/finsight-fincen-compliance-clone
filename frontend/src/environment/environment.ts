/**
 * Development environment configuration.
 * Each microservice has its own base URL for local development.
 */
export const environment = {
  production: false,
  documentsApiBaseUrl: 'http://localhost:8081',
  suspectApiBaseUrl: 'http://localhost:8082',
  complianceApiBaseUrl: 'http://localhost:8085',
  /** Identity and Authorization microservice (identity-auth-service). No trailing slash. */
  identityApiBaseUrl: 'http://localhost:8083',
  /** Compliance event microservice (compliance-event-service). No trailing slash. */
  complianceApiBaseUrl: 'http://localhost:8085',
};
