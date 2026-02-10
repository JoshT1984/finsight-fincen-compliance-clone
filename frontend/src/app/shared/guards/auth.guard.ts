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
    map((loggedIn) => {
      // If the app is still bootstrapping but a token is present, allow navigation.
      // Components/interceptors will handle any 401s and redirect if needed.
      const hasToken = !!localStorage.getItem('authToken');
      return loggedIn || hasToken ? true : router.createUrlTree(['']);
    }));
};
