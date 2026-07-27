-- ============================================================
-- Flyway Migration: V2__seed_furniture_products.sql
-- Möbel-Shop Seed-Daten: Kategorien
-- Passend zu: Product Entity + Category Entity + product_images
-- ============================================================

-- ============================================================
-- 1. HAUPTKATEGORIEN
-- ============================================================
INSERT INTO categories (id, name, slug, parent_id, image_url, active, created_at, updated_at)
VALUES ('a1000000-0000-0000-0000-000000000001', 'Wohnzimmer', 'wohnzimmer', NULL,
        'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800', true, NOW(), NOW()),
       ('a1000000-0000-0000-0000-000000000002', 'Schlafzimmer', 'schlafzimmer', NULL,
        'https://images.unsplash.com/photo-1505693314120-0d443867891c?w=800', true, NOW(), NOW()),
       ('a1000000-0000-0000-0000-000000000003', 'Esszimmer', 'esszimmer', NULL,
        'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800', true, NOW(), NOW()),
       ('a1000000-0000-0000-0000-000000000004', 'Arbeitszimmer', 'arbeitszimmer', NULL,
        'https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=800', true, NOW(), NOW()),
       ('a1000000-0000-0000-0000-000000000005', 'Outdoor', 'outdoor', NULL,
        'https://images.unsplash.com/photo-1600210492493-0946911123ea?w=800', true, NOW(), NOW());

-- ============================================================
-- 2. UNTERKATEGORIEN
-- ============================================================
INSERT INTO categories (id, name, slug, parent_id, image_url, active, created_at, updated_at)
VALUES
-- Wohnzimmer
('b1000000-0000-0000-0000-000000000001', 'Sofas', 'sofas', 'a1000000-0000-0000-0000-000000000001',
 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=400', true, NOW(), NOW()),
('b1000000-0000-0000-0000-000000000002', 'Couchtische', 'couchtische', 'a1000000-0000-0000-0000-000000000001',
 'https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=400', true, NOW(), NOW()),
('b1000000-0000-0000-0000-000000000003', 'TV-Möbel', 'tv-moebel', 'a1000000-0000-0000-0000-000000000001',
 'https://images.unsplash.com/photo-1593642632502-23f8e91ef17b?w=400', true, NOW(), NOW()),
('b1000000-0000-0000-0000-000000000004', 'Regale', 'regale', 'a1000000-0000-0000-0000-000000000001',
 'https://images.unsplash.com/photo-1594026112284-02bb6f3352fe?w=400', true, NOW(), NOW()),
-- Schlafzimmer
('b1000000-0000-0000-0000-000000000005', 'Betten', 'betten', 'a1000000-0000-0000-0000-000000000002',
 'https://images.unsplash.com/photo-1505693314120-0d443867891c?w=400', true, NOW(), NOW()),
('b1000000-0000-0000-0000-000000000006', 'Kleiderschränke', 'kleiderschraenke', 'a1000000-0000-0000-0000-000000000002',
 'https://images.unsplash.com/photo-1558997519-83ea9252edf8?w=400', true, NOW(), NOW()),
('b1000000-0000-0000-0000-000000000007', 'Nachttische', 'nachttische', 'a1000000-0000-0000-0000-000000000002',
 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=400', true, NOW(), NOW()),
-- Esszimmer
('b1000000-0000-0000-0000-000000000008', 'Esstische', 'esstische', 'a1000000-0000-0000-0000-000000000003',
 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=400', true, NOW(), NOW()),
('b1000000-0000-0000-0000-000000000009', 'Essstühle', 'esstuehle', 'a1000000-0000-0000-0000-000000000003',
 'https://images.unsplash.com/photo-1506439773649-6e0eb8cfb237?w=400', true, NOW(), NOW()),
('b1000000-0000-0000-0000-000000000010', 'Sideboards', 'sideboards', 'a1000000-0000-0000-0000-000000000003',
 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=400', true, NOW(), NOW()),
-- Arbeitszimmer
('b1000000-0000-0000-0000-000000000011', 'Schreibtische', 'schreibtische', 'a1000000-0000-0000-0000-000000000004',
 'https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=400', true, NOW(), NOW()),
('b1000000-0000-0000-0000-000000000012', 'Bürostühle', 'buerostuehle', 'a1000000-0000-0000-0000-000000000004',
 'https://images.unsplash.com/photo-1598300042247-d088f8ab3a91?w=400', true, NOW(), NOW()),
('b1000000-0000-0000-0000-000000000013', 'Bücherregale', 'buecherregale', 'a1000000-0000-0000-0000-000000000004',
 'https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=400', true, NOW(), NOW()),
-- Outdoor
('b1000000-0000-0000-0000-000000000014', 'Gartenmöbel', 'gartenmoebel', 'a1000000-0000-0000-0000-000000000005',
 'https://images.unsplash.com/photo-1600210492493-0946911123ea?w=400', true, NOW(), NOW()),
('b1000000-0000-0000-0000-000000000015', 'Balkonmöbel', 'balkonmoebel', 'a1000000-0000-0000-0000-000000000005',
 'https://images.unsplash.com/photo-1600607687644-c7171b42498b?w=400', true, NOW(), NOW());