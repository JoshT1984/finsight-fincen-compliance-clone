import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription, timer, forkJoin, of } from 'rxjs';
import { switchMap, scan, takeWhile, catchError, map } from 'rxjs/operators';

import { ComplianceEventsService } from '../../../services/compliance-events.service';
import { ComplianceEventDto } from '../../../models/compliance-event-dto.interface';

type CtrDetailResponse = {
  ctr?: {
    customerName?: string | null;
    suspectMinimal?: { primaryName?: string | null } | null;
    severityScore?: number | null;
    [k: string]: any;
  };
  ctrFormData?: {
    customerName?: string | null;
    subjectName?: string | null;
    sourceSubjectType?: string | null;
    suspicionScore?: number | null;
    [k: string]: any;
  };
  [k: string]: any;
};

@Component({
  selector: 'app-ctr-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ctr-list.component.html',
  styleUrls: ['./ctr-list.component.css'],
})
export class CtrListComponent implements OnInit, OnDestroy {
  events: Array<
    ComplianceEventDto & {
      subjectName?: string;
      subjectType?: string;
      isLinked?: boolean;
      linkedLabel?: string;
      suspicionScore?: number | null;
    }
  > = [];

  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  loading = false;
  error: string | null = null;

  private pollingSub?: Subscription;

  constructor(private complianceEventsService: ComplianceEventsService) {}

  ngOnInit(): void {
    this.loading = true;
    this.error = null;
    this.startPolling(0, 10, 1500);
  }

  ngOnDestroy(): void {
    this.pollingSub?.unsubscribe();
  }

  startPolling(pageIndex: number, maxAttempts: number, delayMs: number): void {
    this.pollingSub?.unsubscribe();

    this.pollingSub = timer(0, delayMs)
      .pipe(
        switchMap(() =>
          this.complianceEventsService.getCtrEvents(pageIndex, this.pageSize).pipe(
            catchError(() => of(null))
          )
        ),
        scan((_, res) => ({ res, attempt: _.attempt + 1 }), { res: null as any, attempt: 0 }),
        takeWhile(
          ({ res, attempt }) =>
            // keep polling while we have no response and haven't hit max attempts
            (res === null && attempt < maxAttempts) ||
            // or stop immediately on first success
            (res !== null && attempt === 1),
          true
        )
      )
      .subscribe(({ res, attempt }) => {
        if (res !== null) {
          this.loading = false;
          this.error = null;
          this.processCtrEvents(res);
          this.pollingSub?.unsubscribe();
          return;
        }

        if (attempt >= maxAttempts) {
          this.loading = false;
          this.error = 'Failed to load CTRs';
          this.events = [];
          this.pollingSub?.unsubscribe();
        }
      });
  }

  processCtrEvents(response: any): void {
    this.totalElements =
      response?.totalElements ??
      (response?.content ? response.content.length : Array.isArray(response) ? response.length : 0);

    this.pageIndex = response?.number ?? 0;
    this.pageSize = response?.size ?? this.pageSize;

    const events: ComplianceEventDto[] =
      response?.content ?? (Array.isArray(response) ? response : []);

    if (!events.length) {
      this.events = [];
      return;
    }

    forkJoin(
      events.map((event) =>
        this.complianceEventsService.getCtrDetail(event.eventId).pipe(
          map((detailUnknown: unknown) => {
            const detail = (detailUnknown ?? {}) as CtrDetailResponse;
            const ctr = detail.ctr ?? {};
            const form = detail.ctrFormData ?? {};

            const subjectName =
              ctr.customerName ??
              form.customerName ??
              form.subjectName ??
              '—';

            const subjectType =
              form.sourceSubjectType ??
              '—';

            const isLinked = !!(event as any).suspectId;

            let linkedLabel = '';
            if (isLinked && ctr.suspectMinimal?.primaryName) {
              linkedLabel = `Linked: ${ctr.suspectMinimal.primaryName}`;
            } else if (isLinked) {
              linkedLabel = 'Linked';
            }

            const suspicionScore =
              form.suspicionScore ??
              ctr.severityScore ??
              (event as any).severityScore ??
              null;

            return {
              ...event,
              subjectName,
              subjectType,
              isLinked,
              linkedLabel,
              suspicionScore,
            };
          }),
          catchError(() =>
            of({
              ...event,
              subjectName: '—',
              subjectType: '—',
              isLinked: !!(event as any).suspectId,
              linkedLabel: '',
              suspicionScore: (event as any).severityScore ?? null,
            })
          )
        )
      )
    ).subscribe((rows) => {
      this.events = rows as any;
    });
  }

  suspicionLabel(score: number | null): string {
    if (score === null) return '—';
    if (score >= 80) return 'High';
    if (score >= 50) return 'Medium';
    return 'Low';
  }
}
