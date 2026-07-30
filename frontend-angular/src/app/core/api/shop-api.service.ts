import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, catchError, of} from 'rxjs';
import {CheckoutResponse, CreateOrderRequest, Order, OrderResponse} from '../models/order.model';
import {Product} from '../models/product.model';
import {Customer} from '../models/customer.model';

// @ts-ignore
const demoProducts: Product[] = [
    {
        id: '11111111-1111-4111-8111-111111111111',
        sku: 'SKU-CHAIR-OAK',
        name: 'Oak Dining Chair',
        description: 'Solid oak chair with a calm Scandinavian silhouette.',
        price: 129.99,
        categoryName: 'Chairs',
        imageUrls: [
            'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=80'
        ],
        active: false,
        slug: ''
    },
    {
        id: '22222222-2222-4222-8222-222222222222',
        sku: 'SKU-DESK-OSLO',
        name: 'Oslo Work Table',
        description: 'Compact table for focused work, study rooms and home offices.',
        price: 349.99,
        categoryName: 'Tables',
        imageUrls: [
            'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=80'
        ],
        active: false,
        slug: ''
    },
    {
        id: '33333333-3333-4333-8333-333333333333',
        sku: 'SKU-SOFA-LIMA',
        name: 'Lima Lounge Sofa',
        description: 'Soft two-seat sofa with clean lines for modern living rooms.',
        price: 899.99,

        categoryName: 'Sofas',
        imageUrls: [
            'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=80'
        ],
        active: false,
        slug: ''
    },
    {
        id: '44444444-4444-4444-8444-444444444444',
        sku: 'SKU-LAMP-NOVA',
        name: 'Nova Floor Lamp',
        description: 'Warm ambient floor lamp for reading corners and bedrooms.',
        price: 189.99,
        categoryName: 'Lighting',
        imageUrls: [
            'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=80'
        ],
        active: false,
        slug: ''
    }
];

@Injectable({providedIn: 'root'})
export class ShopApiService {
    private readonly baseUrl = '/api/shop/api/v1';

    constructor(private readonly http: HttpClient) {
    }

    getProducts(): Observable<Product[]> {
        return this.http.get<Product[]>(`${this.baseUrl}/products`).pipe(catchError(() => of(demoProducts)));
    }

    getProduct(productId: string): Observable<Product | undefined> {
        return this.http
            .get<Product>(`${this.baseUrl}/products/${productId}`)
            .pipe(catchError(() => of(demoProducts.find((product) => product.id === productId))));
    }

    createOrder(request: CreateOrderRequest): Observable<CheckoutResponse> {
        return this.http.post<CheckoutResponse>(`${this.baseUrl}/orders`, request);
    }

    getOrder(orderId: string): Observable<OrderResponse> {
        return this.http.get<OrderResponse>(`${this.baseUrl}/orders/${orderId}`);
    }

    getCustomers(): Observable<Customer[]> {
        return this.http.get<Customer[]>(`${this.baseUrl}/customers`).pipe(catchError(() => of([])));
    }

    capturePaypalOrder(
        paypalOrderId: string
    ) {

        return this.http.post<string>(
            `${this.baseUrl}/orders/capture/${paypalOrderId}`,
            {}
        );

    }
}
