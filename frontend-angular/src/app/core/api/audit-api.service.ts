import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { AuditEvent, AuditOrderSummary } from '../models/audit-event.model';

@Injectable({ providedIn: 'root' })
export class AuditApiService {
  private readonly baseUrl = '/api/audit/audit';

  constructor(private readonly http: HttpClient) {}

  getOrderSummaries(): Observable<AuditOrderSummary[]> {
    return this.http.get<AuditOrderSummary[]>(`${this.baseUrl}/orders`).pipe(catchError(() => of([])));
  }

  getTimeline(correlationId: string): Observable<AuditEvent[]> {
    return this.http.get<AuditEvent[]>(`${this.baseUrl}/orders/${correlationId}`).pipe(catchError(() => of([])));
  }
}
