import {Component, inject, OnDestroy, OnInit, signal} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {CurrencyPipe} from '@angular/common';
import {ShopApiService} from '../../../../core/api/shop-api.service';


@Component({
    selector: 'app-checkout-success-component',
    standalone: true,
    imports: [
        RouterLink,
        CurrencyPipe
    ],
    templateUrl: './checkout-success.component.html',
    styleUrl: './checkout-success.component.scss'
})
export class CheckoutSuccessComponent implements OnInit {


    private readonly shopApi = inject(ShopApiService);
    private readonly route = inject(ActivatedRoute);


    order = signal<any>(null);



    ngOnInit(): void {


        const orderId = sessionStorage.getItem('lastOrderId');

        if (!orderId) {
            return;
        }

        const paypalToken = this.route.snapshot.queryParamMap.get('token');

        /*
          PayPal: Capture über das Backend anstoßen.
          Das Geld wird eingezogen und die SAGA läuft asynchron im Hintergrund.
        */
        if (paypalToken) {
            console.log(paypalToken)
            this.capturePaypal(paypalToken);
        }

        /*
          Nach dem erfolgreichen Start des Checkouts löschen wir die ID aus dem
          SessionStorage, damit ein Page-Refresh nicht versehentlich ein doppeltes
          PayPal-Capture triggert.
        */
        sessionStorage.removeItem('lastOrderId');

    }


    private capturePaypal(
        paypalOrderId: string
    ): void {


        this.shopApi
            .capturePaypalOrder(paypalOrderId)
            .subscribe({

                next: response => {

                    console.log(
                        'Paypal capture finished',
                        response
                    );

                },


                error: error => {

                    console.error(
                        'Paypal capture failed',
                        error
                    );

                }

            });


    }

}