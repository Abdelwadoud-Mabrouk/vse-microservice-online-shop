import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { ShopApiService } from '../../core/api/shop-api.service';
import { PageHeaderComponent } from '../../shared/components/page-header.component';

@Component({
    selector: 'app-product-edit-page',
    imports: [AsyncPipe, PageHeaderComponent],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
    <app-page-header
      eyebrow="Admin"
      title="Edit Product"
      description="Price updates must be persisted in the shop-service, never only in the frontend."
    />

    <section class="panel">
      @if (product$ | async; as product) {
        <div class="form-grid">
          <label>SKU <input type="text" [value]="product.sku" /></label>
          <label>Name <input type="text" [value]="product.name" /></label>
          <label>Price <input type="number" [value]="product.price" /></label>
          <label>Currency <input type="text" [value]="product.id ?? 'EUR'" /></label>
        </div>
      } @else {
        <div class="form-grid">
          <label>SKU <input type="text" placeholder="SKU-NEW" /></label>
          <label>Name <input type="text" placeholder="New product" /></label>
          <label>Price <input type="number" placeholder="0.00" /></label>
          <label>Currency <input type="text" value="EUR" /></label>
        </div>
      }
      <button type="button" class="primary-button">Save product</button>
    </section>
  `
})
export class ProductEditPageComponent {
  @Input() id = '';
  get product$() {
    return this.shopApi.getProduct(this.id);
  }

  constructor(private readonly shopApi: ShopApiService) {}
}
