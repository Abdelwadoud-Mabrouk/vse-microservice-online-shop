import { AsyncPipe } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuditApiService } from '../../core/api/audit-api.service';
import { AuditEvent, AuditOrderSummary } from '../../core/models/audit-event.model';
import { PageHeaderComponent } from '../../shared/components/page-header.component';
import { AuditTimelineComponent } from '../../shared/components/audit-timeline.component';

@Component({
  selector: 'app-audit-search-page',
  standalone: true,
  imports: [AsyncPipe, FormsModule, PageHeaderComponent, AuditTimelineComponent],
  template: `
    <app-page-header
      eyebrow="Admin"
      title="Audit timeline"
      description="Select an order correlationId or search manually. Audit data stays read-only and owned by the audit-service."
    />

    <section class="audit-workspace">
      <aside class="panel audit-order-list">
        <div class="detail-head">
          <div>
            <span class="eyebrow">Orders</span>
            <h2>Correlation IDs</h2>
          </div>
        </div>

        <div class="search-row">
          <input [(ngModel)]="filter" placeholder="Filter correlationId" />
        </div>

        <div class="compact-list">
          @for (order of filteredOrders(orders$ | async); track order.correlationId) {
            <button
              type="button"
              class="audit-order-button"
              [class.active]="order.correlationId === correlationId"
              (click)="load(order.correlationId)"
            >
              <span>
                <strong>{{ order.lastEventType }}</strong>
                <small>{{ order.correlationId }}</small>
              </span>
              <span class="status-chip" [class.success]="order.lastStatusCode === 'SUCCESS'" [class.failure]="order.lastStatusCode !== 'SUCCESS'">
                {{ order.lastStatusCode }}
              </span>
            </button>
          } @empty {
            <div class="empty-state">No audit orders found.</div>
          }
        </div>
      </aside>

      <div class="panel">
        <div class="detail-head">
          <div>
            <span class="eyebrow">Timeline</span>
            <h2>{{ correlationId || 'Select an order' }}</h2>
          </div>
        </div>

        <div class="search-row">
          <input [(ngModel)]="correlationId" placeholder="Correlation ID" />
          <button type="button" class="primary-button" (click)="search()">Load timeline</button>
        </div>

        <app-audit-timeline [events]="events()" />
      </div>
    </section>
  `
})
export class AuditSearchPageComponent {
  correlationId = '';
  filter = '';
  readonly orders$ = this.auditApi.getOrderSummaries();
  readonly events = signal<AuditEvent[]>([]);

  constructor(private readonly auditApi: AuditApiService) {}

  search(): void {
    const correlationId = this.correlationId.trim();
    if (!correlationId) {
      this.events.set([]);
      return;
    }

    this.load(correlationId);
  }

  load(correlationId: string): void {
    this.correlationId = correlationId;
    this.auditApi.getTimeline(correlationId).subscribe((events) => this.events.set(events));
  }

  filteredOrders(orders: AuditOrderSummary[] | null): AuditOrderSummary[] {
    const search = this.filter.trim().toLowerCase();
    return (orders ?? []).filter((order) => !search || order.correlationId.toLowerCase().includes(search));
  }
}
