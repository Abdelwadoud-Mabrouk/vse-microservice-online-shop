import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const adminAuthInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/audit/admin')) {
    return next(request);
  }

  const authorization = inject(AuthService).adminAuthorization();
  return next(authorization
    ? request.clone({ setHeaders: { Authorization: authorization } })
    : request);
};
