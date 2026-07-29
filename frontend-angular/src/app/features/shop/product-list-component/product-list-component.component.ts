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

    // 💡 In Angular 22 als Signals definiert:
    products = signal<Product[]>([]);
    loading = signal<boolean>(false);
    selectedCategory = signal<string>('all');

    page = 0;
    size = 12;
    totalPages = 0;
    hasNext = false;

    categories = [
        { label: 'Alle', slug: 'all' },
        { label: 'Wohnzimmer', slug: 'wohnzimmer' },
        { label: 'Schlafzimmer', slug: 'schlafzimmer' },
        { label: 'Esszimmer', slug: 'esszimmer' },
        { label: 'Arbeitszimmer', slug: 'arbeitszimmer' },
        { label: 'Outdoor', slug: 'outdoor' }
    ];

    ngOnInit(): void {
        this.loadAllProducts();
    }

    loadAllProducts(): void {
        this.loading.set(true); // Signal Wert setzen

        this.productService
            .getAllProducts(this.page, this.size, 'name')
            .subscribe({
                next: response => {
                    console.log(response);

                    this.products.set(response.content || []);
                    this.totalPages = response.totalPages;
                    this.hasNext = !response.last;

                    this.loading.set(false);
                },
                error: error => {
                    console.error(error);
                    this.loading.set(false);
                }
            });
    }

    nextPage() {
        if (!this.hasNext) return;
        this.page++;
        this.loadAllProducts();
    }

    previousPage() {
        if (this.page === 0) return;
        this.page--;
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
                    this.products.set(response.content || []);
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
        return product.imageUrls?.[0]
            ? this.productService.getApiUrl() + product.imageUrls[0]
            : 'assets/images/product_custom.jpg';
    }
}