import { Component, ChangeDetectionStrategy } from '@angular/core';
import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ShopApiService } from '../../core/api/shop-api.service';
import { PageHeaderComponent } from '../../shared/components/page-header.component';

@Component({
    selector: 'app-product-management-page',
    imports: [AsyncPipe, CurrencyPipe, RouterLink, PageHeaderComponent],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
    <app-page-header
      eyebrow="Admin"
      title="Product Management"
      description="Product data and prices are persisted by the shop-service."
    >
      <a routerLink="/admin/products/new" class="primary-button">New product</a>
    </app-page-header>

    <section class="panel">
      <div class="table table-head">
        <span>SKU</span><span>Name</span><span>Price</span><span>Action</span>
      </div>
      @for (product of products$ | async; track product.id) {
        <div class="table table-row">
          <span>{{ product.sku }}</span>
          <span>{{ product.name }}</span>
          <span>{{ product.price | currency: 'EUR' ?? 'EUR' }}</span>
          <a [routerLink]="['/admin/products', product.id, 'edit']">Edit</a>
        </div>
      }
    </section>
  `
})
export class ProductManagementPageComponent {
  readonly products$ = this.shopApi.getProducts();

  constructor(private readonly shopApi: ShopApiService) {}
}
