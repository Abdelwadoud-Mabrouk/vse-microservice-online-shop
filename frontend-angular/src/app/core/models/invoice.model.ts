export interface Invoice {
  invoiceId: string;
  orderId: string;
  correlationId: string;
  customerEmail: string;
  amount: number;
  currency: string;
  createdAt: string;
}
