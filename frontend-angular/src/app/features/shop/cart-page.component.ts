import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CartService } from './cart.service';
import { PageHeaderComponent } from '../../shared/components/page-header.component';

@Component({
    selector: 'app-cart-page',
    imports: [CurrencyPipe, RouterLink, PageHeaderComponent],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
    <app-page-header
      eyebrow="Checkout"
      title="Your cart"
      description="Review products before the order is created in the shop-service."
    >
      <a routerLink="/shop/products" class="ghost-button">Continue shopping</a>
    </app-page-header>

    <section class="cart-layout">
      <div class="panel cart-panel">
        @for (item of cart.items(); track item.product.id) {
          <div class="cart-row">
            <div>
              <strong>{{ item.product.name }}</strong>
              <small>{{ item.product.sku }}</small>
            </div>
            <label>
              Qty
              <input
                type="number"
                min="1"
                [value]="item.quantity"
                (change)="update(item.product.id, $any($event.target).valueAsNumber)"
              />
            </label>
            <div class="line-total">
              {{ item.product.price * item.quantity | currency:'EUR' }}
            </div>
            <button type="button" class="ghost-button" (click)="cart.remove(item.product.id)">Remove</button>
          </div>
        } @empty {
          <div class="empty-state empty-cart">
            <strong>Your cart is empty.</strong>
            <span>Pick a product first and then continue to checkout.</span>
            <a routerLink="/shop/products" class="primary-button">Browse products</a>
          </div>
        }
      </div>

      <aside class="panel checkout-summary">
        <span class="eyebrow">Summary</span>
        <div class="summary-line">
          <span>Items</span>
          <strong>{{ cart.count() }}</strong>
        </div>
        <div class="summary-line total">
          <span>Total</span>
          <strong>{{ cart.total() | currency: 'EUR' }}</strong>
        </div>
        @if (cart.count() > 0) {

          <a routerLink="/shop/checkout" class="primary-button">
            Checkout
          </a>

        } @else {

          <button disabled class="primary-button">
            Checkout
          </button>

        }
        <small>Cart is temporary UI state. Checkout persists the order in the backend.</small>
      </aside>
    </section>
  `
})
export class CartPageComponent {
  constructor(readonly cart: CartService) {}

  update(productId: string, quantity: number): void {
    this.cart.updateQuantity(productId, quantity);
  }
}
