import {Component, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {CurrencyPipe, NgFor} from '@angular/common';

import {CartService} from '../cart.service';
import {ShopApiService} from '../../../core/api/shop-api.service';
import {CreateOrderRequest} from "../../../core/models/order.model";
import {PaymentMethod} from '../../../core/models/payment-method';
import {Router} from "@angular/router";

@Component({
    selector: 'app-checkout-component',
    standalone: true,
    imports: [
        CurrencyPipe,
        FormsModule,
        NgFor

    ],
    templateUrl: './checkout-component.component.html',
    styleUrl: './checkout-component.component.scss'
})
export class CheckoutComponentComponent {
    error = signal<string | null>(null);
    loading = signal(false);
    private readonly idempotencyKey: string = crypto.randomUUID();

    shipping = {

        firstName: '',

        lastName: '',

        email: '',

        phone: '',

        street: '',

        houseNumber: '',

        postalCode: '',

        city: '',

        country: 'Deutschland'

    };

    paymentMethods = [
        { value: PaymentMethod.CREDIT_CARD, label: 'Kreditkarte', code: 'CC' },
        { value: PaymentMethod.KLARNA,      label: 'Klarna',      code: 'K'  },
        { value: PaymentMethod.PAYPAL,      label: 'PayPal',       code: 'PP' },
        { value: PaymentMethod.APPLE_PAY,   label: 'Apple Pay',    code: 'AP' },
        { value: PaymentMethod.GOOGLE_PAY,  label: 'Google Pay',   code: 'GP' },
    ];

    paymentMethod = PaymentMethod.CREDIT_CARD;


    constructor(
        readonly cart: CartService,
        private readonly shopApi: ShopApiService,
        private router: Router
    ) {
    }

    placeOrder(): void {

        if (this.loading()) {
            return;
        }
        this.loading.set(true);

        this.error.set('');

        const request: CreateOrderRequest = {

            customer: {

                firstName: this.shipping.firstName,

                lastName: this.shipping.lastName,

                email: this.shipping.email,

                phone: this.shipping.phone

            },

            shippingAddress: {

                street: this.shipping.street,

                houseNumber: this.shipping.houseNumber,

                postalCode: this.shipping.postalCode,

                city: this.shipping.city,

                country: this.shipping.country

            },

            currency: 'EUR',

            paymentMethod: this.paymentMethod,

            items: this.cart.items().map(item => ({

                productId: item.product.id,

                sku: item.product.sku,

                quantity: item.quantity

            }))

        };

        this.shopApi.createOrder(request,this.idempotencyKey)
            .subscribe({

                next: order => {


                    window.location.href =
                        order.approvalUrl;

                },


                error: err => {

                    this.loading.set(false);

                    console.log(err)
                    console.log(err.status)

                    const errorCode =
                        err.error?.error ?? 'UNKNOWN';


                    sessionStorage.setItem(
                        'checkoutError',
                        JSON.stringify({
                            code: errorCode
                        })
                    );


                    this.router.navigate(
                        ['/shop/checkout-error']
                    );

                }

            });

    }


}

