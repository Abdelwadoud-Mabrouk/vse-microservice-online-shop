-- ============================================================
-- Flyway Migration: V2__seed_furniture_products.sql
-- Möbel-Shop Seed-Daten: Kategorien + Produkte + Bilder
-- Passend zu: Product Entity + Category Entity + product_images
-- ============================================================



-- SOFAS (15 Produkte)
INSERT INTO products (id, sku, name, slug, description, category_id, price, original_price, width_cm, height_cm, depth_cm, weight_kg, material, color, active, version, created_at, updated_at) VALUES
                                                                                                                                                                                                    ('11111111-1111-4111-8111-111111111111','SKU-CHAIR-OAK','Test_1','slug_Test_1','Stuhl','b1000000-0000-0000-0000-000000000009',99.00,1599.00,220.0,85.0,95.0,68.0,'Samt','Anthrazit',true,0,NOW(),NOW()),
                                                                                                                                                                                                    ('33333333-3333-4333-8333-333333333333','SKU-LAMP-LUNA','Test_2"','slug_Test_2','Lampe','a1000000-0000-0000-0000-000000000001',50.00,100.00,220.0,85.0,95.0,68.0,'Samt','Anthrazit',true,0,NOW(),NOW()),
                                                                                                                                                                                                    ('22222222-2222-4222-8222-222222222222','SKU-DESK-OSLO','Test_3','slug_Test_3','Lna.','a1000000-0000-0000-0000-000000000001',20.00,90.00,220.0,85.0,95.0,68.0,'Samt','Anthrazit',true,0,NOW(),NOW());


-- =====================================
-- PRODUCT IMAGES
-- =====================================

INSERT INTO product_images (product_id, image_url) VALUES

-- Test_1 Chair
('11111111-1111-4111-8111-111111111111',
 'https://images.unsplash.com/photo-1503602642458-232111445657?w=800'),

('11111111-1111-4111-8111-111111111111',
 'https://images.unsplash.com/photo-1598300042247-d088f8ab3a91?w=800'),


-- Test_2 Lamp
('33333333-3333-4333-8333-333333333333',
 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800'),

('33333333-3333-4333-8333-333333333333',
 'https://images.unsplash.com/photo-1540932239986-30128078f3c5?w=800'),


-- Product 22222222
('22222222-2222-4222-8222-222222222222',
 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800'),

('22222222-2222-4222-8222-222222222222',
 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=800'),


-- Couchtisch c0000000-0000-0000-0000-000000000024
('c0000000-0000-0000-0000-000000000024',
 'https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=800'),

('c0000000-0000-0000-0000-000000000024',
 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800'),

('c0000000-0000-0000-0000-000000000024',
 'https://images.unsplash.com/photo-1519710164239-da123dc03ef4?w=800'),


-- Couchtisch c0000000-0000-0000-0000-000000000022
('c0000000-0000-0000-0000-000000000022',
 'https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=800'),

('c0000000-0000-0000-0000-000000000022',
 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800'),

('c0000000-0000-0000-0000-000000000022',
 'https://images.unsplash.com/photo-1519710164239-da123dc03ef4?w=800'),


-- Bett c0000000-0000-0000-0000-000000000054
('c0000000-0000-0000-0000-000000000054',
 'https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=800'),

('c0000000-0000-0000-0000-000000000054',
 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800'),

('c0000000-0000-0000-0000-000000000054',
 'https://images.unsplash.com/photo-1519710164239-da123dc03ef4?w=800');