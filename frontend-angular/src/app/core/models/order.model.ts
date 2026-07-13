export type OrderStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'CANCELLED';

export interface OrderItem {
  productId: string;
  sku: string;
  productName?: string;
  quantity: number;
  pricePerUnit?: number;
  subTotal?: number;
  taxRate?: number;
  taxAmount?: number;
  totalPrice?: number;
}

export interface Order {
  id: string;
  correlationId: string;
  userId: string;
  status: OrderStatus;
  totalAmount: number;
  items: OrderItem[];
}

export interface CreateOrderRequest {
  userId: string;
  items: {
    productId: string;
    sku: string;
    quantity: number;
  }[];
}
