export interface ComplianceEventDto {
  eventId: number;
  eventType: any; // keep your existing type if you have EventType enum
  sourceSystem: string;
  sourceEntityId: string;
  eventTime: string;

  customerName?: string | null;
  subjectType?: string | null;

  totalAmount: number;
  status: string;
  severityScore: number;
  createdAt: string;

  suspectId?: string | number;
  [key: string]: any;
}
