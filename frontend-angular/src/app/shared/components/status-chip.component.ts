import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-status-chip',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  template: '<span [class]="statusClass">{{ label }}</span>'
})
export class StatusChipComponent {
  @Input({ required: true }) label = '';

  get statusClass(): string {
    const normalized = this.label.toLowerCase();
    if (normalized.includes('success') || normalized.includes('created') || normalized.includes('committed')) {
      return 'status-chip success';
    }
    if (normalized.includes('fail') || normalized.includes('cancelled') || normalized.includes('rejected')) {
      return 'status-chip failure';
    }
    if (normalized.includes('reserved') || normalized.includes('processing')) {
      return 'status-chip warning';
    }
    return 'status-chip neutral';
  }
}
