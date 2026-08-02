BEGIN;

INSERT INTO categories (id, name, slug, active, created_at, updated_at)
VALUES
    ('b0000000-0000-0000-0000-000000000001', 'Wohnzimmer', 'wohnzimmer', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b0000000-0000-0000-0000-000000000002', 'Schlafzimmer', 'schlafzimmer', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b0000000-0000-0000-0000-000000000003', 'Esszimmer', 'esszimmer', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b0000000-0000-0000-0000-000000000004', 'Arbeitszimmer', 'arbeitszimmer', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b0000000-0000-0000-0000-000000000005', 'Outdoor', 'outdoor', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (slug) DO UPDATE SET
    name = EXCLUDED.name,
    active = true,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO products (
    id,
    sku,
    name,
    slug,
    description,
    category_id,
    price,
    original_price,
    material,
    color,
    active,
    stock_quantity,
    brand,
    version,
    created_at,
    updated_at
)
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'SOFA-SAMT-ANTH-3S', 'Samt Sofa Anthrazit 3-Sitzer', 'samt-sofa-anthrazit-3-sitzer', 'Komfortables Dreisitzer-Sofa mit weichem Samtbezug.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 899.00, 999.00, 'Samt', 'Anthrazit', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000002', 'LDR-SOFA-COGNAC-2S', 'Leder Sofa Cognac 2-Sitzer', 'leder-sofa-cognac-2-sitzer', 'Kompaktes Ledersofa im warmen Cognac-Ton.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 1199.00, 1299.00, 'Leder', 'Cognac', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000003', 'ECKSOFA-GRAU-MODERN', 'Ecksofa Grau Modern', 'ecksofa-grau-modern', 'Modernes Ecksofa mit großzügiger Liegefläche.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 1399.00, 1499.00, 'Webstoff', 'Grau', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000004', 'SOFA-BEIGE-LOUNGE', 'Lounge Sofa Beige', 'lounge-sofa-beige', 'Helles Lounge-Sofa für eine ruhige Wohnatmosphäre.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 1099.00, 1199.00, 'Bouclé', 'Beige', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000005', 'SOFA-CHESTER-BRAUN', 'Chesterfield Sofa Braun', 'chesterfield-sofa-braun', 'Klassisches Chesterfield-Sofa mit markanter Knopfheftung.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 1299.00, 1399.00, 'Kunstleder', 'Braun', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000006', 'SOFA-MODUL-SCHWARZ', 'Modulares Sofa Schwarz', 'modulares-sofa-schwarz', 'Flexibel kombinierbares Sofa für moderne Räume.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 1499.00, 1599.00, 'Webstoff', 'Schwarz', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000007', 'SOFA-SCANDI-WEISS', 'Scandi Sofa Weiß', 'scandi-sofa-weiss', 'Leichtes skandinavisches Design mit hellen Holzfüßen.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 949.00, 1049.00, 'Leinen', 'Weiß', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000008', 'SOFA-GRUEN-VELVET', 'Velvet Sofa Grün', 'velvet-sofa-gruen', 'Sattes Grün und weicher Velvet-Bezug als Blickfang.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 999.00, 1099.00, 'Samt', 'Grün', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000009', 'SOFA-SCHLAF-BLAU', 'Sofa Bett Blau', 'sofa-bett-blau', 'Praktisches Schlafsofa für Gäste und kleine Wohnungen.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 849.00, 949.00, 'Webstoff', 'Blau', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000010', 'SOFA-LUXUS-CREME', 'Luxus Sofa Creme', 'luxus-sofa-creme', 'Großzügiges Sofa mit besonders weicher Polsterung.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 1699.00, 1799.00, 'Bouclé', 'Creme', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000011', 'TISCH-MARMOR-SCHWARZ', 'Couchtisch Marmor Schwarz', 'couchtisch-marmor-schwarz', 'Eleganter Couchtisch mit dunkler Marmoroptik.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 449.00, 499.00, 'Marmor', 'Schwarz', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000012', 'TISCH-EICHE-NATUR', 'Couchtisch Eiche Natur', 'couchtisch-eiche-natur', 'Zeitloser Couchtisch aus natürlicher Eiche.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 329.00, 379.00, 'Eiche', 'Natur', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000013', 'TISCH-GLAS-MODERN', 'Glas Couchtisch Modern', 'glas-couchtisch-modern', 'Transparenter Couchtisch mit schlankem Metallgestell.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 289.00, 329.00, 'Glas', 'Transparent', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000014', 'TISCH-INDUSTRIAL-METALL', 'Industrial Couchtisch Metall', 'industrial-couchtisch-metall', 'Robuster Couchtisch im Industrial-Stil.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 379.00, 429.00, 'Metall', 'Schwarz', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000015', 'TISCH-RUND-HOLZ', 'Runder Couchtisch Holz', 'runder-couchtisch-holz', 'Runder Holztisch mit weichen, wohnlichen Formen.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 349.00, 399.00, 'Holz', 'Natur', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000016', 'TV-MOEBEL-EICHE-180', 'TV-Möbel Eiche Natur 180cm', 'tv-moebel-eiche-natur-180', 'Breites TV-Möbel mit viel geschlossenem Stauraum.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 699.00, 749.00, 'Eiche', 'Natur', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000017', 'TV-LOWBOARD-SCHWARZ', 'TV Lowboard Schwarz Modern', 'tv-lowboard-schwarz-modern', 'Minimalistisches Lowboard mit grifflosen Fronten.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 599.00, 649.00, 'Holzwerkstoff', 'Schwarz', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000018', 'TV-WAND-MODERN', 'TV Wandkombination Modern', 'tv-wandkombination-modern', 'Abgestimmte Kombination aus Lowboard und Wandregalen.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 999.00, 1099.00, 'Holzwerkstoff', 'Grau', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000019', 'TV-BOARD-INDUSTRIAL', 'TV Board Industrial Style', 'tv-board-industrial-style', 'TV-Board aus Holz und schwarzem Metall.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 649.00, 699.00, 'Holz und Metall', 'Braun', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000020', 'TV-MOEBEL-WEISS', 'TV Möbel Weiß Hochglanz', 'tv-moebel-weiss-hochglanz', 'Helles TV-Möbel mit pflegeleichter Hochglanzfront.', (SELECT id FROM categories WHERE slug = 'wohnzimmer'), 579.00, 629.00, 'Holzwerkstoff', 'Weiß', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000021', 'REGAL-EICHE-180', 'Regal Eiche Natur 180cm', 'regal-eiche-natur-180', 'Hohes Bücherregal aus warmer Eiche.', (SELECT id FROM categories WHERE slug = 'arbeitszimmer'), 499.00, 549.00, 'Eiche', 'Natur', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000022', 'REGAL-METALL-SCHWARZ', 'Metall Regal Schwarz', 'metall-regal-schwarz', 'Offenes schwarzes Metallregal für Büro und Wohnen.', (SELECT id FROM categories WHERE slug = 'arbeitszimmer'), 299.00, 349.00, 'Metall', 'Schwarz', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000023', 'REGAL-WAND-SYSTEM', 'Wandregal System Modern', 'wandregal-system-modern', 'Modulares Wandregal mit flexibel nutzbaren Fächern.', (SELECT id FROM categories WHERE slug = 'arbeitszimmer'), 429.00, 479.00, 'Holzwerkstoff', 'Weiß', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000024', 'REGAL-LOFT-HOLZ', 'Loft Regal Holz Metall', 'loft-regal-holz-metall', 'Offenes Loft-Regal für Geschirr, Bücher oder Dekoration.', (SELECT id FROM categories WHERE slug = 'esszimmer'), 389.00, 439.00, 'Holz und Metall', 'Braun', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000025', 'REGAL-ECKE-MODERN', 'Eckregal Modern', 'eckregal-modern', 'Platzsparendes Eckregal für Schlafzimmer und Flur.', (SELECT id FROM categories WHERE slug = 'schlafzimmer'), 249.00, 289.00, 'Holzwerkstoff', 'Eiche', true, 30, 'WohnWerk', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
    sku = EXCLUDED.sku,
    name = EXCLUDED.name,
    slug = EXCLUDED.slug,
    description = EXCLUDED.description,
    category_id = EXCLUDED.category_id,
    price = EXCLUDED.price,
    original_price = EXCLUDED.original_price,
    material = EXCLUDED.material,
    color = EXCLUDED.color,
    active = true,
    stock_quantity = EXCLUDED.stock_quantity,
    brand = EXCLUDED.brand,
    updated_at = CURRENT_TIMESTAMP;

DELETE FROM product_images
WHERE product_id BETWEEN
    'c0000000-0000-0000-0000-000000000001'::uuid
    AND 'c0000000-0000-0000-0000-000000000025'::uuid;

WITH product_image(product_id, image_url) AS (
    VALUES
        ('c0000000-0000-0000-0000-000000000001'::uuid, 'https://images.pexels.com/photos/5732/design-home-interior-couch.jpg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000002'::uuid, 'https://images.pexels.com/photos/8112333/pexels-photo-8112333.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000003'::uuid, 'https://images.pexels.com/photos/31594917/pexels-photo-31594917.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000004'::uuid, 'https://images.pexels.com/photos/24304750/pexels-photo-24304750.png?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000005'::uuid, 'https://images.unsplash.com/photo-1573866926487-a1865558a9cf?auto=format&fit=crop&w=1000&q=80'),
        ('c0000000-0000-0000-0000-000000000006'::uuid, 'https://images.pexels.com/photos/34131464/pexels-photo-34131464.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000007'::uuid, 'https://images.pexels.com/photos/3705529/pexels-photo-3705529.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000008'::uuid, 'https://images.pexels.com/photos/6580413/pexels-photo-6580413.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000009'::uuid, 'https://images.pexels.com/photos/8135269/pexels-photo-8135269.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000010'::uuid, 'https://images.pexels.com/photos/7160605/pexels-photo-7160605.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000011'::uuid, 'https://images.pexels.com/photos/7607331/pexels-photo-7607331.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000012'::uuid, 'https://images.pexels.com/photos/19966793/pexels-photo-19966793.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000013'::uuid, 'https://images.pexels.com/photos/1543447/pexels-photo-1543447.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000014'::uuid, 'https://images.pexels.com/photos/4115559/pexels-photo-4115559.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000015'::uuid, 'https://images.pexels.com/photos/7607454/pexels-photo-7607454.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000016'::uuid, 'https://images.pexels.com/photos/6636297/pexels-photo-6636297.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000017'::uuid, 'https://images.pexels.com/photos/32177950/pexels-photo-32177950.png?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000018'::uuid, 'https://images.pexels.com/photos/6527053/pexels-photo-6527053.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000019'::uuid, 'https://images.pexels.com/photos/18289778/pexels-photo-18289778.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000020'::uuid, 'https://images.pexels.com/photos/6527028/pexels-photo-6527028.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000021'::uuid, 'https://images.pexels.com/photos/30184046/pexels-photo-30184046.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000022'::uuid, 'https://images.pexels.com/photos/6045084/pexels-photo-6045084.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000023'::uuid, 'https://images.pexels.com/photos/19955736/pexels-photo-19955736.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000024'::uuid, 'https://images.pexels.com/photos/7046149/pexels-photo-7046149.jpeg?auto=compress&cs=tinysrgb&w=1200'),
        ('c0000000-0000-0000-0000-000000000025'::uuid, 'https://images.pexels.com/photos/7243801/pexels-photo-7243801.jpeg?auto=compress&cs=tinysrgb&w=1200')
)
INSERT INTO product_images (product_id, image_url)
SELECT product_id, image_url
FROM product_image;

-- ============================================================
-- PRODUCT IMAGES
-- Generated automatically
-- ============================================================

INSERT INTO products
(
    id,
    sku,
    name,
    slug,
    description,
    category_id,
    price,
    original_price,
    width_cm,
    height_cm,
    depth_cm,
    weight_kg,
    material,
    color,
    active,
    version,
    created_at,
    updated_at,
    stock_quantity,
    brand
)
VALUES
    (
        '11111111-1111-4111-8111-111111111111',
        'SKU-CHAIR-OAK',
        'Stuhl Eiche Natur mit Stoffbezug',
        'stuhl-eiche-natur-stoffbezug',
        'Eleganter Esszimmerstuhl aus massivem Eichenholz mit bequemem Stoffbezug. Zeitloses Design für moderne Essbereiche.',
        (SELECT id FROM categories WHERE slug = 'esszimmer'),
        249.00,
        349.00,
        52,
        82,
        58,
        8.5,
        'Eiche/Stoff',
        'Beige/Natur',
        true,
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        30,
        'NordLiving'
    )
    ON CONFLICT (id) DO UPDATE SET
    sku = EXCLUDED.sku,
                            name = EXCLUDED.name,
                            slug = EXCLUDED.slug,
                            description = EXCLUDED.description,
                            category_id = EXCLUDED.category_id,
                            price = EXCLUDED.price,
                            original_price = EXCLUDED.original_price,
                            material = EXCLUDED.material,
                            color = EXCLUDED.color,
                            active = true,
                            stock_quantity = EXCLUDED.stock_quantity,
                            brand = EXCLUDED.brand,
                            updated_at = CURRENT_TIMESTAMP;


DELETE FROM product_images
WHERE product_id = '11111111-1111-4111-8111-111111111111';


INSERT INTO product_images
(product_id, image_url)
VALUES
    (
        '11111111-1111-4111-8111-111111111111',
        'https://images.pexels.com/photos/18289778/pexels-photo-18289778.jpeg?auto=compress&cs=tinysrgb&w=1200'
    ),
    (
        '11111111-1111-4111-8111-111111111111',
        'https://images.pexels.com/photos/7243801/pexels-photo-7243801.jpeg?auto=compress&cs=tinysrgb&w=1200'
    ),
    (
        '11111111-1111-4111-8111-111111111111',
        'https://images.pexels.com/photos/7607331/pexels-photo-7607331.jpeg?auto=compress&cs=tinysrgb&w=1200'
    ),
    (
        '11111111-1111-4111-8111-111111111111',
        'https://images.pexels.com/photos/7243801/pexels-photo-7243801.jpeg?auto=compress&cs=tinysrgb&w=1200'
    );
COMMIT;
