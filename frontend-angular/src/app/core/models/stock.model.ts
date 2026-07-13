export type ReservationStatus = 'Reserved' | 'Committed' | 'Cancelled';

export interface StockItem {
  productId: string;
  sku: string;
  name: string;
  availableQuantity: number;
  reservedQuantity?: number;
}

export interface Reservation {
  reservationId: string;
  orderId: string;
  correlationId: string;
  status: ReservationStatus;
  items: {
    productId: string;
    sku: string;
    quantity: number;
  }[];
  createdAt: string;
  updatedAt?: string;
}
