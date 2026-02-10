// src/app/models/compliance-event-dto.interface.ts

export type EventType = 'CTR' | 'SAR';

export interface ComplianceEventDto {
  eventId: number;

  // ✅ was string; now matches the enum/union used elsewhere
  eventType: EventType;

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
