import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { StockItem } from '../models/stock.model';

export interface ServiceHealth {
  service: string;
  status: 'healthy' | 'unhealthy' | 'unreachable' | string;
  statusCode?: number;
}

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly baseUrl = '/api/audit/admin';

  constructor(private readonly http: HttpClient) {}

  getServiceHealth(): Observable<ServiceHealth[]> {
    return this.http.get<ServiceHealth[]>(`${this.baseUrl}/service-health`).pipe(catchError(() => of([])));
  }

  getWarehouseInventory(): Observable<StockItem[]> {
    return this.http.get<StockItem[]>(`${this.baseUrl}/warehouse/inventory`).pipe(catchError(() => of([])));
  }

  createWarehouseProduct(body: Partial<StockItem>): Observable<StockItem> {
    return this.http.post<StockItem>(`${this.baseUrl}/warehouse/inventory`, body);
  }

  updateWarehouseProduct(productId: string, body: Partial<StockItem>): Observable<StockItem> {
    return this.http.put<StockItem>(`${this.baseUrl}/warehouse/inventory/${productId}`, body);
  }

  setWarehouseStock(productId: string, availableQuantity: number): Observable<StockItem> {
    return this.http.patch<StockItem>(`${this.baseUrl}/warehouse/inventory/${productId}/stock`, {
      availableQuantity
    });
  }

  deleteWarehouseProduct(productId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/warehouse/inventory/${productId}`);
  }
}
