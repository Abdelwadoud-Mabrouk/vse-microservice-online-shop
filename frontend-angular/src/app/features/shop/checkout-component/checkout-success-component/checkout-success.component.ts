import {Component, inject, OnInit, signal} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {ShopApiService} from '../../../../core/api/shop-api.service';


@Component({
    selector: 'app-checkout-success-component',
    standalone: true,
    imports: [
        RouterLink
    ],
    templateUrl: './checkout-success.component.html',
    styleUrl: './checkout-success.component.scss'
})
export class CheckoutSuccessComponent implements OnInit {


    private readonly shopApi = inject(ShopApiService);
    private readonly route = inject(ActivatedRoute);


    order = signal<any>(null);



    ngOnInit(): void {


        const paypalToken = this.route.snapshot.queryParamMap.get('token');

        if (paypalToken) {
            this.capturePaypal(paypalToken);
        }

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