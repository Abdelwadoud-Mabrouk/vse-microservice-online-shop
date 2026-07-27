export interface Product {

  id: string;

  sku: string;

  name: string;

  description?: string;

  price: number;

  originalPrice?: number;

  categoryName?: string;

  categorySlug?: string;

  color?: string;

  material?: string;

  widthCm?: number;

  heightCm?: number;

  depthCm?: number;

  weightKg?: number;

  active: boolean;

  slug: string;

  imageUrls?: string[];

}


export interface CartItem {

  product: Product;

  quantity: number;

}