/**
 * Production environment configuration.
 * For AWS deployment: update each URL to your API Gateway/Load Balancer,
 * or use '' for same-origin (when APIs are proxied behind the same domain).
 */
export const environment = {
  production: true,
  /** Documents microservice. Use '' for same-origin, or full URL for AWS. */
  documentsApiBaseUrl: '',
  /** Suspect registry microservice. Use '' for same-origin, or full URL for AWS. */
  suspectApiBaseUrl: '',
  /** Identity and Authorization microservice. Use '' for same-origin, or full URL for AWS. */
  identityApiBaseUrl: '',
  /** Compliance event microservice. Use '' for same-origin, or full URL for AWS. */
  complianceApiBaseUrl: '',
};
