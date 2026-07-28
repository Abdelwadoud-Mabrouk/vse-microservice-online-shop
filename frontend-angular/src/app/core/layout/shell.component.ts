import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { CartService } from '../../features/shop/cart.service';

@Component({
    selector: 'app-shell',
    imports: [RouterOutlet, RouterLink, RouterLinkActive],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
    <div class="shell">
      <header class="storefront-nav">
        <a class="brand" routerLink="/shop/products">
          <span class="brand-mark">WW</span>
          <span>
            <strong>WohnWerk</strong>
            <small>Furniture shop</small>
          </span>
        </a>

        <nav>
          <a routerLink="/shop/products" routerLinkActive="active">Shop</a>
          <a routerLink="/account" routerLinkActive="active">Customer</a>
          @if (role() === 'ADMIN') {
            <a routerLink="/admin" routerLinkActive="active">Admin</a>
            <a routerLink="/admin/audit" routerLinkActive="active">Timeline</a>
            <a routerLink="/admin/system" routerLinkActive="active">Status</a>
          }
          @if (role() === 'WAREHOUSE') {
            <a routerLink="/warehouse" routerLinkActive="active">Warehouse</a>
            <a routerLink="/warehouse/stock" routerLinkActive="active">Stock</a>
          }
          @if (role() === 'ADMIN' || role() === 'WAREHOUSE') {
            <a routerLink="/observability" routerLinkActive="active">Monitoring</a>
          }
        </nav>

        <div class="user-box">
          <a class="cart-link" routerLink="/shop/cart">Cart <span>{{ cartCount() }}</span></a>
          @if (session(); as currentUser) {
            <span class="role-pill">{{ currentUser.role }}</span>
            <button type="button" class="ghost-button" (click)="logout()">Logout</button>
          } @else {
            <a class="primary-button" routerLink="/auth/login">Login</a>
          }
        </div>
      </header>

      <main class="workspace">
        <router-outlet />
      </main>
    </div>
  `
})
export class ShellComponent {
  readonly session = this.auth.session;
  readonly role = this.auth.role;
  readonly cartCount = this.cart.count;

  constructor(
    private readonly auth: AuthService,
    private readonly cart: CartService
  ) {}

  logout(): void {
    this.auth.logout();
  }
}
