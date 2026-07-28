import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe, JsonPipe } from '@angular/common';
import { AuditEvent } from '../../core/models/audit-event.model';
import { StatusChipComponent } from './status-chip.component';

@Component({
    selector: 'app-audit-timeline',
    imports: [DatePipe, JsonPipe, StatusChipComponent],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
    <div class="timeline">
      @for (event of events; track event.id) {
        <article class="timeline-item">
          <div class="timeline-marker"></div>
          <div class="timeline-card">
            <div class="timeline-head">
              <div>
                <strong>{{ event.eventType }}</strong>
                <span>{{ event.service }}</span>
              </div>
              <app-status-chip [label]="event.statusCode" />
            </div>
            <small>{{ event.timestamp | date: 'medium' }}</small>
            <pre>{{ event.payload | json }}</pre>
          </div>
        </article>
      } @empty {
        <div class="empty-state">No audit events found for this correlation id.</div>
      }
    </div>
  `
})
export class AuditTimelineComponent {
  @Input() events: AuditEvent[] = [];
}
