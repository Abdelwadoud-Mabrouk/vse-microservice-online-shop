import { Routes } from '@angular/router';
import { ShellComponent } from './core/layout/shell.component';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';
import { ProductListComponentComponent } from './features/shop/product-list-component/product-list-component.component';
import { CheckoutComponentComponent } from './features/shop/checkout-component/checkout-component.component';
import { CheckoutSuccessComponent } from './features/shop/checkout-component/checkout-success-component/checkout-success.component';
import { ProductDetailComponent } from './features/shop/product-detail/product-detail.component';
import { PaymentErrorComponent } from './features/shop/checkout-component/payment-error/payment-error.component';

export const routes: Routes = [
  { path: '', redirectTo: 'shop/products', pathMatch: 'full' },
  {
    path: 'auth/login',
    loadComponent: () => import('./features/auth/login-page.component')
      .then(module => module.LoginPageComponent)
  },
  {
    path: '',
    component: ShellComponent,
    children: [
      {
        path: 'shop',
        children: [
          { path: '', redirectTo: 'products', pathMatch: 'full' },
          { path: 'products', component: ProductListComponentComponent },
          { path: 'products/:slug', component: ProductDetailComponent },
          {
            path: 'cart',
            loadComponent: () => import('./features/shop/cart-page.component')
              .then(module => module.CartPageComponent)
          },
          { path: 'checkout', component: CheckoutComponentComponent },
          {
            path: 'checkout-error',
            component: PaymentErrorComponent
          },
          {
            path: 'orders/:id',
            loadComponent: () => import('./features/shop/order-status-page.component')
              .then(module => module.OrderStatusPageComponent),
            canActivate: [authGuard]
          },
          {
            path: 'checkout/success',
            component: CheckoutSuccessComponent
          }
        ]
      },
      {
        path: 'admin',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        children: [
          {
            path: '',
            loadComponent: () => import('./features/admin/admin-dashboard-page.component')
              .then(module => module.AdminDashboardPageComponent)
          },
          { path: 'products', component: ProductListComponentComponent },
          {
            path: 'products/new',
            loadComponent: () => import('./features/admin/product-edit-page.component')
              .then(module => module.ProductEditPageComponent)
          },
          {
            path: 'products/:id/edit',
            loadComponent: () => import('./features/admin/product-edit-page.component')
              .then(module => module.ProductEditPageComponent)
          },
          {
            path: 'orders',
            loadComponent: () => import('./features/admin/order-overview-page.component')
              .then(module => module.OrderOverviewPageComponent)
          },
          {
            path: 'audit',
            loadComponent: () => import('./features/admin/audit-search-page.component')
              .then(module => module.AuditSearchPageComponent)
          },
          {
            path: 'system',
            loadComponent: () => import('./features/admin/system-status-page.component')
              .then(module => module.SystemStatusPageComponent)
          },
          {
            path: 'invoices',
            loadComponent: () => import('./features/admin/invoice-overview-page.component')
              .then(module => module.InvoiceOverviewPageComponent)
          }
        ]
      },
      {
        path: 'warehouse',
        canActivate: [roleGuard],
        data: { roles: ['WAREHOUSE'] },
        children: [
          {
            path: '',
            loadComponent: () => import('./features/warehouse/warehouse-dashboard-page.component')
              .then(module => module.WarehouseDashboardPageComponent)
          },
          {
            path: 'stock',
            loadComponent: () => import('./features/warehouse/stock-overview-page.component')
              .then(module => module.StockOverviewPageComponent)
          },
          {
            path: 'stock/:id/edit',
            loadComponent: () => import('./features/warehouse/stock-edit-page.component')
              .then(module => module.StockEditPageComponent)
          },
          {
            path: 'reservations',
            loadComponent: () => import('./features/warehouse/reservation-overview-page.component')
              .then(module => module.ReservationOverviewPageComponent)
          }
        ]
      },
      {
        path: 'observability',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'WAREHOUSE'] },
        loadComponent: () => import('./features/observability/observability-page.component')
          .then(module => module.ObservabilityPageComponent)
      }
    ]
  },
  { path: '**', redirectTo: 'shop/products' }
];
