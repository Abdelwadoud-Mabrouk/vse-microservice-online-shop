-- Kategorien
CREATE INDEX idx_categories_parent_id
    ON categories(parent_id);


-- Produkte Landing
CREATE INDEX idx_products_active_name
    ON products(active, name);


-- Kategorie Filter
CREATE INDEX idx_products_category_active
    ON products(category_id, active);


-- Preis Filter
CREATE INDEX idx_products_price_active
    ON products(price, active);


-- Farbe
CREATE INDEX idx_products_color_active
    ON products(color, active);


-- Material
CREATE INDEX idx_products_material_active
    ON products(material, active);


-- Suche
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_products_name_trgm
    ON products
    USING gin(name gin_trgm_ops);

CREATE INDEX idx_products_description_trgm
    ON products
    USING gin(description gin_trgm_ops);