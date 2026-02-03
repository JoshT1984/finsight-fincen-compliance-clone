/**
 * Development environment configuration.
 * Each microservice has its own base URL for local development.
 */
export const environment = {
  production: false,
  documentsApiBaseUrl: 'http://localhost:8081',
  suspectApiBaseUrl: 'http://localhost:8082',
  complianceApiBaseUrl: 'http://localhost:8085',
};
