import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { Invoice } from '../models/invoice.model';

@Injectable({ providedIn: 'root' })
export class InvoiceApiService {
  private readonly baseUrl = '/api/invoices';

  constructor(private readonly http: HttpClient) {}

  getInvoices(customerEmail?: string): Observable<Invoice[]> {
    const params = customerEmail ? new HttpParams().set('customerEmail', customerEmail) : undefined;
    return this.http.get<Invoice[]>(`${this.baseUrl}/invoices`, { params }).pipe(catchError(() => of([])));
  }

  getPdfUrl(invoiceId: string): string {
    return `${this.baseUrl}/invoices/${invoiceId}/pdf`;
  }
}
