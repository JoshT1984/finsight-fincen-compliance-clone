import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { ComplianceEventsService } from '../../services/compliance-events.service';
import { ComplianceEventDto } from '../../models/compliance-event-dto.interface';
import { RoleService } from '../../shared/services/role.service';

@Component({
  selector: 'app-sar-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sar-detail.component.html',
  styleUrls: ['./sar-detail.component.css'],
})
export class SarDetailComponent implements OnInit {
  loading = false;
  error: string | null = null;
  sar: ComplianceEventDto | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private events: ComplianceEventsService,
    public roleService: RoleService,
  ) {}

  // ✅ Helper to safely get SAR id for template
  get sarId(): string | number {
    const sar: any = this.sar;
    if (!sar) return '—';
    return sar.eventId ?? sar.id ?? '—';
  }

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id');
    const id = raw != null ? Number(raw) : NaN;

    if (!Number.isFinite(id)) {
      this.error = 'Invalid SAR id.';
      return;
    }

    this.load(id);
  }

  back(): void {
    this.router.navigate(['/sars']);
  }

  private load(id: number): void {
    this.loading = true;
    this.error = null;
    this.sar = null;

    this.events.getEventById(id).subscribe({
      next: (dto) => {
        // Defensive: this endpoint returns any compliance event; ensure it's a SAR.
        const eventType = String(
          (dto as any)?.eventType ?? (dto as any)?.event_type ?? '',
        ).toUpperCase();

        if (eventType && eventType !== 'SAR') {
          this.error = `Event ${id} is not a SAR.`;
          this.loading = false;
          return;
        }

        this.sar = dto;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.error = 'Failed to load SAR details.';
        this.loading = false;
      },
    });
  }

  // Simple helpers (no hardcoded colors)
  badgeClass(status: any): string {
    const s = String(status ?? '').toUpperCase();
    if (s === 'SUBMITTED') return 'badge badge--success';
    if (s === 'DRAFT') return 'badge badge--warn';
    if (s === 'REJECTED') return 'badge badge--danger';
    return 'badge';
  }
}
