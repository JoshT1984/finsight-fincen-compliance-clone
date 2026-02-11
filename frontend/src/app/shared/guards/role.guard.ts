import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { RoleService } from '../services/role.service';

/**
 * Protects routes by role. Use with route data: { allowedRoles: ['ANALYST', 'COMPLIANCE_USER'] }.
 * Redirects to /dashboard if the current user's role is not in allowedRoles.
 * Must be used after authGuard so the user is logged in and profile is available.
 */
export const roleGuard: CanActivateFn = (route) => {
  const roleService = inject(RoleService);
  const router = inject(Router);

  const allowedRoles = route.data['allowedRoles'] as string[] | undefined;
  if (!allowedRoles?.length) {
    return true;
  }

  const role = roleService.roleName();
  if (role && allowedRoles.includes(role)) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
