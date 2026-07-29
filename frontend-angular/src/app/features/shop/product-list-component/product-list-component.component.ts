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


    products = signal<Product[]>([]);

    loading = signal(false);


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

    page = signal(0);

    size = 12;

    totalPages = signal(0);

    hasNext = signal(false);

    loadAllProducts(): void {

        this.loading.set(true);


        this.productService
            .getAllProducts(
                this.page(),
                this.size,
                'name'
            )
            .subscribe({

                next: response => {

                    this.products.set(response.content);

                    this.totalPages.set(response.totalPages);

                    this.hasNext.set(!response.last);


                    this.loading.set(false);

                },


                error: error => {

                    console.error(error);

                    this.loading.set(false);

                }

            });

    }


    nextPage() {

        if (!this.hasNext()) {
            return;
        }


        this.page.update(page => page + 1);

        this.loadAllProducts();

    }

    previousPage() {

        if (this.page() === 0) {
            return;
        }


        this.page.update(page => page - 1);

        this.loadAllProducts();

    }

    loadCategory(slug: string): void {


        this.selectedCategory.set(slug);


        if (slug === 'all') {

            this.loadAllProducts();

            return;
        }


        this.loading.set(true);


        this.productService
            .getProductsByCategory(slug)
            .subscribe({

                next: response => {

                    this.products.set(response.content);

                    this.loading.set(false);

                },


                error: error => {

                    console.error(error);

                    this.loading.set(false);

                }

            });

    }


    addToCart(product: Product): void {

        this.cart.add(product);

    }


    image(product: Product): string {
        return this.productService.resolveImageUrl(product.imageUrls?.[0]);
    }
}

// todo
//  1: Saga Code (Theorie) mit Worse Case  a: Direct mit Shop
//  2: Bericht / Sequence Diagram ==> Best Case
//  2: Clean Code  a: Shop Service, b: Billing c: Angular d: Product
//  4: filters design(Hero, Header. footer)
