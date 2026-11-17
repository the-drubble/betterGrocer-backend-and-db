CREATE TABLE IF NOT EXISTS barcode_name (
    barcode TEXT PRIMARY KEY,
    product_name TEXT,
    img TEXT
);

CREATE TABLE IF NOT EXISTS barcode_foodstuff_sku (
    internal_sku TEXT PRIMARY KEY,
    barcode TEXT
);

CREATE TABLE IF NOT EXISTS ww_dep_id (
    dep_id SMALLINT PRIMARY KEY,
    dep_name TEXT
);

CREATE TABLE IF NOT EXISTS fs_dep_id (
    dep_id SMALLINT PRIMARY KEY,
    dep_name TEXT
);

CREATE TABLE IF NOT EXISTS nw_store_id_store_name_mapping (
    store_id TEXT PRIMARY KEY,
    store_name TEXT,
    store_address TEXT,
    store_island_key VARCHAR(2)
);

CREATE TABLE IF NOT EXISTS ps_store_id_store_name_mapping (
    store_id TEXT PRIMARY KEY,
    store_name TEXT,
    store_address TEXT,
    store_island_key VARCHAR(2)
);

CREATE TABLE IF NOT EXISTS ww_store_id_store_name_mapping (
    store_id TEXT PRIMARY KEY,
    store_name TEXT,
    store_address TEXT
);

CREATE TABLE IF NOT EXISTS woolworths (
    store_id TEXT NOT NULL,
    barcode TEXT NOT NULL,
    price INT,
    save_price INT,
    member_price INT,
    price_per_unit TEXT,
    stock_level BOOLEAN,
    dep_id SMALLINT,

    PRIMARY KEY (store_id, barcode),
    FOREIGN KEY (store_id) REFERENCES ww_store_id_store_name_mapping(store_id),
    FOREIGN KEY (barcode) REFERENCES barcode_name(barcode),
    FOREIGN KEY (dep_id) REFERENCES ww_dep_id(dep_id)
);

CREATE TABLE IF NOT EXISTS paknsave (
    store_id TEXT NOT NULL,
    internal_sku TEXT NOT NULL,
    price INT,
    price_per_unit TEXT,
    stock_level BOOLEAN,
    dep_id SMALLINT,

    PRIMARY KEY (store_id, internal_sku),
    FOREIGN KEY (store_id) REFERENCES ps_store_id_store_name_mapping(store_id),
    FOREIGN KEY (internal_sku) REFERENCES barcode_foodstuff_sku(internal_sku),
    FOREIGN KEY (dep_id) REFERENCES fs_dep_id(dep_id)

);

CREATE TABLE IF NOT EXISTS newworld (
    store_id TEXT NOT NULL,
    internal_sku TEXT NOT NULL,
    price INT,
    price_per_unit TEXT,
    stock_level BOOLEAN,
    dep_id SMALLINT,

    PRIMARY KEY (store_id, internal_sku),
    FOREIGN KEY (store_id) REFERENCES nw_store_id_store_name_mapping(store_id),
    FOREIGN KEY (internal_sku) REFERENCES barcode_foodstuff_sku(internal_sku),
    FOREIGN KEY (dep_id) REFERENCES fs_dep_id(dep_id)
);

