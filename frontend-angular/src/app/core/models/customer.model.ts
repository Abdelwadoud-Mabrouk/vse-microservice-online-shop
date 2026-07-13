export interface Customer {
  customerId: string;
  displayName: string;
  email: string;
  phone?: string;
  shippingAddress?: Address;
  billingAddress?: Address;
}

export interface Address {
  street: string;
  postalCode: string;
  city: string;
  country: string;
}
