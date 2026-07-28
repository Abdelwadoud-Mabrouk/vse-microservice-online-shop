import { Component, ChangeDetectionStrategy } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { AdminApiService } from '../../core/api/admin-api.service';
import { PageHeaderComponent } from '../../shared/components/page-header.component';

@Component({
    selector: 'app-observability-page',
    imports: [AsyncPipe, PageHeaderComponent],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
    <app-page-header
      eyebrow="Observability"
      title="Monitoring & diagnostics"
      description="Technical dashboards stay separated from the shop journey. Use this page for demo analysis and debugging."
    >
      <a class="primary-button" href="http://localhost:3000" target="_blank" rel="noreferrer">Open Grafana</a>
    </app-page-header>

    <section class="observability-grid">
      <article class="panel observability-card">
        <span class="eyebrow">Grafana</span>
        <h2>Logs and dashboards</h2>
        <p>Open Grafana in a separate tab to inspect logs, traces and service behavior without cluttering business screens.</p>
        <a class="ghost-button" href="http://localhost:3000" target="_blank" rel="noreferrer">Launch dashboard</a>
      </article>

      <article class="panel observability-card">
        <span class="eyebrow">Health</span>
        <h2>Service status</h2>
        <div class="health-list-ui">
          @for (service of health$ | async; track service.service) {
            <div class="health-row">
              <span>{{ service.service }}</span>
              <span class="status-chip" [class.success]="service.status === 'healthy'" [class.failure]="service.status !== 'healthy'">
                {{ service.status }}
              </span>
            </div>
          } @empty {
            <div class="empty-state">Service health is not available.</div>
          }
        </div>
      </article>

      <article class="panel observability-card wide">
        <span class="eyebrow">Recommended demo flow</span>
        <h2>How to use this during presentation</h2>
        <div class="debug-flow">
          <span>1. Create order in Shop</span>
          <span>2. Watch Audit timeline</span>
          <span>3. Open Invoice PDF</span>
          <span>4. Inspect logs in Grafana</span>
        </div>
      </article>
    </section>
  `
})
export class ObservabilityPageComponent {
  readonly health$ = this.adminApi.getServiceHealth();

  constructor(private readonly adminApi: AdminApiService) {}
}
