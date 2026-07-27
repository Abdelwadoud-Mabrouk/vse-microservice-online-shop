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

export interface OrderResponse {

  id: string;

  userId: string;

  paymentId: string;

  status: string;

  totalAmount: number;

  currency: string;

  paymentProvider: string;

  items: OrderItemResponse[];

  createdAt: string;

  updatedAt: string;

  approvalUrl: string;

}

export interface OrderItemResponse {

  productId: string;

  productName: string;

  quantity: number;

  pricePerUnit: number;

  subTotal: number;

  taxRate: number;

  taxAmount: number;

  totalPrice: number;

}

export interface CheckoutResponse {

  orderId: string;

  status: string;

  totalAmount: number;

  currency: string;

  approvalUrl: string;

}

export interface CreateOrderRequest {

  customer: CustomerInformationRequest;

  shippingAddress: ShippingAddressRequest;

  currency: string;

  paymentMethod: 'PAYPAL'  | 'KLARNA'  | 'CREDIT_CARD'  | 'APPLE_PAY' | 'GOOGLE_PAY'

  items: CreateOrderItem[];

}
export interface CustomerInformationRequest {

  firstName: string;

  lastName: string;

  email: string;

  phone?: string;

}

export interface ShippingAddressRequest {

  street: string;

  houseNumber: string;

  postalCode: string;

  city: string;

  country: string;

}

export interface CreateOrderItem {

  productId: string;

  sku: string;

  quantity: number;

}