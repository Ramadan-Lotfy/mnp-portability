import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { OperatorService } from './operator.service';

/** Adds the acting operator's `organization` header to every backend call. */
export const organizationInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/')) {
    return next(request);
  }
  const organization = inject(OperatorService).organization();
  return next(request.clone({ setHeaders: { organization } }));
};
