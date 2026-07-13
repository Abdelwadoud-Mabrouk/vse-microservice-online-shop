export type UserRole = 'CUSTOMER' | 'ADMIN' | 'WAREHOUSE';

export interface UserSession {
  id: string;
  displayName: string;
  email: string;
  role: UserRole;
}

export const demoUsers: UserSession[] = [
  {
    id: 'demo-customer',
    displayName: 'Customer Demo',
    email: 'customer@example.com',
    role: 'CUSTOMER'
  },
  {
    id: 'demo-admin',
    displayName: 'Admin Demo',
    email: 'admin@example.com',
    role: 'ADMIN'
  },
  {
    id: 'demo-warehouse',
    displayName: 'Warehouse Demo',
    email: 'warehouse@example.com',
    role: 'WAREHOUSE'
  }
];
