export interface ComplianceEventDto {
  eventId: number;
  eventType: string;
  sourceSystem: string;
  sourceEntityId: string;
  eventTime: string;
  totalAmount: number;
  status: string;
  severityScore: number;
  createdAt: string;
  suspectId?: string | number;
  [key: string]: any;
}
