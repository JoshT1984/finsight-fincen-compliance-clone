export interface CtrFormData {
  customerName?: string | null;
  subjectName?: string | null;
  subjectKey?: string | null;
  sourceSubjectType?: string | null;

  suspicionScore?: number | null;
  triggeredRules?: string[] | null;

  contributingTxnIds?: Array<number | string> | null;

  // allow extra backend keys without making everything "any"
  [key: string]: unknown;
}

export interface SuspectMinimal {
  primaryName?: string | null;
  [key: string]: unknown;
}

export interface CtrDetailCtr {
  eventId?: number | null;
  sourceEntityId?: string | null;
  severityScore?: number | null;
  suspectId?: number | null;

  // sometimes returned in your backend response
  customerName?: string | null;

  suspectMinimal?: SuspectMinimal | null;

  [key: string]: unknown;
}

export interface CtrDetailResponse {
  ctr?: CtrDetailCtr;
  ctrFormData?: CtrFormData | null;

  [key: string]: unknown;
}
