import { Injectable, inject } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable, tap} from "rxjs";
import {Product} from "../models/product.model";

@Injectable({
  providedIn: 'root'
})
export class ProductServiceService {

  private http = inject(HttpClient);

  private readonly apiUrl = '/api/shop/api/v1/products';

  getApiUrl(){
    return '/api/shop';
  }

  resolveImageUrl(imageUrl?: string): string {
    if (!imageUrl) {
      return 'assets/images/product-placeholder.jpg';
    }

    if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
      return imageUrl;
    }

    return `${this.getApiUrl()}${imageUrl.startsWith('/') ? '' : '/'}${imageUrl}`;
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
        { params }
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
        { params }
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
