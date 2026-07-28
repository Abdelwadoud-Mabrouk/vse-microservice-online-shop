import { Component, ChangeDetectionStrategy } from '@angular/core';
import { AsyncPipe, DatePipe } from '@angular/common';
import { WarehouseApiService } from '../../core/api/warehouse-api.service';
import { PageHeaderComponent } from '../../shared/components/page-header.component';
import { StatusChipComponent } from '../../shared/components/status-chip.component';

@Component({
    selector: 'app-reservation-overview-page',
    imports: [AsyncPipe, DatePipe, PageHeaderComponent, StatusChipComponent],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
    <app-page-header
      eyebrow="Warehouse"
      title="Reservations"
      description="Reservation state is owned by the warehouse-service and visible to warehouse users."
    />

    <section class="panel">
      @for (reservation of reservations$ | async; track reservation.reservationId) {
        <div class="reservation-card">
          <div>
            <strong>{{ reservation.reservationId }}</strong>
            <small>Order {{ reservation.orderId }} | {{ reservation.createdAt | date: 'short' }}</small>
          </div>
          <app-status-chip [label]="reservation.status" />
          <div class="button-row">
            <button type="button" class="ghost-button">Cancel</button>
            <button type="button" class="primary-button">Commit</button>
          </div>
        </div>
      } @empty {
        <div class="empty-state">Reservation list endpoint is not available yet. Commit/cancel endpoints already exist.</div>
      }
    </section>
  `
})
export class ReservationOverviewPageComponent {
  readonly reservations$ = this.warehouseApi.getReservations();

  constructor(private readonly warehouseApi: WarehouseApiService) {}
}
