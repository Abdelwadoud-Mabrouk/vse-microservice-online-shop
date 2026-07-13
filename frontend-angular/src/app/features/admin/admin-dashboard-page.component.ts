import { Component } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PageHeaderComponent } from '../../shared/components/page-header.component';
import { AdminApiService } from '../../core/api/admin-api.service';
import { AuditApiService } from '../../core/api/audit-api.service';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [AsyncPipe, RouterLink, PageHeaderComponent],
  template: `
    <app-page-header
      eyebrow="Admin"
      title="Operations center"
      description="The visible admin UI lives here in Angular. Backend services only provide APIs."
    />

    <section class="dashboard-grid admin-metrics">
      <article class="metric-card">
        <span>Audit orders</span>
        <strong>{{ (orders$ | async)?.length ?? 0 }}</strong>
      </article>
      <article class="metric-card">
        <span>System health</span>
        <strong>{{ healthyCount(health$ | async) }}</strong>
      </article>
      <a routerLink="/observability" class="metric-card">
        <span>Grafana</span>
        <strong>Monitoring</strong>
      </a>
    </section>

    <section class="admin-action-grid">
      <a routerLink="/admin/audit" class="admin-action-card">
        <span class="eyebrow">Timeline</span>
        <strong>Audit search</strong>
        <small>Find a correlationId and inspect the complete event sequence.</small>
      </a>
      <a routerLink="/admin/system" class="admin-action-card">
        <span class="eyebrow">System</span>
        <strong>Services & databases</strong>
        <small>Check service and database health in one organized page.</small>
      </a>
      <a routerLink="/admin/orders" class="admin-action-card">
        <span class="eyebrow">Orders</span>
        <strong>Order overview</strong>
        <small>Review order lifecycle and customer-facing status.</small>
      </a>
      <a routerLink="/admin/invoices" class="admin-action-card">
        <span class="eyebrow">Invoices</span>
        <strong>Invoice documents</strong>
        <small>Search invoices and open generated PDFs.</small>
      </a>
    </section>
  `
})
export class AdminDashboardPageComponent {
  readonly health$ = this.adminApi.getServiceHealth();
  readonly orders$ = this.auditApi.getOrderSummaries();

  constructor(
    private readonly adminApi: AdminApiService,
    private readonly auditApi: AuditApiService
  ) {}

  healthyCount(services: { status: string }[] | null | undefined): string {
    if (!services?.length) {
      return '0 / 0';
    }

    return `${services.filter((service) => service.status === 'healthy').length} / ${services.length}`;
  }
}
