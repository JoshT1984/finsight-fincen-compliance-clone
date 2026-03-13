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
  /** Compliance events (CTR/SAR) microservice. Use '' for same-origin, or full URL for AWS. */
  complianceApiBaseUrl: '',
  /** Identity and Authorization microservice. Use '' for same-origin, or full URL for AWS. */
  identityApiBaseUrl: '',
  /** Geoapify API key (set for your deployed environment if you want autocomplete in prod). */
  geoapifyApiKey: '21f203e496c149e6a87c7c90cc4f6a5f',
};
