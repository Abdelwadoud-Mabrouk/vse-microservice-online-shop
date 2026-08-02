INSERT INTO categories (id, name, slug, parent_id, image_url, active, created_at, updated_at)
VALUES (
           'b1000000-0000-0000-0000-000000000009',
           'Essstühle',
           'esstuehle',
           null,
           null,
           true,
           NOW(),
           NOW()
       )
    ON CONFLICT (id) DO NOTHING;

INSERT INTO products (
    id, sku, name, slug, description, category_id,
    price, original_price, width_cm, height_cm, depth_cm,
    weight_kg, material, color, active, version,
    created_at, updated_at, stock_quantity, brand
)
VALUES (
           '11111111-1111-4111-8111-111111111111',
           'SKU-CHAIR-OAK',
           'Stuhl Eiche Natur mit Stoffbezug',
           'stuhl-eiche-natur-stoffbezug',
           'Eleganter Esszimmerstuhl aus massivem Eichenholz mit bequemem Stoffbezug. Zeitloses Design für moderne Essbereiche.',
           'b1000000-0000-0000-0000-000000000009',
           249.00,
           349.00,
           52, 82, 58,
           8.5,
           'Eiche/Stoff',
           'Beige/Natur',
           true,
           0,
           NOW(),
           NOW(),
           30,
           'NordLiving'
       )
    ON CONFLICT (id) DO NOTHING;