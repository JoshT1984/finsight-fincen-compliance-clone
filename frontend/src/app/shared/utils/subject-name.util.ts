/**
 * Shared helpers for resolving friendly subject names from Transactions.
 *
 * Rules:
 * - Transactions are authoritative for customer/subject display names.
 * - Never show CTR-#### or AGGREGATION as a subject name.
 * - Prefer Transaction lookup map first, then event payload names, then subject id.
 */

export type SubjectNameLookup = Record<string, string>;

export function pickFirstString(...values: unknown[]): string | null {
  for (const v of values) {
    if (typeof v === 'string' && v.trim().length > 0) return v;
    if (typeof v === 'number' && Number.isFinite(v)) return String(v);
  }
  return null;
}

export function normalizeCustomerKey(id: string | null | undefined): string | null {
  if (!id) return null;
  const s = String(id).trim();
  if (!s) return null;

  const up = s.toUpperCase();
  if (up.startsWith('CUST-')) return up;
  if (/^\d+$/.test(s)) return `CUST-${s}`;
  return s;
}

export function addSubjectLookupKeys(map: SubjectNameLookup, rawId: string, name: string): void {
  const id = String(rawId || '').trim();
  if (!id) return;
  const n = String(name || '').trim();
  if (!n) return;

  // raw
  map[id] = n;

  // canonical + digits-only
  const canon = normalizeCustomerKey(id);
  if (canon) map[canon] = n;

  if (canon && canon.toUpperCase().startsWith('CUST-')) {
    const digits = canon.substring(5);
    if (digits) map[digits] = n;
  } else if (/^\d+$/.test(id)) {
    map[`CUST-${id}`] = n;
  }
}

export function sanitizeSubjectId(maybeId: string | null): string | null {
  if (!maybeId) return null;
  const id = String(maybeId).trim();
  if (!id) return null;

  const up = id.toUpperCase();
  if (up.startsWith('CTR-')) return null;
  if (up.includes('AGGREGATION')) return null;
  return id;
}

export function resolveSubjectName(args: {
  /** transaction-derived map of subject id -> friendly name */
  lookup: SubjectNameLookup;
  /** best-effort subject id (customerId/subjectKey/sourceEntityId) */
  subjectIdRaw: string | null | undefined;
  /** optional names from event payload */
  eventNames?: Array<string | null | undefined>;
}): { subjectName: string; subjectId: string | null } {
  const subjectIdSanitized = sanitizeSubjectId(args.subjectIdRaw ? String(args.subjectIdRaw) : null);
  const subjectIdCanon = normalizeCustomerKey(subjectIdSanitized);

  const keyTry = subjectIdCanon ?? subjectIdSanitized;
  let subjectName: string | null = null;

  // 1) transactions are authoritative
  if (keyTry && args.lookup[keyTry]) subjectName = args.lookup[keyTry];

  // 2) event payload fallback
  if (!subjectName && args.eventNames?.length) {
    subjectName = pickFirstString(...args.eventNames);
  }

  // 3) final fallback to id
  if (!subjectName && subjectIdSanitized) subjectName = subjectIdSanitized;
  if (!subjectName) subjectName = '—';

  // never allow AGGREGATION as a display name
  if (String(subjectName).toUpperCase().includes('AGGREGATION')) {
    subjectName = subjectIdSanitized ?? '—';
  }

  return { subjectName, subjectId: subjectIdCanon ?? subjectIdSanitized ?? null };
}
