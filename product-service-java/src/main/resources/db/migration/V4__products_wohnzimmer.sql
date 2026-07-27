

-- ============================================================
-- 3. PRODUKTE
-- ============================================================

-- ============================================================
--
-- Wohnzimmer Produkte
--
-- Kategorien:
--
-- Sofas
-- b1000000-0000-0000-0000-000000000001
--
-- Couchtische
-- b1000000-0000-0000-0000-000000000002
--
-- TV-Möbel
-- b1000000-0000-0000-0000-000000000003
--
-- Regale
-- b1000000-0000-0000-0000-000000000004
--
-- ============================================================


INSERT INTO products
(id,
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
 brand)
VALUES


-- ============================================================
-- SOFAS (10)
-- ============================================================


('c0000000-0000-0000-0000-000000000001',
 'SOFA-SAMT-ANTH-3S',
 'Samt Sofa Anthrazit 3-Sitzer',
 'samt-sofa-anthrazit-3-sitzer',
 'Modernes 3-Sitzer Sofa mit hochwertigem Samtbezug und komfortabler Polsterung.',
 'b1000000-0000-0000-0000-000000000001',
 1299.00,
 1599.00,
 220,
 85,
 95,
 68,
 'Samt',
 'Anthrazit',
 true,
 0,
 NOW(),
 NOW(),
 30,
 'NordLiving'),


('c0000000-0000-0000-0000-000000000002',
 'LDR-SOFA-COGNAC-2S',
 'Leder Sofa Cognac 2-Sitzer',
 'leder-sofa-cognac-2-sitzer',
 'Klassisches Ledersofa aus hochwertigem Leder mit zeitlosem Design.',
 'b1000000-0000-0000-0000-000000000001',
 1899.00,
 2199.00,
 180,
 82,
 92,
 75,
 'Echtleder',
 'Cognac',
 true,
 0,
 NOW(),
 NOW(),
 30,
 'UrbanHome'),


('c0000000-0000-0000-0000-000000000003',
 'ECKSOFA-GRAU-MODERN',
 'Ecksofa Grau Modern',
 'ecksofa-grau-modern',
 'Großzügiges Ecksofa für moderne Wohnzimmer mit weicher Sitzfläche.',
 'b1000000-0000-0000-0000-000000000001',
 1599.00,
 1899.00,
 280,
 90,
 180,
 110,
 'Stoff',
 'Grau',
 true,
 0,
 NOW(),
 NOW(),
 30,
 'CasaDesign'),


('c0000000-0000-0000-0000-000000000004',
 'SOFA-BEIGE-LOUNGE',
 'Lounge Sofa Beige',
 'lounge-sofa-beige',
 'Komfortables Lounge Sofa mit elegantem Bezug und tiefem Sitzkomfort.',
 'b1000000-0000-0000-0000-000000000001',
 1499.00,
 1799.00,
 240,
 88,
 100,
 85,
 'Leinenstoff',
 'Beige',
 true,
 0,
 NOW(),
 NOW(),
 30,
 'NordLiving'),


('c0000000-0000-0000-0000-000000000005',
 'SOFA-CHESTER-BRAUN',
 'Chesterfield Sofa Braun',
 'chesterfield-sofa-braun',
 'Klassisches Chesterfield Sofa mit dekorativer Knopfheftung.',
 'b1000000-0000-0000-0000-000000000001',
 2299.00,
 2599.00,
 230,
 85,
 100,
 95,
 'Leder',
 'Braun',
 true,
 0,
 NOW(),
 NOW(),
 30,
 'RoyalFurniture'),


('c0000000-0000-0000-0000-000000000006',
 'SOFA-MODUL-SCHWARZ',
 'Modulares Sofa Schwarz',
 'modulares-sofa-schwarz',
 'Flexibles Modulsystem Sofa für individuelle Wohnlandschaften.',
 'b1000000-0000-0000-0000-000000000001',
 1999.00,
 2399.00,
 300,
 90,
 200,
 120,
 'Samt',
 'Schwarz',
 true,
 0,
 NOW(),
 NOW(),
 30,
 'ModernLiving'),


