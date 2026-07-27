import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { UserRole, UserSession, demoUsers } from '../models/user.model';

const sessionKey = 'online-shop-session';
const adminAuthorizationKey = 'online-shop-admin-authorization';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly sessionSignal = signal<UserSession | null>(this.readSession());

  readonly session = this.sessionSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.sessionSignal() !== null);
  readonly role = computed(() => this.sessionSignal()?.role ?? null);

  constructor(private readonly router: Router) {}

  loginAs(role: UserRole, username?: string, password?: string): boolean {
    const user = demoUsers.find((candidate) => candidate.role === role);
    if (!user) {
      return false;
    }

    if (role === 'ADMIN') {
      if (!username || !password) {
        return false;
      }
      sessionStorage.setItem(adminAuthorizationKey, `Basic ${btoa(`${username}:${password}`)}`);
    } else {
      sessionStorage.removeItem(adminAuthorizationKey);
    }

    sessionStorage.setItem(sessionKey, JSON.stringify(user));
    this.sessionSignal.set(user);
    this.router.navigateByUrl(role === 'ADMIN' ? '/admin' : role === 'WAREHOUSE' ? '/warehouse' : '/shop/products');
    return true;
  }

  logout(): void {
    sessionStorage.removeItem(sessionKey);
    sessionStorage.removeItem(adminAuthorizationKey);
    this.sessionSignal.set(null);
    this.router.navigateByUrl('/auth/login');
  }

  adminAuthorization(): string | null {
    return this.sessionSignal()?.role === 'ADMIN'
      ? sessionStorage.getItem(adminAuthorizationKey)
      : null;
  }

  hasRole(roles: UserRole[]): boolean {
    const currentRole = this.sessionSignal()?.role;
    return currentRole !== undefined && roles.includes(currentRole);
  }

  private readSession(): UserSession | null {
    const raw = sessionStorage.getItem(sessionKey);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as UserSession;
    } catch {
      sessionStorage.removeItem(sessionKey);
      return null;
    }
  }
}
