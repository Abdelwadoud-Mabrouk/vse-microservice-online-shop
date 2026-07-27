/*
import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ShopApiService } from '../../core/api/shop-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { PageHeaderComponent } from '../../shared/components/page-header.component';
import { CartService } from './cart.service';
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-checkout-page',
  standalone: true,
  imports: [RouterLink, PageHeaderComponent, FormsModule],
  template: `
    <app-page-header
      eyebrow="Checkout"
      title="Create Order"
      description="Checkout sends the selected items to the shop-service. Stock, payment and invoice stay in backend services."
    />

    <section class="panel">
      <div class="summary-grid">
        <h2>Payment</h2>

        @for(method of paymentMethods; track method.value){

          <label>

            <input
                type="radio"
                name="payment"
                [value]="method.value"
                [(ngModel)]="paymentProvider"
            >

            {{method.label}}

          </label>

        }
        <div>
          <span class="metric-label">Items</span>
          <strong>{{ cart.count() }}</strong>
        </div>
        <div>
          <span class="metric-label">Customer</span>
          <strong>{{ auth.session()?.email ?? 'not logged in' }}</strong>
        </div>
      </div>

      @if (error()) {
        <div class="alert failure">{{ error() }}</div>
      }

      <div class="button-row">
        <a routerLink="/shop/cart" class="ghost-button">Back to cart</a>
        <button type="button" class="primary-button" [disabled]="cart.count() === 0 || loading()" (click)="placeOrder()">
          Place order
        </button>
      </div>
    </section>
  `
})
export class CheckoutPageComponent {
  readonly loading = signal(false);
  readonly error = signal('');

  constructor(
    readonly cart: CartService,
    readonly auth: AuthService,
    private readonly shopApi: ShopApiService,
    private readonly router: Router
  ) {}

  paymentMethod: 'PAYPAL'  | 'KLARNA'  | 'CREDIT_CARD'  | 'APPLE_PAY' | 'GOOGLE_PAY'  = 'PAYPAL';

  paymentMethods = [
    {
      value: 'PAYPAL',
      label: 'PayPal'
    },
    {
      value: 'STRIPE',
      label: 'Stripe'
    },
    {
      value: 'KLARNA',
      label: 'Klarna'
    }
  ];
  placeOrder(): void {

    const user = this.auth.session();

    if (!user) {

      this.router.navigateByUrl('/auth/login');

      return;
    }

    this.loading.set(true);

    this.error.set('');

    this.shopApi.createOrder({

      userId: user.id,

      paymentMethod: this.paymentProvider,

      items: this.cart.items().map(item => ({

        productId: item.product.id,

        sku: item.product.sku,

        quantity: item.quantity

      }))

    }).subscribe({

      next: response => {

        this.cart.clear();

        window.location.href = response.approvalUrl;

      },

      error: () => {

        this.error.set(
            'Could not create order.'
        );

        this.loading.set(false);

      }

    });

  }
}
*/
