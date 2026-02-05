import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs/operators';
import { IdentityService } from '../services/identity.service';

/**
 * Used on the public landing route ('').
 * Redirects signed-in users to the dashboard so they see the app home instead.
 * Exception: if the URL has a fragment (e.g. /#about), allow access so section
 * anchor links and test links work for logged-in users.
 */
export const publicLandingGuard: CanActivateFn = (_, state) => {
  const identity = inject(IdentityService);
  const router = inject(Router);
  const hasFragment = !!state?.root?.fragment;

  return identity.isLoggedIn$.pipe(
    take(1),
    map((loggedIn) => {
      if (!loggedIn) return true;
      if (hasFragment) return true; 
      return router.createUrlTree(['/dashboard']);
    })
  );
};
