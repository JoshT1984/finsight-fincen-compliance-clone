/**
 * Development environment configuration.
 * Each microservice has its own base URL for local development.
 */
export const environment = {
  production: false,
  /** Documents microservice (documents-cases-service). No trailing slash. */
  documentsApiBaseUrl: 'http://localhost:8081',
  /** Suspect registry microservice (suspect-registry-service). No trailing slash. */
  suspectApiBaseUrl: 'http://localhost:8082',
};
