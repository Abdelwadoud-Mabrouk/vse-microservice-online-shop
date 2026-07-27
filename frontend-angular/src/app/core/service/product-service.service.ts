import { Injectable, inject } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {catchError, Observable, of, tap} from "rxjs";
import {Product} from "../models/product.model";

@Injectable({
  providedIn: 'root'
})
export class ProductServiceService {

  private http = inject(HttpClient);

  private apiUrl = 'http://localhost:8080/api/products';

  getApiUrl(){
    return 'http://localhost:8080/'
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
