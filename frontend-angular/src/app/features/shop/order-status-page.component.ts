import { Component, Input } from '@angular/core';
import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { ShopApiService } from '../../core/api/shop-api.service';
import { PageHeaderComponent } from '../../shared/components/page-header.component';
import { StatusChipComponent } from '../../shared/components/status-chip.component';

@Component({
  selector: 'app-order-status-page',
  standalone: true,
  imports: [AsyncPipe, CurrencyPipe, PageHeaderComponent, StatusChipComponent],
  template: `
    <app-page-header eyebrow="Order" title="Order Status" description="The order state is owned by the shop-service." />

    @if (order$ | async; as order) {
      <section class="panel">
        <div class="detail-head">
          <div>
            <span class="eyebrow">Correlation</span>
            <h3>{{ order.correlationId }}</h3>
          </div>
          <app-status-chip [label]="order.status" />
        </div>
        <div class="table">
          @for (item of order.items; track item.productId) {
            <div class="table-row">
              <span>{{ item.productName ?? item.sku }}</span>
              <span>{{ item.quantity }}</span>
              <span>{{ item.totalPrice ?? 0 | currency: 'EUR' }}</span>
            </div>
          }
        </div>
      </section>
    }
  `
})
export class OrderStatusPageComponent {
  @Input() id = '';
  get order$() {
    return this.shopApi.getOrder(this.id);
  }

  constructor(private readonly shopApi: ShopApiService) {}
}
