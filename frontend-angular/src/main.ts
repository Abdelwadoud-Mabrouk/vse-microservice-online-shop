import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { adminAuthInterceptor } from './app/core/auth/admin-auth.interceptor';

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(withInterceptors([adminAuthInterceptor])),
    provideRouter(routes, withComponentInputBinding())
  ]
}).catch((error) => console.error(error));
