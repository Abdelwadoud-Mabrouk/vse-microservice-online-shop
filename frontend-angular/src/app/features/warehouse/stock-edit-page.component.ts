import { Component, Input, OnChanges, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PageHeaderComponent } from '../../shared/components/page-header.component';
import { WarehouseApiService } from '../../core/api/warehouse-api.service';
import { StockItem } from '../../core/models/stock.model';

@Component({
  selector: 'app-stock-edit-page',
  standalone: true,
  imports: [FormsModule, PageHeaderComponent],
  template: `
    <app-page-header
      eyebrow="Warehouse"
      title="Edit Stock"
      description="This Angular UI replaces the old stock UI. Warehouse-service still owns the stock data."
    />

    <section class="panel">
      @if (item(); as stockItem) {
        <div class="detail-head stock-edit-head">
          <div>
            <span class="eyebrow">{{ stockItem.sku }}</span>
            <h2>{{ stockItem.name }}</h2>
          </div>
          <span class="status-chip neutral">Reserved {{ stockItem.reservedQuantity ?? 0 }}</span>
        </div>
      }

      <div class="form-grid">
        <label>Product ID <input type="text" [value]="id" readonly /></label>
        <label>Available quantity <input type="number" min="0" [(ngModel)]="availableQuantity" /></label>
      </div>
      <div class="button-row">
        <button type="button" class="primary-button" (click)="save()">Save stock</button>
        <span class="form-message">{{ message() }}</span>
      </div>
    </section>
  `
})
export class StockEditPageComponent implements OnChanges {
  @Input() id = '';
  availableQuantity = 0;
  readonly item = signal<StockItem | undefined>(undefined);
  readonly message = signal('');

  constructor(private readonly warehouseApi: WarehouseApiService) {}

  ngOnChanges(): void {
    if (!this.id) {
      return;
    }

    this.warehouseApi.getStock().subscribe((items) => {
      const item = items.find((stockItem) => stockItem.productId === this.id);
      this.item.set(item);
      this.availableQuantity = item?.availableQuantity ?? 0;
    });
  }

  save(): void {
    if (!this.id) {
      return;
    }

    this.message.set('Saving...');
    this.warehouseApi.setStock(this.id, Number(this.availableQuantity)).subscribe({
      next: (item) => {
        this.item.set(item);
        this.availableQuantity = item.availableQuantity;
        this.message.set('Stock saved.');
      },
      error: () => this.message.set('Stock could not be saved.')
    });
  }
}
