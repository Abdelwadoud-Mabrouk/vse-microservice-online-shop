import { Component, ChangeDetectionStrategy } from '@angular/core';
import { PageHeaderComponent } from '../../shared/components/page-header.component';

@Component({
    selector: 'app-order-overview-page',
    imports: [PageHeaderComponent],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
    <app-page-header
      eyebrow="Admin"
      title="Order Overview"
      description="Order data belongs to the shop-service. This page should list orders and their correlation ids."
    />
    <section class="panel">
      <div class="empty-state">Admin order list endpoint is planned in the shop-service.</div>
    </section>
  `
})
export class OrderOverviewPageComponent {}