('c0000000-0000-0000-0000-000000000007',
 'SOFA-SCANDI-WEISS',
 'Scandi Sofa Weiß',
 'scandi-sofa-weiss',
 'Skandinavisches Sofa mit schlichtem Design und hellen Farben.',
 'b1000000-0000-0000-0000-000000000001',
 1199.00,
 1499.00,
 210,
 85,
 90,
 65,
 'Baumwolle',
 'Weiß',
 true,
 0,
 NOW(),
 NOW(),
 30,
 'ScandiHome'),


('c0000000-0000-0000-0000-000000000008',
 'SOFA-GRUEN-VELVET',
 'Velvet Sofa Grün',
 'velvet-sofa-gruen',
 'Samt Sofa in elegantem Grün für moderne Wohnräume.',
 'b1000000-0000-0000-0000-000000000001',
 1399.00,
 1699.00,
 220,
 86,
 95,
 70,
 'Samt',
 'Grün',
 true,
 0,
 NOW(),
 NOW(),
 30,
 'CasaDesign'),


('c0000000-0000-0000-0000-000000000009',
 'SOFA-SCHLAF-BLAU',
 'Sofa Bett Blau',
 'sofa-bett-blau',
 'Praktisches Schlafsofa mit integriertem Bettmechanismus.',
 'b1000000-0000-0000-0000-000000000001',
 899.00,
 1099.00,
 200,
 85,
 95,
 80,
 'Stoff',
 'Blau',
 true,
 0,
 NOW(),
 NOW(),
 30,
 'UrbanHome'),


('c0000000-0000-0000-0000-000000000010',
 'SOFA-LUXUS-CREME',
 'Luxus Sofa Creme',
 'luxus-sofa-creme',
 'Hochwertiges Designer Sofa mit Premium Polsterung.',
 'b1000000-0000-0000-0000-000000000001',
 2799.00,
 3199.00,
 260,
 90,
 110,
 100,
 'Premium Stoff',
 'Creme',
 true,
 0,
 NOW(),
 NOW(),
 30,
 'LuxuryHome');


-- ============================================================
-- COUCHTISCHE (5)
-- category_id:
-- b1000000-0000-0000-0000-000000000002
-- ============================================================


INSERT INTO products
(id,
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
 brand)
VALUES ('c0000000-0000-0000-0000-000000000011',
        'TISCH-MARMOR-SCHWARZ',
        'Couchtisch Marmor Schwarz',
        'couchtisch-marmor-schwarz',
        'Luxuriöser Couchtisch mit schwarzer Marmorplatte und goldenem Metallgestell. Elegantes Design für moderne Wohnzimmer.',
        'b1000000-0000-0000-0000-000000000002',
        849.00,
        999.00,
        100,
        38,
        50,
        32,
        'Marmor/Metall',
        'Schwarz/Gold',
        true,
        0,
        NOW(),
        NOW(),
        12,
        'LuxuryHome'),


       ('c0000000-0000-0000-0000-000000000012',
        'TISCH-EICHE-NATUR',
        'Couchtisch Eiche Natur',
        'couchtisch-eiche-natur',
        'Moderner Couchtisch aus Massivholz mit natürlicher Holzstruktur und zeitlosem Design.',
        'b1000000-0000-0000-0000-000000000002',
        499.00,
        599.00,
        120,
        45,
        60,
        25,
        'Eiche Massivholz',
        'Natur',
        true,
        0,
        NOW(),
        NOW(),
        20,
        'NordLiving'),


       ('c0000000-0000-0000-0000-000000000013',
        'TISCH-GLAS-MODERN',
        'Glas Couchtisch Modern',
        'glas-couchtisch-modern',
        'Minimalistischer Couchtisch mit Sicherheitsglasplatte und stabilem Metallrahmen.',
        'b1000000-0000-0000-0000-000000000002',
        399.00,
        499.00,
        110,
        42,
        55,
        22,
        'Glas/Metall',
        'Transparent/Schwarz',
        true,
        0,
        NOW(),
        NOW(),
        15,
        'ModernLiving'),


       ('c0000000-0000-0000-0000-000000000014',
        'TISCH-INDUSTRIAL-METALL',
        'Industrial Couchtisch Metall',
        'industrial-couchtisch-metall',
        'Robuster Couchtisch im Industrial Style mit Metallrahmen und Holzplatte.',
        'b1000000-0000-0000-0000-000000000002',
        579.00,
        699.00,
        115,
        48,
        65,
        35,
        'Holz/Metall',
        'Braun/Schwarz',
        true,
        0,
        NOW(),
        NOW(),
        9,
        'UrbanHome'),


       ('c0000000-0000-0000-0000-000000000015',
        'TISCH-RUND-HOLZ',
        'Runder Couchtisch Holz',
        'runder-couchtisch-holz',
        'Runder Couchtisch aus hochwertigem Holz mit weichen Formen für gemütliche Wohnbereiche.',
        'b1000000-0000-0000-0000-000000000002',
        349.00,
        449.00,
        90,
        40,
        90,
        20,
        'Akazienholz',
        'Hellbraun',
        true,
        0,
        NOW(),
        NOW(),
        18,
        'ScandiHome');

