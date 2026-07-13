import { Component } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { AdminApiService, ServiceHealth } from '../../core/api/admin-api.service';
import { PageHeaderComponent } from '../../shared/components/page-header.component';

@Component({
  selector: 'app-system-status-page',
  standalone: true,
  imports: [AsyncPipe, PageHeaderComponent],
  template: `
    <app-page-header
      eyebrow="Admin"
      title="Services & databases"
      description="A clean operational overview for backend services and database connectivity."
    />

    @if (health$ | async; as health) {
      <section class="system-grid">
        <article class="panel">
          <span class="eyebrow">Services</span>
          <h2>Microservices</h2>
          <div class="health-list-ui">
            @for (service of services(health); track service.service) {
              <div class="health-row">
                <span>{{ service.service }}</span>
                <span class="status-chip" [class.success]="service.status === 'healthy'" [class.failure]="service.status !== 'healthy'">
                  {{ service.status }}
                </span>
              </div>
            }
          </div>
        </article>

        <article class="panel">
          <span class="eyebrow">Databases</span>
          <h2>Persistence</h2>
          <div class="health-list-ui">
            @for (database of databases(health); track database.service) {
              <div class="health-row">
                <span>{{ database.service }}</span>
                <span class="status-chip" [class.success]="database.status === 'healthy'" [class.failure]="database.status !== 'healthy'">
                  {{ database.status }}
                </span>
              </div>
            }
          </div>
        </article>
      </section>
    }
  `
})
export class SystemStatusPageComponent {
  readonly health$ = this.adminApi.getServiceHealth();

  constructor(private readonly adminApi: AdminApiService) {}

  services(health: ServiceHealth[]): ServiceHealth[] {
    return health.filter((item) => !item.service.endsWith('-db'));
  }

  databases(health: ServiceHealth[]): ServiceHealth[] {
    return health.filter((item) => item.service.endsWith('-db'));
  }
}
