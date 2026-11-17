-- Development-only seed data. Run manually after V1__create_product_catalog.sql.
INSERT INTO categories (id, name, description)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'Phones', 'Mobile phones'),
    ('00000000-0000-0000-0000-000000000002', 'Laptops', 'Portable computers'),
    ('00000000-0000-0000-0000-000000000003', 'Accessories', 'Device accessories');

INSERT INTO products
    (id, name, description, category_id, brand, price, currency, stock_quantity, status)
VALUES
    ('00000000-0000-0000-0000-000000000011', 'Phone X', 'Development phone',
     '00000000-0000-0000-0000-000000000001', 'Example', 25000.00, 'BDT', 40, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000012', 'Laptop Pro', 'Development laptop',
     '00000000-0000-0000-0000-000000000002', 'Example', 85000.00, 'BDT', 10, 'ACTIVE');
