import { Component } from '@angular/core';
import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { InvoiceApiService } from '../../core/api/invoice-api.service';
import { PageHeaderComponent } from '../../shared/components/page-header.component';

@Component({
  selector: 'app-invoice-overview-page',
  standalone: true,
  imports: [AsyncPipe, CurrencyPipe, DatePipe, PageHeaderComponent],
  template: `
    <app-page-header
      eyebrow="Admin"
      title="Invoices"
      description="Invoice documents belong to the invoice-service. The frontend only links to downloads."
    />

    <section class="panel">
      @for (invoice of invoices$ | async; track invoice.invoiceId) {
        <div class="table-row">
          <span>{{ invoice.customerEmail }}</span>
          <span>{{ invoice.amount | currency: invoice.currency }}</span>
          <span>{{ invoice.createdAt | date: 'short' }}</span>
          <a [href]="invoiceApi.getPdfUrl(invoice.invoiceId)" target="_blank" rel="noreferrer">PDF</a>
        </div>
      } @empty {
        <div class="empty-state">No invoices found.</div>
      }
    </section>
  `
})
export class InvoiceOverviewPageComponent {
  readonly invoices$ = this.invoiceApi.getInvoices();

  constructor(readonly invoiceApi: InvoiceApiService) {}
}
