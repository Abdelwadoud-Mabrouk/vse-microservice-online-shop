import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  standalone: true,
  template: `
    <section class="page-header">
      <div>
        <span class="eyebrow">{{ eyebrow }}</span>
        <h2>{{ title }}</h2>
        <p>{{ description }}</p>
      </div>
      <ng-content />
    </section>
  `
})
export class PageHeaderComponent {
  @Input() eyebrow = '';
  @Input({ required: true }) title = '';
  @Input() description = '';
}
