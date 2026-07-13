import { Component } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { WarehouseApiService } from '../../core/api/warehouse-api.service';
import { PageHeaderComponent } from '../../shared/components/page-header.component';

@Component({
  selector: 'app-stock-overview-page',
  standalone: true,
  imports: [AsyncPipe, RouterLink, PageHeaderComponent],
  template: `
    <app-page-header
      eyebrow="Warehouse"
      title="Stock Overview"
      description="Stock quantities are owned by the warehouse-service."
    />

    <section class="panel">
      <div class="table table-head">
        <span>SKU</span><span>Name</span><span>Available</span><span>Reserved</span><span>Action</span>
      </div>
      @for (item of stock$ | async; track item.productId) {
        <div class="table table-row">
          <span>{{ item.sku }}</span>
          <span>{{ item.name }}</span>
          <strong>{{ item.availableQuantity }}</strong>
          <span>{{ item.reservedQuantity ?? 0 }}</span>
          <a [routerLink]="['/warehouse/stock', item.productId, 'edit']">Edit stock</a>
        </div>
      } @empty {
        <div class="empty-state">No stock data returned by the warehouse-service.</div>
      }
    </section>
  `
})
export class StockOverviewPageComponent {
  readonly stock$ = this.warehouseApi.getStock();

  constructor(private readonly warehouseApi: WarehouseApiService) {}
}
