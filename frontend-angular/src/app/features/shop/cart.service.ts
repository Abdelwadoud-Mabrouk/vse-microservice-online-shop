import { Injectable, computed, signal } from '@angular/core';
import { CartItem, Product } from '../../core/models/product.model';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly itemsSignal = signal<CartItem[]>([]);

  readonly items = this.itemsSignal.asReadonly();
  readonly total = computed(() =>
    this.itemsSignal().reduce((sum, item) => sum + item.product.price * item.quantity, 0)
  );
  readonly count = computed(() => this.itemsSignal().reduce((sum, item) => sum + item.quantity, 0));

  add(product: Product): void {
    const current = this.itemsSignal();
    const existing = current.find((item) => item.product.id === product.id);
    if (existing) {
      this.itemsSignal.set(
        current.map((item) =>
          item.product.id === product.id ? { ...item, quantity: item.quantity + 1 } : item
        )
      );
      return;
    }

    this.itemsSignal.set([...current, { product, quantity: 1 }]);
  }

  updateQuantity(productId: string, quantity: number): void {
    if (quantity <= 0) {
      this.remove(productId);
      return;
    }

    this.itemsSignal.set(
      this.itemsSignal().map((item) => (item.product.id === productId ? { ...item, quantity } : item))
    );
  }

  remove(productId: string): void {
    this.itemsSignal.set(this.itemsSignal().filter((item) => item.product.id !== productId));
  }

  clear(): void {
    this.itemsSignal.set([]);
  }
}
