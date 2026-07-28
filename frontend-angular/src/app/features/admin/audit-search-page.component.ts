import { AsyncPipe } from '@angular/common';
import { Component, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuditApiService } from '../../core/api/audit-api.service';
import { InvoiceApiService } from '../../core/api/invoice-api.service';
import { AuditEvent, AuditOrderSummary } from '../../core/models/audit-event.model';
import { Invoice } from '../../core/models/invoice.model';
import { PageHeaderComponent } from '../../shared/components/page-header.component';
import { AuditTimelineComponent } from '../../shared/components/audit-timeline.component';

@Component({
    selector: 'app-audit-search-page',
    imports: [AsyncPipe, FormsModule, PageHeaderComponent, AuditTimelineComponent],
    changeDetection: ChangeDetectionStrategy.Eager,
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

        @if (invoice(); as availableInvoice) {
          <div class="alert success">
            <span>Rechnung für diese Bestellung ist verfügbar.</span>
            <a
              class="primary-button"
              [href]="invoiceApi.getPdfUrl(availableInvoice.invoiceId)"
              [attr.download]="'rechnung-' + availableInvoice.invoiceId + '.pdf'"
            >
              Rechnung herunterladen
            </a>
          </div>
        } @else if (invoiceLoading()) {
          <div class="alert">Rechnung wird gesucht …</div>
        } @else if (correlationId && events().length > 0) {
          <div class="alert">Für diese Timeline ist noch keine Rechnung verfügbar.</div>
        }

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
  readonly invoice = signal<Invoice | null>(null);
  readonly invoiceLoading = signal(false);

  constructor(
    private readonly auditApi: AuditApiService,
    readonly invoiceApi: InvoiceApiService
  ) {}

  search(): void {
    const correlationId = this.correlationId.trim();
    if (!correlationId) {
      this.events.set([]);
      this.invoice.set(null);
      return;
    }

    this.load(correlationId);
  }

  load(correlationId: string): void {
    this.correlationId = correlationId;
    this.invoice.set(null);
    this.invoiceLoading.set(true);
    this.auditApi.getTimeline(correlationId).subscribe((events) => {
      this.events.set(events);
      this.loadInvoice(events);
    });
  }

  private loadInvoice(events: AuditEvent[]): void {
    const orderIds = new Set(
      [
        this.findPayloadValue(events, ['orderId']),
        ...events.map((event) => event.correlationId)
      ].filter((orderId): orderId is string => Boolean(orderId))
    );
    const eventEmail = this.findPayloadValue(events, ['customerEmail', 'email']);
    if (orderIds.size === 0) {
      this.invoiceLoading.set(false);
      return;
    }

    const emails = [...new Set([eventEmail, 'mock@email.com'].filter((email): email is string => Boolean(email)))];
    forkJoin(emails.map((email) => this.invoiceApi.getInvoices(email))).subscribe((invoiceGroups) => {
      const invoice = invoiceGroups.flat().find((candidate) => orderIds.has(candidate.orderId)) ?? null;
      this.invoice.set(invoice);
      this.invoiceLoading.set(false);
    });
  }

  private findPayloadValue(events: AuditEvent[], keys: string[]): string | undefined {
    for (const event of events) {
      const value = this.findValue(event.payload, new Set(keys));
      if (value) {
        return value;
      }
    }
    return undefined;
  }

  private findValue(value: unknown, keys: Set<string>): string | undefined {
    if (!value || typeof value !== 'object') {
      return undefined;
    }

    for (const [key, nestedValue] of Object.entries(value)) {
      if (keys.has(key) && typeof nestedValue === 'string' && nestedValue.trim()) {
        return nestedValue;
      }
      const result = this.findValue(nestedValue, keys);
      if (result) {
        return result;
      }
    }
    return undefined;
  }

  filteredOrders(orders: AuditOrderSummary[] | null): AuditOrderSummary[] {
    const search = this.filter.trim().toLowerCase();
    return (orders ?? []).filter((order) => !search || order.correlationId.toLowerCase().includes(search));
  }
}
