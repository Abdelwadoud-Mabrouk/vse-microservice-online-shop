import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [FormsModule],
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
          <div class="admin-login-card">
            <strong>Operations admin</strong>
            <span>Use the credentials configured in the environment.</span>
            <label>
              Username
              <input name="adminUsername" [(ngModel)]="adminUsername" autocomplete="username" />
            </label>
            <label>
              Password
              <input name="adminPassword" [(ngModel)]="adminPassword" type="password" autocomplete="current-password" />
            </label>
            <button type="button" (click)="login('ADMIN')">Open admin workspace</button>
            @if (adminLoginError) {
              <span class="form-error">Enter the configured admin username and password.</span>
            }
          </div>
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
  adminUsername = '';
  adminPassword = '';
  adminLoginError = false;

  constructor(private readonly auth: AuthService) {}

  login(role: 'CUSTOMER' | 'ADMIN' | 'WAREHOUSE'): void {
    this.adminLoginError = !this.auth.loginAs(role, this.adminUsername, this.adminPassword);
  }
}
