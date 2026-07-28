import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ProductServiceService} from "../../../core/service/product-service.service";
import {CartService} from "../cart.service";
import {RouterLink} from "@angular/router";
import {Product} from "../../../core/models/product.model";

@Component({
    selector: 'app-product-list-component',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './product-list-component.component.html',
    styleUrl: './product-list-component.component.scss'
})
export class ProductListComponentComponent implements OnInit {

    private productService = inject(ProductServiceService);

    readonly cart = inject(CartService);


    products: Product[] = [];

    loading = false;


    selectedCategory = signal<string>('all');


    categories = [
        {
            label: 'Alle',
            slug: 'all'
        },
        {
            label: 'Wohnzimmer',
            slug: 'wohnzimmer'
        },
        {
            label: 'Schlafzimmer',
            slug: 'schlafzimmer'
        },
        {
            label: 'Esszimmer',
            slug: 'esszimmer'
        },
        {
            label: 'Arbeitszimmer',
            slug: 'arbeitszimmer'
        },
        {
            label: 'Outdoor',
            slug: 'outdoor'
        }
    ];


    ngOnInit(): void {

        this.loadAllProducts();

    }

    page = 0;

    size = 12;

    totalPages = 0;

    hasNext = false;

    loadAllProducts(): void {

        this.loading = true;


        this.productService
            .getAllProducts(
                this.page,
                this.size,
                'name'
            )
            .subscribe({

                next: response => {

                    console.log(response)

                    this.products = response.content;

                    this.totalPages = response.totalPages;

                    this.hasNext = !response.last;


                    this.loading = false;

                },


                error: error => {

                    console.error(error);

                    this.loading = false;

                }

            });

    }


    nextPage() {

        if (!this.hasNext) {
            return;
        }


        this.page++;

        this.loadAllProducts();

    }

    previousPage() {

        if (this.page === 0) {
            return;
        }


        this.page--;

        this.loadAllProducts();

    }

    loadCategory(slug: string): void {


        this.selectedCategory.set(slug);


        if (slug === 'all') {

            this.loadAllProducts();

            return;
        }


        this.loading = true;


        this.productService
            .getProductsByCategory(slug)
            .subscribe({

                next: response => {

                    this.products = response.content;

                    this.loading = false;

                },


                error: error => {

                    console.error(error);

                    this.loading = false;

                }

            });

    }


    addToCart(product: Product): void {

        this.cart.add(product);

    }


    image(product: Product): string {
        return product.imageUrls?.[0]
            ? this.productService.getApiUrl() + product.imageUrls[0]
            : 'assets/images/product-placeholder.jpg';
    }
}

// todo
//  1: Saga Code (Theorie) mit Worse Case  a: Direct mit Shop
//  2: Bericht / Sequence Diagram ==> Best Case
//  2: Clean Code  a: Shop Service, b: Billing c: Angular d: Product
//  4: filters design(Hero, Header. footer)