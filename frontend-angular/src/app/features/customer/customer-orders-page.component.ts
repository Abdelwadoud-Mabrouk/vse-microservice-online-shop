import { Component, ChangeDetectionStrategy } from '@angular/core';
import { PageHeaderComponent } from '../../shared/components/page-header.component';

@Component({
    selector: 'app-customer-orders-page',
    imports: [PageHeaderComponent],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
    <app-page-header
      eyebrow="Customer Account"
      title="Own Orders"
      description="This view should query the shop-service by customerId."
    />
    <section class="panel">
      <div class="empty-state">Customer order history endpoint is planned in the shop-service.</div>
    </section>
  `
})
export class CustomerOrdersPageComponent {}
