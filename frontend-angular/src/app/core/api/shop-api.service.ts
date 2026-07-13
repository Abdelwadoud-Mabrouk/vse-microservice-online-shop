import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { CreateOrderRequest, Order } from '../models/order.model';
import { Product } from '../models/product.model';
import { Customer } from '../models/customer.model';

const demoProducts: Product[] = [
  {
    productId: '11111111-1111-4111-8111-111111111111',
    sku: 'SKU-CHAIR-OAK',
    name: 'Oak Dining Chair',
    description: 'Solid oak chair with a calm Scandinavian silhouette.',
    price: 129.99,
    currency: 'EUR',
    category: 'Chairs',
    imageUrl: 'https://images.unsplash.com/photo-1517705008128-361805f42e86?auto=format&fit=crop&w=900&q=80'
  },
  {
    productId: '22222222-2222-4222-8222-222222222222',
    sku: 'SKU-DESK-OSLO',
    name: 'Oslo Work Table',
    description: 'Compact table for focused work, study rooms and home offices.',
    price: 349.99,
    currency: 'EUR',
    category: 'Tables',
    imageUrl: 'https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?auto=format&fit=crop&w=900&q=80'
  },
  {
    productId: '33333333-3333-4333-8333-333333333333',
    sku: 'SKU-SOFA-LIMA',
    name: 'Lima Lounge Sofa',
    description: 'Soft two-seat sofa with clean lines for modern living rooms.',
    price: 899.99,
    currency: 'EUR',
    category: 'Sofas',
    imageUrl: 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=900&q=80'
  },
  {
    productId: '44444444-4444-4444-8444-444444444444',
    sku: 'SKU-LAMP-NOVA',
    name: 'Nova Floor Lamp',
    description: 'Warm ambient floor lamp for reading corners and bedrooms.',
    price: 189.99,
    currency: 'EUR',
    category: 'Lighting',
    imageUrl: 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=80'
  }
];

@Injectable({ providedIn: 'root' })
export class ShopApiService {
  private readonly baseUrl = '/api/shop/api/v1';

  constructor(private readonly http: HttpClient) {}

  getProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.baseUrl}/products`).pipe(catchError(() => of(demoProducts)));
  }

  getProduct(productId: string): Observable<Product | undefined> {
    return this.http
      .get<Product>(`${this.baseUrl}/products/${productId}`)
      .pipe(catchError(() => of(demoProducts.find((product) => product.productId === productId))));
  }

  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(`${this.baseUrl}/orders`, request);
  }

  getOrder(orderId: string): Observable<Order> {
    return this.http.get<Order>(`${this.baseUrl}/orders/${orderId}`);
  }

  getCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>(`${this.baseUrl}/customers`).pipe(catchError(() => of([])));
  }
}
