import { Component } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { PageHeaderComponent } from '../../shared/components/page-header.component';

@Component({
  selector: 'app-customer-profile-page',
  standalone: true,
  imports: [PageHeaderComponent],
  template: `
    <app-page-header
      eyebrow="Customer Account"
      title="Profile"
      description="Customer data should be persisted by the shop-service. This page shows the frontend contract."
    />

    <section class="panel">
      <div class="form-grid">
        <label>Name <input type="text" [value]="auth.session()?.displayName ?? ''" /></label>
        <label>Email <input type="email" [value]="auth.session()?.email ?? ''" /></label>
        <label>Street <input type="text" placeholder="Musterstrasse 12" /></label>
        <label>City <input type="text" placeholder="Luedenscheid" /></label>
      </div>
      <button type="button" class="primary-button">Save profile</button>
    </section>
  `
})
export class CustomerProfilePageComponent {
  constructor(readonly auth: AuthService) {}
}
