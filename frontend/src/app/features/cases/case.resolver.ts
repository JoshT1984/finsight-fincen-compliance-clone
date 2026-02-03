import { inject } from '@angular/core';
import { ResolveFn, Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { CasesService, CaseFileResponse } from '../../shared/services/cases.service';

export const caseResolver: ResolveFn<CaseFileResponse | null> = (route) => {
  const id = route.paramMap.get('id');
  const casesService = inject(CasesService);
  const router = inject(Router);
  if (!id) return of(null);
  const caseId = Number(id);
  if (Number.isNaN(caseId)) return of(null);
  return casesService.getById(caseId).pipe(
    catchError(() => {
      router.navigate(['/cases']);
      return of(null);
    }),
  );
};
