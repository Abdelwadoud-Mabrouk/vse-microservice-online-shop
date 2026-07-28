import { Component, signal, ChangeDetectionStrategy } from '@angular/core';
import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ShopApiService } from '../../core/api/shop-api.service';
import { CartService } from './cart.service';
import { Product } from '../../core/models/product.model';
import {
  FurnitureCategory,
  furnitureCategories,
  furnitureCategory,
  furnitureCategoryLabel,
  furnitureImageStyle
} from '../../shared/product-presentation';

@Component({
    selector: 'app-product-list-page',
    imports: [AsyncPipe, CurrencyPipe, RouterLink],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
    <section class="shop-hero">
      <div class="shop-hero-copy">
        <span class="eyebrow">Furniture collection 2026</span>
        <h1>Modern furniture for calm, beautiful living spaces.</h1>
        <p>
          Discover sofas, chairs, tables, storage and lighting with a checkout flow backed by shop, warehouse, billing, invoice and audit services.
        </p>
        <div class="hero-actions">
          <a href="#products" class="primary-button">Explore collection</a>
          <a routerLink="/shop/cart" class="ghost-button">View cart</a>
        </div>
      </div>
      <div class="hero-product" aria-hidden="true">
        <div class="hero-product-image furniture-hero-image"></div>
        <div class="hero-product-card">
          <span>Curated cart</span>
          <strong>{{ cart.count() }} items</strong>
          <small>Ready for checkout</small>
        </div>
      </div>
    </section>

    <section class="shop-benefits" aria-label="Shop benefits">
      <article>
        <strong>Curated furniture</strong>
        <span>Clear categories and realistic product presentation.</span>
      </article>
      <article>
        <strong>Reliable stock</strong>
        <span>Warehouse reserves availability before payment.</span>
      </article>
      <article>
        <strong>Order transparency</strong>
        <span>Audit timeline and invoice are available after checkout.</span>
      </article>
    </section>

    <section class="shop-section-head" id="products">
      <div>
        <span class="eyebrow">Catalog</span>
        <h2>Furniture for every room</h2>
      </div>
      <div class="category-tabs">
        @for (category of categories; track category.value) {
          <button
            type="button"
            class="ghost-button"
            [class.active]="selectedCategory() === category.value"
            (click)="selectedCategory.set(category.value)"
          >
            {{ category.label }}
          </button>
        }
      </div>
    </section>

    <section class="product-grid">
      @for (product of products$ | async; track product.id) {
        @if (isVisible(product)) {
          <article class="product-card shop-card">
            <a
              class="product-media furniture-photo"
              [style.background-image]="imageStyle(product)"
              [routerLink]="['/shop/products', product.id]"
              [attr.aria-label]="'View ' + product.name"
            >
              <span>{{ categoryLabel(product) }}</span>
            </a>
            <div class="product-card-body">
              <div class="product-meta">
                <span>{{ categoryLabel(product) }}</span>
                <span>In stock</span>
              </div>
              <h3>{{ product.name }}</h3>
              <p>{{ product.description || 'A selected furniture item prepared for a clean checkout demo.' }}</p>
            </div>
            <div class="card-actions product-actions">
              <div>
                <span class="price-label">Now</span>
                <strong>{{ product.price | currency: product.categoryName ?? 'EUR' }}</strong>
              </div>
              <div class="button-row">
                <a [routerLink]="['/shop/products', product.id]" class="ghost-button">View</a>
                <button type="button" (click)="addToCart(product)">Add</button>
              </div>
            </div>
          </article>
        }
      } @empty {
        <div class="panel empty-state">
          <strong>No products available.</strong>
          <span>Start the shop-service seed data or create products in Admin.</span>
        </div>
      }
    </section>

    <section class="shop-cta">
      <div>
        <span class="eyebrow">Checkout journey</span>
        <h2>From living room inspiration to traceable order flow.</h2>
        <p>The frontend stays elegant while the backend handles order, reservation, payment, stock commit, invoice and audit.</p>
      </div>
      <a routerLink="/shop/cart" class="primary-button">Continue to cart</a>
    </section>
  `
})
export class ProductListPageComponent {
  readonly products$ = this.shopApi.getProducts();
  readonly selectedCategory = signal<FurnitureCategory>('all');
  readonly categories = furnitureCategories;

  constructor(
    private readonly shopApi: ShopApiService,
    readonly cart: CartService
  ) {}

  addToCart(product: Product): void {
    this.cart.add(product);
  }

  isVisible(product: Product): boolean {
    const selected = this.selectedCategory();
    return selected === 'all' || this.category(product) === selected;
  }

  categoryLabel(product: Product): string {
    return furnitureCategoryLabel(product);
  }

  productInitials(product: Product): string {
    return product.name
      .split(' ')
      .map((part) => part[0])
      .join('')
      .slice(0, 2)
      .toUpperCase();
  }

  category(product: Product): FurnitureCategory {
    return furnitureCategory(product);
  }

  imageStyle(product: Product): string {
    return furnitureImageStyle(product);
  }
}
