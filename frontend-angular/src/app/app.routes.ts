import { Routes } from '@angular/router';
import { ShellComponent } from './core/layout/shell.component';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';
import { LoginPageComponent } from './features/auth/login-page.component';
import { RegisterPageComponent } from './features/auth/register-page.component';
import { ProductListPageComponent } from './features/shop/product-list-page.component';
import { ProductDetailPageComponent } from './features/shop/product-detail-page.component';
import { CartPageComponent } from './features/shop/cart-page.component';
import { CheckoutPageComponent } from './features/shop/checkout-page.component';
import { OrderStatusPageComponent } from './features/shop/order-status-page.component';
import { CustomerProfilePageComponent } from './features/customer/customer-profile-page.component';
import { CustomerOrdersPageComponent } from './features/customer/customer-orders-page.component';
import { CustomerAddressesPageComponent } from './features/customer/customer-addresses-page.component';
import { AdminDashboardPageComponent } from './features/admin/admin-dashboard-page.component';
import { ProductManagementPageComponent } from './features/admin/product-management-page.component';
import { ProductEditPageComponent } from './features/admin/product-edit-page.component';
import { CustomerManagementPageComponent } from './features/admin/customer-management-page.component';
import { OrderOverviewPageComponent } from './features/admin/order-overview-page.component';
import { AuditSearchPageComponent } from './features/admin/audit-search-page.component';
import { InvoiceOverviewPageComponent } from './features/admin/invoice-overview-page.component';
import { SystemStatusPageComponent } from './features/admin/system-status-page.component';
import { WarehouseDashboardPageComponent } from './features/warehouse/warehouse-dashboard-page.component';
import { StockOverviewPageComponent } from './features/warehouse/stock-overview-page.component';
import { StockEditPageComponent } from './features/warehouse/stock-edit-page.component';
import { ReservationOverviewPageComponent } from './features/warehouse/reservation-overview-page.component';
import { ObservabilityPageComponent } from './features/observability/observability-page.component';

export const routes: Routes = [
  { path: '', redirectTo: 'shop/products', pathMatch: 'full' },
  { path: 'auth/login', component: LoginPageComponent },
  { path: 'auth/register', component: RegisterPageComponent },
  {
    path: '',
    component: ShellComponent,
    children: [
      {
        path: 'shop',
        children: [
          { path: '', redirectTo: 'products', pathMatch: 'full' },
          { path: 'products', component: ProductListPageComponent },
          { path: 'products/:id', component: ProductDetailPageComponent },
          { path: 'cart', component: CartPageComponent },
          { path: 'checkout', component: CheckoutPageComponent, canActivate: [authGuard] },
          { path: 'orders/:id', component: OrderStatusPageComponent, canActivate: [authGuard] }
        ]
      },
      {
        path: 'account',
        canActivate: [authGuard],
        children: [
          { path: '', component: CustomerProfilePageComponent },
          { path: 'orders', component: CustomerOrdersPageComponent },
          { path: 'addresses', component: CustomerAddressesPageComponent }
        ]
      },
      {
        path: 'admin',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        children: [
          { path: '', component: AdminDashboardPageComponent },
          { path: 'products', component: ProductManagementPageComponent },
          { path: 'products/new', component: ProductEditPageComponent },
          { path: 'products/:id/edit', component: ProductEditPageComponent },
          { path: 'customers', component: CustomerManagementPageComponent },
          { path: 'orders', component: OrderOverviewPageComponent },
          { path: 'audit', component: AuditSearchPageComponent },
          { path: 'system', component: SystemStatusPageComponent },
          { path: 'invoices', component: InvoiceOverviewPageComponent }
        ]
      },
      {
        path: 'warehouse',
        canActivate: [roleGuard],
        data: { roles: ['WAREHOUSE'] },
        children: [
          { path: '', component: WarehouseDashboardPageComponent },
          { path: 'stock', component: StockOverviewPageComponent },
          { path: 'stock/:id/edit', component: StockEditPageComponent },
          { path: 'reservations', component: ReservationOverviewPageComponent }
        ]
      },
      {
        path: 'observability',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'WAREHOUSE'] },
        component: ObservabilityPageComponent
      }
    ]
  },
  { path: '**', redirectTo: 'shop/products' }
];
