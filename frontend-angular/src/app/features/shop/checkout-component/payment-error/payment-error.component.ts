import {Component} from '@angular/core';
import {Router} from '@angular/router';


@Component({
    selector: 'app-payment-error',
    standalone: true,
    imports: [],
    templateUrl: './payment-error.component.html',
    styleUrl: './payment-error.component.scss'
})
export class PaymentErrorComponent {


    message =
        'Die Bestellung konnte momentan nicht verarbeitet werden.';


    details = '';


    constructor(
        private router: Router
    ) {
    }


    ngOnInit(): void {


        const error =
            sessionStorage.getItem('checkoutError');


        if (error) {

            const data = JSON.parse(error);


            switch (data.code) {


                case 'OUT_OF_STOCK':

                    this.message =
                        'Ein oder mehrere Artikel sind nicht mehr verfügbar.';

                    this.details =
                        'Bitte prüfen Sie Ihren Warenkorb und versuchen Sie es erneut.';

                    break;

                case 'PAYMENT_SERVICE_UNAVAILABLE':

                    this.message =
                        'Der Zahlungsdienst ist momentan nicht erreichbar.';

                    this.details =
                        'Bitte versuchen Sie es später erneut.';

                    break;


                case 'WAREHOUSE_SERVICE_UNAVAILABLE':

                    this.message =
                        'Der Lager-Service ist momentan nicht erreichbar.';

                    this.details =
                        'Ihre Bestellung konnte nicht reserviert werden. Bitte versuchen Sie es später erneut.';

                    break;

                case 'PAYMENT_FAILED':

                    this.message =
                        'Die Zahlung konnte nicht durchgeführt werden.';

                    this.details =
                        'Bitte wählen Sie eine andere Zahlungsmethode.';

                    break;


                case 'PAYMENT_PROVIDER_UNAVAILABLE':

                    this.message =
                        'Der Zahlungsdienst ist momentan nicht erreichbar.';

                    this.details =
                        'Bitte versuchen Sie es später erneut.';

                    break;


                case 'ORDER_CREATION_FAILED':

                    this.message =
                        'Die Bestellung konnte nicht erstellt werden.';

                    break;


                default:

                    this.message =
                        'Ein unerwarteter Fehler ist aufgetreten.';

            }

        }

    }


    backToCheckout(): void {

        this.router.navigate(
            ['/shop/checkout']
        );

    }

}
