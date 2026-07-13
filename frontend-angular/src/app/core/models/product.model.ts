export interface Product {
  productId: string;
  sku: string;
  name: string;
  description?: string;
  price: number;
  currency?: string;
  category?: string;
  imageUrl?: string;
}

export interface CartItem {
  product: Product;
  quantity: number;
}
