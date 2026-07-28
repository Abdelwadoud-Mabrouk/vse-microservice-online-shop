import { Component, ChangeDetectionStrategy } from '@angular/core';
import { PageHeaderComponent } from '../../shared/components/page-header.component';

@Component({
    selector: 'app-register-page',
    imports: [PageHeaderComponent],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
    <app-page-header
      eyebrow="Customer"
      title="Register Customer"
      description="This page is reserved for the clean customer management flow in the shop-service."
    />

    <section class="panel">
      <div class="form-grid">
        <label>First name <input type="text" placeholder="Max" /></label>
        <label>Last name <input type="text" placeholder="Mustermann" /></label>
        <label>Email <input type="email" placeholder="customer@example.com" /></label>
        <label>Phone <input type="text" placeholder="+49 ..." /></label>
      </div>
      <button type="button" class="primary-button">Create customer</button>
    </section>
  `
})
export class RegisterPageComponent {}
