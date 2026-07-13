import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { UserRole, UserSession, demoUsers } from '../models/user.model';

const sessionKey = 'online-shop-session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly sessionSignal = signal<UserSession | null>(this.readSession());

  readonly session = this.sessionSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.sessionSignal() !== null);
  readonly role = computed(() => this.sessionSignal()?.role ?? null);

  constructor(private readonly router: Router) {}

  loginAs(role: UserRole): void {
    const user = demoUsers.find((candidate) => candidate.role === role);
    if (!user) {
      return;
    }

    sessionStorage.setItem(sessionKey, JSON.stringify(user));
    this.sessionSignal.set(user);
    this.router.navigateByUrl(role === 'ADMIN' ? '/admin' : role === 'WAREHOUSE' ? '/warehouse' : '/shop/products');
  }

  logout(): void {
    sessionStorage.removeItem(sessionKey);
    this.sessionSignal.set(null);
    this.router.navigateByUrl('/auth/login');
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
