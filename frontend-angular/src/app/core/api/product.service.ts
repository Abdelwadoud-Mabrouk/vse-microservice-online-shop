import {Injectable, inject} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable, tap} from "rxjs";
import {Product} from "../models/product.model";
import {environment} from "../../../environments/environment";

@Injectable({
    providedIn: 'root'
})
export class ProductService {

    private http = inject(HttpClient);

    private baseUrl = environment.apiUrl;
    private apiUrl = `${this.baseUrl}/products`;


    getApiUrl() {
        return this.baseUrl;
    }

    formatImageUrl(path?: string, fallback: string = 'assets/images/product-placeholder.jpg'): string {
        if (!path) {
            return fallback;
        }
        if (path.startsWith('http://') || path.startsWith('https://')) {
            return path;
        }

        const baseUrl = this.getApiUrl();
        const cleanBaseUrl = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;
        const cleanPath = path.startsWith('/') ? path : `/${path}`;

        return `${cleanBaseUrl}${cleanPath}`;
    }

    // ================================
    // Alle aktiven Produkte
    // GET /api/products
    // ================================
    getAllProducts(
        page: number = 0,
        size: number = 12,
        sortBy: string = 'name'
    ) {

        const params = new HttpParams()
            .set('page', page)
            .set('size', size)
            .set('sortBy', sortBy);


        return this.http.get<any>(
            this.apiUrl,
            {params}
        );
    }


    // ================================
    // Produkte nach Kategorie
    // GET /api/products/category/{slug}
    // ================================
    getProductsByCategory(
        slug: string,
        page: number = 0,
        size: number = 12,
        sortBy: string = 'name'
    ) {

        const params = new HttpParams()
            .set('page', page)
            .set('size', size)
            .set('sortBy', sortBy);


        return this.http.get<any>(
            `${this.apiUrl}/category/${slug}`,
            {params}
        );
    }


    getProduct(productId: string): Observable<Product> {
        console.log("ID:", productId);

        return this.http.get<Product>(`${this.apiUrl}/${productId}`).pipe(
            tap({
                next: product => console.log("Response:", product),
                error: err => console.error("HTTP Error:", err)
            })
        );
    }

    getProductBySlug(slug: string): Observable<Product> {
        console.log("slug:", slug);

        return this.http.get<Product>(`${this.apiUrl}/details/${slug}`).pipe(
            tap({
                next: product => console.log("Response:", product),
                error: err => console.error("HTTP Error:", err)
            })
        );
    }
}
