import { Component, ChangeDetectionStrategy } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { ShopApiService } from '../../core/api/shop-api.service';
import { PageHeaderComponent } from '../../shared/components/page-header.component';

@Component({
    selector: 'app-customer-management-page',
    imports: [AsyncPipe, PageHeaderComponent],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
    <app-page-header
      eyebrow="Admin"
      title="Customer Management"
      description="Customer master data should be handled by the shop-service."
    />

    <section class="panel">
      @for (customer of customers$ | async; track customer.customerId) {
        <div class="table-row">
          <span>{{ customer.displayName }}</span>
          <span>{{ customer.email }}</span>
        </div>
      } @empty {
        <div class="empty-state">Customer endpoints are planned for the next backend step.</div>
      }
    </section>
  `
})
export class CustomerManagementPageComponent {
  readonly customers$ = this.shopApi.getCustomers();

  constructor(private readonly shopApi: ShopApiService) {}
}
