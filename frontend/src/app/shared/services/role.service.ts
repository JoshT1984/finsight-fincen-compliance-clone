import { Injectable, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { IdentityService } from './identity.service';

/** Role names aligned with backend (JWT / SecurityContextUtils). */
export const ROLES = {
  ANALYST: 'ANALYST',
  COMPLIANCE_USER: 'COMPLIANCE_USER',
  LAW_ENFORCEMENT_USER: 'LAW_ENFORCEMENT_USER',
} as const;

@Injectable({ providedIn: 'root' })
export class RoleService {
  private readonly identity = inject(IdentityService);

  private readonly profile = toSignal(this.identity.profile$, { initialValue: null });

  /** Current user's role name, or null if not logged in / unknown. */
  roleName(): string | null {
    return this.profile()?.roleName ?? null;
  }

  isComplianceUser(): boolean {
    return ROLES.COMPLIANCE_USER === this.roleName();
  }

  isAnalyst(): boolean {
    return ROLES.ANALYST === this.roleName();
  }

  isLawEnforcement(): boolean {
    return ROLES.LAW_ENFORCEMENT_USER === this.roleName();
  }
}
