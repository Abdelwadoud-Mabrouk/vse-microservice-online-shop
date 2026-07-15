-- ============================================================
-- Flyway Migration: V1__create_tables.sql (KORRIGIERT)
-- ============================================================

-- 1. Tabelle für die Kategorien (inklusive hierarchischer Parent-Beziehung!)
CREATE TABLE categories (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            name VARCHAR(255) NOT NULL,
                            slug VARCHAR(255) NOT NULL UNIQUE,
                            image_url VARCHAR(255),
                            parent_id UUID,
                            active BOOLEAN DEFAULT TRUE NOT NULL,
                            created_at TIMESTAMP,
                            updated_at TIMESTAMP,

                            CONSTRAINT fk_category_parent
                                FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE INDEX idx_categories_parent ON categories(parent_id);


-- 2. Tabelle für die Produkte (Bleibt gleich, verweist auf categories)
CREATE TABLE products (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          sku VARCHAR(255) NOT NULL UNIQUE,
                          name VARCHAR(255) NOT NULL,
                          slug VARCHAR(255) NOT NULL UNIQUE,
                          description TEXT,
                          category_id UUID NOT NULL,
                          price DECIMAL(10, 2) NOT NULL,
                          original_price DECIMAL(10, 2),
                          width_cm DOUBLE PRECISION,
                          height_cm DOUBLE PRECISION,
                          depth_cm DOUBLE PRECISION,
                          weight_kg DOUBLE PRECISION,
                          material VARCHAR(255),
                          color VARCHAR(255),
                          active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                          version BIGINT NOT NULL DEFAULT 0,

                          CONSTRAINT fk_product_category FOREIGN KEY (category_id)
                              REFERENCES categories (id) ON DELETE RESTRICT
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_slug ON products(slug);


-- 3. Hilfstabelle für die @ElementCollection (Bilder-Liste der Produkte)
CREATE TABLE product_images (
                                product_id UUID NOT NULL,
                                image_url VARCHAR(512) NOT NULL,

                                CONSTRAINT fk_image_product FOREIGN KEY (product_id)
                                    REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_images_product ON product_images(product_id);