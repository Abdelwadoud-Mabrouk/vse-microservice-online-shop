import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PageHeaderComponent } from '../../shared/components/page-header.component';

@Component({
    selector: 'app-warehouse-dashboard-page',
    imports: [RouterLink, PageHeaderComponent],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
    <app-page-header
      eyebrow="Warehouse"
      title="Stock Operations"
      description="Warehouse users manage stock and reservation state. Product prices remain in the shop-service."
    />

    <section class="dashboard-grid">
      <a routerLink="/warehouse/stock" class="metric-card"><span>Stock</span><strong>Inventory quantities</strong></a>
      <a routerLink="/warehouse/reservations" class="metric-card"><span>Reservations</span><strong>Reserved, committed, cancelled</strong></a>
    </section>
  `
})
export class WarehouseDashboardPageComponent {}