-- ============================================================
-- TV-MÖBEL (5 Produkte)
-- category_id:
-- b1000000-0000-0000-0000-000000000003
-- ============================================================

INSERT INTO products
(id, sku, name, slug, description, category_id, price, original_price, width_cm, height_cm, depth_cm, weight_kg, material, color, active, version, created_at, updated_at)
VALUES

    ('c0000000-0000-0000-0000-000000000016',
     'TV-MOEBEL-EICHE-180',
     'TV-Möbel Eiche Natur 180cm',
     'tv-moebel-eiche-natur-180',
     'Modernes TV-Board aus Eichenholz mit zwei Schubladen und offenen Fächern für Mediengeräte.',
     'b1000000-0000-0000-0000-000000000003',
     699.00,
     799.00,
     180.0,
     55.0,
     42.0,
     45.0,
     'Eiche Massivholz',
     'Natur',
     true,
     0,
     NOW(),
     NOW()),

    ('c0000000-0000-0000-0000-000000000017',
     'TV-LOWBOARD-SCHWARZ',
     'TV Lowboard Schwarz Modern',
     'tv-lowboard-schwarz-modern',
     'Elegantes schwarzes TV-Lowboard mit minimalistischer Front und viel Stauraum.',
     'b1000000-0000-0000-0000-000000000003',
     549.00,
     649.00,
     160.0,
     50.0,
     40.0,
     38.0,
     'MDF/Metall',
     'Schwarz',
     true,
     0,
     NOW(),
     NOW()),

    ('c0000000-0000-0000-0000-000000000018',
     'TV-WAND-MODERN',
     'TV Wandkombination Modern',
     'tv-wandkombination-modern',
     'Komplette TV-Wand mit Hängeschränken und Beleuchtung für moderne Wohnzimmer.',
     'b1000000-0000-0000-0000-000000000003',
     1199.00,
     1499.00,
     240.0,
     190.0,
     45.0,
     85.0,
     'Holzwerkstoff',
     'Weiß/Eiche',
     true,
     0,
     NOW(),
     NOW()),

    ('c0000000-0000-0000-0000-000000000019',
     'TV-BOARD-INDUSTRIAL',
     'TV Board Industrial Style',
     'tv-board-industrial-style',
     'TV-Möbel im Industrial Design mit Metallrahmen und rustikaler Holzoptik.',
     'b1000000-0000-0000-0000-000000000003',
     629.00,
     749.00,
     170.0,
     60.0,
     45.0,
     55.0,
     'Holz/Metall',
     'Braun/Schwarz',
     true,
     0,
     NOW(),
     NOW()),

    ('c0000000-0000-0000-0000-000000000020',
     'TV-MOEBEL-WEISS',
     'TV Möbel Weiß Hochglanz',
     'tv-moebel-weiss-hochglanz',
     'Modernes TV-Möbel in Hochglanz-Weiß mit Soft-Close Türen und elegantem Design.',
     'b1000000-0000-0000-0000-000000000003',
     799.00,
     899.00,
     200.0,
     52.0,
     43.0,
     50.0,
     'Hochglanz MDF',
     'Weiß',
     true,
     0,
     NOW(),
     NOW());

