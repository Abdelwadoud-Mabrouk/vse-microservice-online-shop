import { Component } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  template: `
    <main class="login-page">
      <section class="login-panel">
        <span class="eyebrow">WohnWerk access</span>
        <h1>Choose your workspace</h1>
        <p>One Angular frontend for furniture shopping, operations and warehouse workflows.</p>

        <div class="login-grid">
          <button type="button" (click)="login('CUSTOMER')">
            <strong>Customer shop</strong>
            <span>Browse furniture, cart, checkout and own orders</span>
          </button>
          <button type="button" (click)="login('ADMIN')">
            <strong>Operations admin</strong>
            <span>Products, prices, orders, audit, invoices and monitoring</span>
          </button>
          <button type="button" (click)="login('WAREHOUSE')">
            <strong>Warehouse</strong>
            <span>Stock, reservations, commit/cancel and monitoring</span>
          </button>
        </div>
      </section>
    </main>
  `
})
export class LoginPageComponent {
  constructor(private readonly auth: AuthService) {}

  login(role: 'CUSTOMER' | 'ADMIN' | 'WAREHOUSE'): void {
    this.auth.loginAs(role);
  }
}
