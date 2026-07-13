import { Product } from '../core/models/product.model';

export type FurnitureCategory = 'all' | 'sofas' | 'chairs' | 'tables' | 'storage' | 'lighting';

export const furnitureCategories: { value: FurnitureCategory; label: string }[] = [
  { value: 'all', label: 'All' },
  { value: 'sofas', label: 'Sofas' },
  { value: 'chairs', label: 'Chairs' },
  { value: 'tables', label: 'Tables' },
  { value: 'storage', label: 'Storage' },
  { value: 'lighting', label: 'Lighting' }
];

const categoryImages: Record<Exclude<FurnitureCategory, 'all'>, string> = {
  sofas: 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=1100&q=85',
  chairs: 'https://images.unsplash.com/photo-1517705008128-361805f42e86?auto=format&fit=crop&w=1100&q=85',
  tables: 'https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?auto=format&fit=crop&w=1100&q=85',
  storage: 'https://images.unsplash.com/photo-1594026112284-02bb6f3352fe?auto=format&fit=crop&w=1100&q=85',
  lighting: 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=1100&q=85'
};

export function furnitureCategory(product: Product): Exclude<FurnitureCategory, 'all'> {
  const text = `${product.category ?? ''} ${product.name} ${product.description ?? ''} ${product.sku}`.toLowerCase();
  if (/sofa|couch|lounge/.test(text)) return 'sofas';
  if (/chair|stuhl|seat|bench/.test(text)) return 'chairs';
  if (/table|desk|tisch/.test(text)) return 'tables';
  if (/shelf|shelves|wardrobe|cabinet|storage|regal|schrank/.test(text)) return 'storage';
  if (/lamp|light|lighting|leuchte/.test(text)) return 'lighting';
  return 'chairs';
}

export function furnitureCategoryLabel(product: Product): string {
  const category = furnitureCategory(product);
  return furnitureCategories.find((item) => item.value === category)?.label ?? 'Furniture';
}

export function furnitureImage(product: Product): string {
  return product.imageUrl ?? categoryImages[furnitureCategory(product)];
}

export function furnitureImageStyle(product: Product): string {
  return `linear-gradient(180deg, rgba(20, 27, 23, 0.04), rgba(20, 27, 23, 0.22)), url('${furnitureImage(product)}')`;
}