-- ============================================================
-- REGALE (5 Produkte)
-- category_id:
-- b1000000-0000-0000-0000-000000000004
-- ============================================================

INSERT INTO products
(id, sku, name, slug, description, category_id, price, original_price, width_cm, height_cm, depth_cm, weight_kg, material, color, active, version, created_at, updated_at)
VALUES

    ('c0000000-0000-0000-0000-000000000021',
     'REGAL-EICHE-180',
     'Regal Eiche Natur 180cm',
     'regal-eiche-natur-180',
     'Hochwertiges Bücherregal aus Eichenholz mit fünf Ebenen für Bücher und Dekoration.',
     'b1000000-0000-0000-0000-000000000004',
     499.00,
     599.00,
     90.0,
     180.0,
     35.0,
     35.0,
     'Eiche Holz',
     'Natur',
     true,
     0,
     NOW(),
     NOW()),

    ('c0000000-0000-0000-0000-000000000022',
     'REGAL-METALL-SCHWARZ',
     'Metall Regal Schwarz',
     'metall-regal-schwarz',
     'Modernes Regal mit schwarzem Metallrahmen und Holzablagen im Industrial Style.',
     'b1000000-0000-0000-0000-000000000004',
     299.00,
     399.00,
     80.0,
     170.0,
     35.0,
     28.0,
     'Metall/Holz',
     'Schwarz/Braun',
     true,
     0,
     NOW(),
     NOW()),

    ('c0000000-0000-0000-0000-000000000023',
     'REGAL-WAND-SYSTEM',
     'Wandregal System Modern',
     'wandregal-system-modern',
     'Flexibles Wandregal-System für Wohnzimmer mit verschiedenen Ablageflächen.',
     'b1000000-0000-0000-0000-000000000004',
     349.00,
     449.00,
     120.0,
     80.0,
     25.0,
     18.0,
     'MDF',
     'Weiß',
     true,
     0,
     NOW(),
     NOW()),

    ('c0000000-0000-0000-0000-000000000024',
     'REGAL-LOFT-HOLZ',
     'Loft Regal Holz Metall',
     'loft-regal-holz-metall',
     'Großes Loft-Regal mit stabiler Konstruktion für Bücher, Pflanzen und Dekoration.',
     'b1000000-0000-0000-0000-000000000004',
     579.00,
     699.00,
     100.0,
     200.0,
     40.0,
     45.0,
     'Akazienholz/Metall',
     'Braun/Schwarz',
     true,
     0,
     NOW(),
     NOW()),

    ('c0000000-0000-0000-0000-000000000025',
     'REGAL-ECKE-MODERN',
     'Eckregal Modern',
     'eckregal-modern',
     'Platzsparendes Eckregal für moderne Wohnbereiche mit mehreren Ebenen.',
     'b1000000-0000-0000-0000-000000000004',
     249.00,
     299.00,
     70.0,
     160.0,
     30.0,
     22.0,
     'Holzwerkstoff',
     'Eiche Grau',
     true,
     0,
     NOW(),
     NOW());


-- ============================================================
-- PRODUCT IMAGES
-- ============================================================


-- ============================================================
-- PRODUCT IMAGES
-- Generated automatically
-- ============================================================


