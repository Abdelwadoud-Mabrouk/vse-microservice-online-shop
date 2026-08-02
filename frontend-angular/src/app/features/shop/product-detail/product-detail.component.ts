import {ProductService} from "../../../core/api/product.service";
import {CartService} from "../cart.service";
import {Product} from "../../../core/models/product.model";
import {Component, Input, SimpleChanges} from "@angular/core";
import {Observable} from "rxjs";
import {AsyncPipe, CurrencyPipe} from "@angular/common";


const categoryImages: Record<string, string> = {
    betten: 'https://images.unsplash.com/photo-1505693314120-0d443867891c?w=800',
    sofas: 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800',
    tische: 'https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=800',
    stuehle: 'https://images.unsplash.com/photo-1517705008128-361805f42e86?w=800'
};


@Component({
    selector: 'app-product-detail',
    standalone: true,
    imports: [
        CurrencyPipe,
        AsyncPipe
    ],
    templateUrl: './product-detail.component.html',
    styleUrl: './product-detail.component.scss'
})
export class ProductDetailComponent {
    @Input() slug = '';

    product$!: Observable<Product>;

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['slug'] && this.slug) {
            this.product$ = this.productService.getProductBySlug(this.slug);
        }
    }

    constructor(
        private readonly productService: ProductService,
        private readonly cart: CartService
    ) {
    }


    add(product: Product): void {
        this.cart.add(product);
    }


    private furnitureImages(product: Product): string[] {

        if (product.imageUrls && product.imageUrls.length > 0) {
            return product.imageUrls;
        }

        return [
            this.categoryImage(product)
        ];
    }


    private furnitureImage(product: Product): string {

        return this.furnitureImages(product)[0];

    }


    private categoryImage(product: Product): string {

        const category = product.categorySlug?.toLowerCase();

        // @ts-ignore
        return categoryImages[category]
            ?? 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800';

    }

    selectedImage = '';

    selectImage(image: string): void {
        this.selectedImage = image;
    }


    mainImage(product: Product): string {
        const image = this.selectedImage || product.imageUrls?.[0];
        return this.productService.formatImageUrl(image, 'assets/images/product-placeholder.jpg');
    }

    images(product: Product): string[] {
        if (!product.imageUrls || product.imageUrls.length === 0) {
            return ['assets/images/product-placeholder.jpg'];
        }

        return product.imageUrls.map(img =>this.productService.formatImageUrl(img));
    }


}
