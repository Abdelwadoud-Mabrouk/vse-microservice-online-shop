import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { Reservation, StockItem } from '../models/stock.model';

@Injectable({ providedIn: 'root' })
export class WarehouseApiService {
  private readonly baseUrl = '/api/warehouse';

  constructor(private readonly http: HttpClient) {}

  getStock(): Observable<StockItem[]> {
    return this.http.get<StockItem[]>(`${this.baseUrl}/inventory`).pipe(catchError(() => of([])));
  }

  updateStock(productId: string, body: Partial<StockItem>): Observable<StockItem> {
    return this.http.put<StockItem>(`${this.baseUrl}/inventory/${productId}`, body);
  }

  setStock(productId: string, availableQuantity: number): Observable<StockItem> {
    return this.http.patch<StockItem>(`${this.baseUrl}/inventory/${productId}/stock`, { availableQuantity });
  }

  getReservations(): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(`${this.baseUrl}/reservations`).pipe(catchError(() => of([])));
  }

  cancelReservation(reservationId: string, correlationId: string): Observable<Reservation> {
    return this.http.delete<Reservation>(`${this.baseUrl}/reservations/${reservationId}`, {
      headers: { 'X-Correlation-Id': correlationId }
    });
  }

  commitReservation(reservationId: string, correlationId: string): Observable<Reservation> {
    return this.http.post<Reservation>(
      `${this.baseUrl}/reservations/${reservationId}/commit`,
      {},
      { headers: { 'X-Correlation-Id': correlationId } }
    );
  }
}