INSERT INTO product_images
(product_id, image_url)
VALUES

    (
        'c0000000-0000-0000-0000-000000000001',
        'images/wohnzimmer/Sofas/guzman-barquin-FkKClUPUURU-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000001',
        'images/wohnzimmer/Sofas/kam-idris-AUOuawx5B0M-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000001',
        'images/wohnzimmer/Sofas/prydumano-design-W86O2hU317U-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000002',
        'images/wohnzimmer/Sofas/prydumano-design-yOx4oPNUR10-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000002',
        'images/wohnzimmer/Sofas/trend-K9pU2u0Z5WU-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000002',
        'images/wohnzimmer/Sofas/inside-weather-2EUdAWQ3Uko-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000003',
        'images/wohnzimmer/Sofas/julia-_9gxlIXdqDQ-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000003',
        'images/wohnzimmer/Sofas/marlon-corona-1tMc27CFUbA-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000003',
        'images/wohnzimmer/Sofas/sven-brandsma-jxbdO7YXL8Y-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000004',
        'images/wohnzimmer/Sofas/syahrin-seth-CgjBaRKroEY-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000004',
        'images/wohnzimmer/Sofas/hadi-yazdi-aznaveh-FoTZazW3WxY-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000004',
        'images/wohnzimmer/Sofas/katsia-jazwinska-zSgzGRdJmMY-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000005',
        'images/wohnzimmer/Sofas/keith-tanner-3l2UKximisM-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000005',
        'images/wohnzimmer/Sofas/roberto-nickson-rEJxpBskj3Q-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000005',
        'images/wohnzimmer/Sofas/volant-zRXbV0XhlN8-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000006',
        'images/wohnzimmer/Sofas/guzman-barquin-FkKClUPUURU-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000006',
        'images/wohnzimmer/Sofas/kam-idris-AUOuawx5B0M-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000006',
        'images/wohnzimmer/Sofas/prydumano-design-W86O2hU317U-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000007',
        'images/wohnzimmer/Sofas/prydumano-design-yOx4oPNUR10-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000007',
        'images/wohnzimmer/Sofas/trend-K9pU2u0Z5WU-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000007',
        'images/wohnzimmer/Sofas/inside-weather-2EUdAWQ3Uko-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000008',
        'images/wohnzimmer/Sofas/julia-_9gxlIXdqDQ-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000008',
        'images/wohnzimmer/Sofas/marlon-corona-1tMc27CFUbA-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000008',
        'images/wohnzimmer/Sofas/sven-brandsma-jxbdO7YXL8Y-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000009',
        'images/wohnzimmer/Sofas/syahrin-seth-CgjBaRKroEY-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000009',
        'images/wohnzimmer/Sofas/hadi-yazdi-aznaveh-FoTZazW3WxY-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000009',
        'images/wohnzimmer/Sofas/katsia-jazwinska-zSgzGRdJmMY-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000010',
        'images/wohnzimmer/Sofas/keith-tanner-3l2UKximisM-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000010',
        'images/wohnzimmer/Sofas/roberto-nickson-rEJxpBskj3Q-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000010',
        'images/wohnzimmer/Sofas/volant-zRXbV0XhlN8-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000011',
        'images/wohnzimmer/Couchtische/dada_design-o5GiFSl5iYw-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000011',
        'images/wohnzimmer/Couchtische/rizky-subagja-1k7TnX5GAww-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000011',
        'images/wohnzimmer/Couchtische/tabitha-turner-rhcllVy2zBU-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000012',
        'images/wohnzimmer/Couchtische/yolk-coworking-krakow-l_brMXgpJhk-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000012',
        'images/wohnzimmer/Couchtische/lui-peng-8NxTrV6i4WQ-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000012',
        'images/wohnzimmer/Couchtische/snapbythree-my-g6e641CiHFQ-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000013',
        'images/wohnzimmer/Couchtische/tabitha-turner-rhcllVy2zBU-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000013',
        'images/wohnzimmer/Couchtische/guven-gunes-Iy3GN4iPLW8-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000013',
        'images/wohnzimmer/Couchtische/rizky-subagja-UavcyKcVwMY-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000014',
        'images/wohnzimmer/Couchtische/snapbythree-my-g6e641CiHFQ-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000014',
        'images/wohnzimmer/Couchtische/eugenia-pan-kiv-1JJJIHh7-Mk-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000014',
        'images/wohnzimmer/Couchtische/rizky-subagja-IBhJivtPqHQ-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000015',
        'images/wohnzimmer/Couchtische/rizky-subagja-UavcyKcVwMY-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000015',
        'images/wohnzimmer/Couchtische/dada_design-o5GiFSl5iYw-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000015',
        'images/wohnzimmer/Couchtische/rizky-subagja-1k7TnX5GAww-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000016',
        'images/wohnzimmer/TV-Möbel/dario-KzGhmrQmB6I-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000016',
        'images/wohnzimmer/TV-Möbel/jalg-tv-stand-Ixk2yO8D1aI-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000016',
        'images/wohnzimmer/TV-Möbel/jonas-leupe-XE6PBDd7_FQ-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000017',
        'images/wohnzimmer/TV-Möbel/marques-kaspbrak-n1amn-SHKzw-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000017',
        'images/wohnzimmer/TV-Möbel/david-svihovec-ZG-eiD3Ep_Q-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000017',
        'images/wohnzimmer/TV-Möbel/jalg-tv-stand-JIZJa-ZCqGY-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000018',
        'images/wohnzimmer/TV-Möbel/jalg-tv-stand-ipF8WKs3iZI-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000018',
        'images/wohnzimmer/TV-Möbel/samuel-regan-asante-KBRRSow33qA-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000018',
        'images/wohnzimmer/TV-Möbel/imitat-_GlgV6pOUAY-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000019',
        'images/wohnzimmer/TV-Möbel/jalg-tv-stand--j3QQaJ-Ygk-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000019',
        'images/wohnzimmer/TV-Möbel/jens-kreuter-ngMtsE5r9eI-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000019',
        'images/wohnzimmer/TV-Möbel/stephen-monterroso-cwcPV_VKfy4-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000020',
        'images/wohnzimmer/TV-Möbel/dario-KzGhmrQmB6I-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000020',
        'images/wohnzimmer/TV-Möbel/jalg-tv-stand-Ixk2yO8D1aI-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000020',
        'images/wohnzimmer/TV-Möbel/jonas-leupe-XE6PBDd7_FQ-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000021',
        'images/wohnzimmer/Regale/connor-home-djJoOgDRvzU-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000021',
        'images/wohnzimmer/Regale/emi-ymerai-3DytzH8rf-U-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000021',
        'images/wohnzimmer/Regale/marcus-loke-8APvVNW5srI-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000022',
        'images/wohnzimmer/Regale/natural-goods-berlin-L6NP4ZGnpSY-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000022',
        'images/wohnzimmer/Regale/darren-richardson-q23lcKhJYCE-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000022',
        'images/wohnzimmer/Regale/jessica-arends-yzPWspWSDm0-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000023',
        'images/wohnzimmer/Regale/luisa-brimble-qnWMnsDwdiw-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000023',
        'images/wohnzimmer/Regale/connor-home-djJoOgDRvzU-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000023',
        'images/wohnzimmer/Regale/emi-ymerai-3DytzH8rf-U-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000024',
        'images/wohnzimmer/Regale/jason-leung-FRrIZnkS13g-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000024',
        'images/wohnzimmer/Regale/natural-goods-berlin-L6NP4ZGnpSY-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000024',
        'images/wohnzimmer/Regale/darren-richardson-q23lcKhJYCE-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000025',
        'images/wohnzimmer/Regale/darren-richardson-wMaUWhnah7E-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000025',
        'images/wohnzimmer/Regale/luisa-brimble-qnWMnsDwdiw-unsplash.jpg'
    ),
    (
        'c0000000-0000-0000-0000-000000000025',
        'images/wohnzimmer/Regale/connor-home-djJoOgDRvzU-unsplash.jpg'
    )

;


-- ============================================================
-- Für Kaufen
-- ============================================================


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
        'b1000000-0000-0000-0000-000000000009',
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
        NOW(),
        NOW(),
        30,
        'NordLiving'
    );

INSERT INTO product_images
(product_id, image_url)
VALUES

    (
        '11111111-1111-4111-8111-111111111111',
        'images/Arbeitszimmer/Buerostuehle/eugene-chystiakov-3neSwyntbQ8-unsplash.jpg'
    ),

    (
        '11111111-1111-4111-8111-111111111111',
        'images/Arbeitszimmer/Buerostuehle/eugenia-pan-kiv-1JJJIHh7-Mk-unsplash.jpg'
    ),

    (
        '11111111-1111-4111-8111-111111111111',
        'images/Arbeitszimmer/Buerostuehle/kari-shea-AMyjxxLEHU4-unsplash.jpg'
    ),

    (
        '11111111-1111-4111-8111-111111111111',
        'images/Arbeitszimmer/Buerostuehle/kelly-miller-BL6XQLZeXpg-unsplash.jpg'
    );


