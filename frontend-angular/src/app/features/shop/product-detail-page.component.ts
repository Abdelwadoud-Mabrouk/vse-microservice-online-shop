import { Component, Input } from '@angular/core';
import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { ShopApiService } from '../../core/api/shop-api.service';
import { CartService } from './cart.service';
import { Product } from '../../core/models/product.model';
import { furnitureCategoryLabel, furnitureImageStyle } from '../../shared/product-presentation';

@Component({
  selector: 'app-product-detail-page',
  standalone: true,
  imports: [AsyncPipe, CurrencyPipe],
  template: `
    @if (product$ | async; as product) {
      <section class="furniture-detail">
        <div class="detail-gallery">
          <div class="detail-photo" [style.background-image]="imageStyle(product)">
            <span>{{ categoryLabel(product) }}</span>
          </div>
        </div>

        <div class="detail-panel">
          <span class="eyebrow">{{ product.sku }}</span>
          <h1>{{ product.name }}</h1>
          <p>{{ product.description ?? 'A selected furniture piece for a modern living space.' }}</p>

          <div class="detail-price-row">
            <div>
              <span class="metric-label">Price</span>
              <strong class="price">{{ product.price | currency: product.currency ?? 'EUR' }}</strong>
            </div>
            <span class="status-chip success">Available</span>
          </div>

          <div class="detail-features">
            <span>Warehouse stock is checked during checkout</span>
            <span>Invoice is created after order completion</span>
            <span>Audit timeline keeps the process traceable</span>
          </div>

          <button type="button" class="primary-button detail-buy-button" (click)="add(product)">Add to cart</button>
        </div>
      </section>
    }
  `
})
export class ProductDetailPageComponent {
  @Input() id = '';
  get product$() {
    return this.shopApi.getProduct(this.id);
  }

  constructor(
    private readonly shopApi: ShopApiService,
    private readonly cart: CartService
  ) {}

  add(product: Product): void {
    this.cart.add(product);
  }

  categoryLabel(product: Product): string {
    return furnitureCategoryLabel(product);
  }

  imageStyle(product: Product): string {
    return furnitureImageStyle(product);
  }
}
