
CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'USER'
);

CREATE TABLE IF NOT EXISTS profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    profile_name TEXT NOT NULL,
    pin TEXT,
    CONSTRAINT unique_profile_name_per_user UNIQUE (user_id, profile_name)
);

CREATE TABLE IF NOT EXISTS stores (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    franchiseName TEXT NOT NULL,
    location TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
    barcode TEXT PRIMARY KEY,
    display_name TEXT
);

CREATE TABLE IF NOT EXISTS store_product_prices (
    store_id TEXT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_reference TEXT NOT NULL REFERENCES products(barcode) ON DELETE CASCADE,
    price_cents INT NOT NULL CHECK (price_cents >=0),
    in_stock BOOLEAN,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (store_id, product_reference)
);

-- profile_id = the profile that created (owns) the list
-- is_shared  = visible to and editable by every profile with the same user_id
CREATE TABLE IF NOT EXISTS shopping_lists (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    profile_id BIGINT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_shared BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT unique_profile_shopping_list_name UNIQUE (profile_id, name)
);

CREATE INDEX IF NOT EXISTS idx_shopping_lists_user_shared
    ON shopping_lists (user_id)
    WHERE is_shared;


CREATE TABLE IF NOT EXISTS profile_stores (
    profile_id BIGINT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    store_id TEXT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    PRIMARY KEY (profile_id, store_id)
);

CREATE TABLE IF NOT EXISTS shopping_list_item (
    id BIGSERIAL PRIMARY KEY,

    shopping_list_id BIGINT NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
    product_reference TEXT NOT NULL REFERENCES products(barcode) ON DELETE RESTRICT,
    quantity SMALLINT NOT NULL,

    CONSTRAINT quantity_quantity CHECK (quantity >= 1),
    CONSTRAINT unique_product_per_list UNIQUE (shopping_list_id, product_reference)
);


