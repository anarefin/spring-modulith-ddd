-- Create schemas for each module
CREATE SCHEMA IF NOT EXISTS product_schema;
CREATE SCHEMA IF NOT EXISTS order_schema;
CREATE SCHEMA IF NOT EXISTS payment_schema;

-- Grant privileges
GRANT ALL PRIVILEGES ON SCHEMA product_schema TO admin;
GRANT ALL PRIVILEGES ON SCHEMA order_schema TO admin;
GRANT ALL PRIVILEGES ON SCHEMA payment_schema TO admin;

