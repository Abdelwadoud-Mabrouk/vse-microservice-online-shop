import { Component, ChangeDetectionStrategy } from '@angular/core';
import { PageHeaderComponent } from '../../shared/components/page-header.component';

@Component({
    selector: 'app-customer-addresses-page',
    imports: [PageHeaderComponent],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
    <app-page-header
      eyebrow="Customer Account"
      title="Addresses"
      description="Address data belongs to customer management in the shop-service."
    />
    <section class="panel">
      <div class="form-grid">
        <label>Shipping street <input type="text" /></label>
        <label>Postal code <input type="text" /></label>
        <label>City <input type="text" /></label>
        <label>Country <input type="text" /></label>
      </div>
    </section>
  `
})
export class CustomerAddressesPageComponent {}
