import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs/operators';
import { IdentityService } from '../services/identity.service';

/**
 * Protects routes that require a signed-in user.
 * Redirects to the public landing (home) if not logged in.
 */
export const authGuard: CanActivateFn = () => {
  const identity = inject(IdentityService);
  const router = inject(Router);

  return identity.isLoggedIn$.pipe(
    take(1),
    map((loggedIn) => (loggedIn ? true : router.createUrlTree([''])))
  );
};
