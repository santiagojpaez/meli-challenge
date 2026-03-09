INSERT INTO unit_groups (id, name) VALUES
(1, 'digital_storage'),
(2, 'display_size'),
(3, 'battery_mah'),
(4, 'volume'),
(5, 'pressure'),
(6, 'weight'),
(7, 'dimension'),
(8, 'currency');          

INSERT INTO units (id, unit_group_id, symbol, conversion_factor, is_base_unit) VALUES
(1, 1, 'GB', 1.0, true),            -- ug: digital_storage
(2, 1, 'MB', 0.0009765625, false),  -- ug: digital_storage
(3, 1, 'TB', 1024.0, false),        -- ug: digital_storage
(4, 2, 'in', 1.0, true),            -- ug: display_size
(5, 2, 'cm', 0.393700787, false),   -- ug: display_size
(6, 3, 'mAh', 1.0, true),           -- ug: battery_mah
(7, 4, 'L', 1.0, true),             -- ug: volume
(8, 4, 'ml', 0.001, false),         -- ug: volume
(9, 5, 'bar', 1.0, true),           -- ug: pressure
(10, 6, 'g', 1.0, true),            -- ug: weight
(11, 6, 'kg', 1000.0, false),       -- ug: weight
(12, 7, 'mm', 1.0, true),           -- ug: dimension
(13, 7, 'cm', 0.1, false),          -- ug: dimension
(14, 7, 'in', 0.393700787, false),  -- ug: dimension
(15, 8, 'ARS', 1.0, true);          -- ug: currency


INSERT INTO categories (id, name, parent_id) VALUES
(1, 'Tecnología', NULL),        -- parent: (raíz)
(2, 'Smartphones', 1),          -- parent: Tecnología
(3, 'Electrodomésticos', NULL), -- parent: (raíz)
(4, 'Cafeteras', 3),            -- parent: Electrodomésticos
(5, 'Microcontroladores', 1),   -- parent: Tecnología
(6, 'Laptops', 1),              -- parent: Tecnología
(7, 'Tablets', 1);              -- parent: Tecnología


INSERT INTO prices (id, amount, original_amount, currency) VALUES
(1, 2199999.00, NULL, 'ARS'),
(2, 850, 900, 'USD'),
(3, 1799999.00, 1999999.00, 'ARS'),
(4, 2600, 2700, 'BRL'),
(5, 85000.00, 95000.00, 'ARS'),
(6, 250000.00, 280000.00, 'ARS'),
(7, 1250000.00, NULL, 'ARS'),
(8, 380, 400, 'USD');


INSERT INTO shippings (id, free_shipping, store_pickup) VALUES
(1, true, false),
(2, false, true),
(3, true, false),
(4, true, true),
(5, true, true),
(6, true, true),
(7, true, true),
(8, true, false);


INSERT INTO attribute_definitions (id, canonical_name, display_name, description, data_type, unit_group_id, comparison_strategy, product_field) VALUES
(1, 'ram_memory', 'Memoria RAM', 'Capacidad de memoria RAM', 'NUMBER', 1, 'HIGHER_IS_BETTER', NULL),                            -- ug: digital_storage
(2, 'internal_storage', 'Almacenamiento interno', 'Capacidad de almacenamiento interno', 'NUMBER', 1, 'HIGHER_IS_BETTER', NULL),-- ug: digital_storage
(3, 'screen_size', 'Tamaño de pantalla', 'Diagonal de pantalla en pulgadas', 'NUMBER', 2, 'HIGHER_IS_BETTER', NULL),            -- ug: display_size
(4, 'battery_capacity', 'Capacidad de batería', 'Capacidad en mAh', 'NUMBER', 3, 'HIGHER_IS_BETTER', NULL),                     -- ug: battery_mah
(5, 'main_camera_mp', 'Cámara principal', 'Megapixeles de cámara principal', 'NUMBER', NULL, 'HIGHER_IS_BETTER', NULL),
(6, 'operating_system', 'Sistema operativo', 'OS del dispositivo', 'TEXT', NULL, 'NEUTRAL', NULL),
(7, 'connectivity', 'Conectividad', 'Red móvil soportada', 'TEXT', NULL, 'NEUTRAL', NULL),
(8, 'processor', 'Procesador', 'Modelo de procesador', 'TEXT', NULL, 'NEUTRAL', NULL),
(9, 'refresh_rate', 'Frecuencia de pantalla', 'Hz de refresco', 'NUMBER', NULL, 'HIGHER_IS_BETTER', NULL),
(10, 'height', 'Alto', 'Dimensión Alto del Producto', 'NUMBER', 7, 'NEUTRAL', NULL),                                    -- ug: dimension
(11, 'architecture', 'Arquitectura', 'Arquitectura de procesador', 'TEXT', NULL, 'NEUTRAL', NULL),
(12, 'water_capacity', 'Capacidad de agua', 'Depósito de agua', 'NUMBER', 4, 'HIGHER_IS_BETTER', NULL),                         -- ug: volume
(13, 'pressure', 'Presión', 'Presión de bomba', 'NUMBER', 5, 'NEUTRAL', NULL),                                                  -- ug: pressure
(14, 'screen_type', 'Tipo de pantalla', 'Tecnología de pantalla', 'TEXT', NULL, 'NEUTRAL', NULL),
(15, 'zoom_optical', 'Zoom óptico', 'Zoom óptico de cámara', 'TEXT', NULL, 'NEUTRAL', NULL),
(16, 'dual_sim', 'Dual SIM', 'Soporte dual SIM', 'BOOLEAN', NULL, 'HIGHER_IS_BETTER', NULL),
(17, 'nfc', 'NFC', 'Comunicación NFC', 'BOOLEAN', NULL, 'HIGHER_IS_BETTER', NULL),
(18, 'waterproof', 'Resistente al agua', 'Certificación IP', 'BOOLEAN', NULL, 'HIGHER_IS_BETTER', NULL),
(19, 'storage_expandable', 'Memoria expandible', 'Ranura microSD', 'BOOLEAN', NULL, 'HIGHER_IS_BETTER', NULL),
(20, 'fast_charging', 'Carga rápida', 'Soporta carga rápida', 'BOOLEAN', NULL, 'HIGHER_IS_BETTER', NULL),
(21, 'wireless_charging', 'Carga inalámbrica', 'Soporta carga inalámbrica', 'BOOLEAN', NULL, 'HIGHER_IS_BETTER', NULL),
(22, 'fingerprint_sensor', 'Lector de huella', 'Sensor de huella digital', 'BOOLEAN', NULL, 'HIGHER_IS_BETTER', NULL),
(23, 'face_unlock', 'Desbloqueo facial', 'Reconocimiento facial', 'BOOLEAN', NULL, 'HIGHER_IS_BETTER', NULL),
(24, 'cores', 'Núcleos', 'Cantidad de núcleos de CPU', 'NUMBER', NULL, 'HIGHER_IS_BETTER', NULL),
(25, 'model_name', 'Modelo', 'Nombre del modelo', 'TEXT', NULL, 'NEUTRAL', NULL),
(26, 'brand', 'Marca', 'Marca del producto', 'TEXT', NULL, 'NEUTRAL', NULL),
(27, 'width', 'Ancho', 'Dimensión Ancho del Producto', 'NUMBER', 7, 'NEUTRAL', NULL),                                 -- ug: dimension
(28, 'depth', 'Profundidad', 'Dimensión Profundidad del Producto', 'NUMBER', 7, 'NEUTRAL', NULL),                     -- ug: dimension
-- Virtual attributes: backed by Product entity fields, no ProductAttribute rows
(29, 'price', 'Precio', 'Precio actual del producto',                  'NUMBER',  8,    'LOWER_IS_BETTER',  'PRICE'), 
(30, 'discount', 'Descuento', 'Porcentaje de descuento sobre precio original', 'NUMBER', NULL, 'HIGHER_IS_BETTER', 'DISCOUNT'),
(31, 'free_shipping', 'Envío gratis', 'El producto tiene envío gratis', 'BOOLEAN', NULL, 'HIGHER_IS_BETTER', 'FREE_SHIPPING'),
(32, 'rating', 'Valoración', 'Puntuación promedio del producto', 'NUMBER',  NULL, 'HIGHER_IS_BETTER', 'RATING');


INSERT INTO attribute_groups (id, name) VALUES
(1, 'Características Principales'),
(2, 'Cámara'),
(3, 'Pantalla'),
(4, 'Conectividad'),
(5, 'Batería y Carga'),
(6, 'Diseño y Resistencia'),
(7, 'Especificaciones'),
(8, 'Dimensiones'),
(9, 'Otros'),
(10, 'Precio y Valoración');


INSERT INTO category_attribute_rules (id, category_id, attribute_def_id, attribute_group_id, is_required, is_comparable, display_order) VALUES
(1, 2, 25, 1, true, true, 0),       -- c: Smartphones, a: model_name, group: Características Principales
(2, 2, 26, 1, true, true, 1),       -- c: Smartphones, a: brand, group: Características Principales
(3, 2, 1, 1, true, true, 2),        -- c: Smartphones, a: ram_memory, group: Características Principales
(4, 2, 2, 1, true, true, 3),        -- c: Smartphones, a: internal_storage, group: Características Principales
(5, 2, 3, 3, true, true, 7),        -- c: Smartphones, a: screen_size, group: Pantalla
(6, 2, 4, 5, true, true, 8),        -- c: Smartphones, a: battery_capacity, group: Batería y Carga
(7, 2, 5, 2, true, true, 9),        -- c: Smartphones, a: main_camera_mp, group: Cámara
(8, 2, 6, 1, true, true, 10),        -- c: Smartphones, a: operating_system, group: Características Principales
(9, 2, 7, 4, false, true, 11),       -- c: Smartphones, a: connectivity, group: Conectividad
(10, 2, 8, 1, false, true, 12),      -- c: Smartphones, a: processor, group: Características Principales
(11, 2, 9, 3, false, true, 13),     -- c: Smartphones, a: refresh_rate, group: Pantalla
(12, 2, 14, 3, false, true, 14),    -- c: Smartphones, a: screen_type, group: Pantalla
(13, 2, 15, 2, false, true, 15),    -- c: Smartphones, a: zoom_optical, group: Cámara
(14, 2, 16, 4, false, true, 16),    -- c: Smartphones, a: dual_sim, group: Conectividad
(15, 2, 17, 4, false, true, 17),    -- c: Smartphones, a: nfc, group: Conectividad
(16, 2, 18, 6, false, true, 18),    -- c: Smartphones, a: waterproof, group: Diseño y Resistencia
(17, 2, 20, 5, false, true, 19),    -- c: Smartphones, a: fast_charging, group: Batería y Carga
(60, 2, 19, 1, false, true, 20),   -- c: Smartphones, a: storage_expandable, group: Características Principales
(61, 2, 21, 5, false, true, 21),   -- c: Smartphones, a: wireless_charging, group: Batería y Carga
(62, 2, 22, 6, false, true, 22),   -- c: Smartphones, a: fingerprint_sensor, group: Diseño y Resistencia
(63, 2, 23, 6, false, true, 23),   -- c: Smartphones, a: face_unlock, group: Diseño y Resistencia
(18, 2, 10, 8, true, true, 4),     -- c: Smartphones, a: height, group: Dimensiones
(19, 2, 27, 8, true, true, 5),     -- c: Smartphones, a: width, group: Dimensiones
(20, 2, 28, 8, true, true, 6),     -- c: Smartphones, a: depth, group: Dimensiones
(23, 5, 25, 7, true, true, 0),      -- c: Microcontroladores, a: model_name, group: Especificaciones
(24, 5, 26, 7, true, true, 1),      -- c: Microcontroladores, a: brand, group: Especificaciones
(25, 5, 1, 7, true, true, 2),       -- c: Microcontroladores, a: ram_memory, group: Especificaciones
(26, 5, 11, 7, true, true, 3),      -- c: Microcontroladores, a: architecture, group: Especificaciones
(27, 5, 24, 7, true, true, 4),      -- c: Microcontroladores, a: cores, group: Especificaciones
(28, 4, 25, 7, true, true, 0),      -- c: Cafeteras, a: model_name, group: Especificaciones
(29, 4, 26, 7, true, true, 1),      -- c: Cafeteras, a: brand, group: Especificaciones
(30, 4, 12, 7, true, true, 2),      -- c: Cafeteras, a: water_capacity, group: Especificaciones
(31, 4, 13, 7, true, true, 3),      -- c: Cafeteras, a: pressure, group: Especificaciones
(32, 5, 10, 8, true, true, 4),     -- c: Microcontroladores, a: height, group: Dimensiones
(33, 5, 27, 8, true, true, 5),     -- c: Microcontroladores, a: width, group: Dimensiones
(34, 5, 28, 8, true, true, 6),     -- c: Microcontroladores, a: depth, group: Dimensiones
(35, 4, 10, 8, true, true, 4),     -- c: Cafeteras, a: height, group: Dimensiones
(36, 4, 27, 8, true, true, 5),     -- c: Cafeteras, a: width, group: Dimensiones
(37, 4, 28, 8, true, true, 6),     -- c: Cafeteras, a: depth, group: Dimensiones
(38, 6, 25, 1, true, true, 0),     -- c: Laptops, a: model_name
(39, 6, 26, 1, true, true, 1),     -- c: Laptops, a: brand
(40, 6, 1, 1, true, true, 2),      -- c: Laptops, a: ram_memory
(41, 6, 2, 1, true, true, 3),      -- c: Laptops, a: internal_storage
(42, 6, 3, 3, true, true, 4),      -- c: Laptops, a: screen_size
(43, 6, 4, 5, true, true, 5),      -- c: Laptops, a: battery_capacity
(44, 6, 8, 1, true, true, 6),      -- c: Laptops, a: processor
(45, 6, 6, 1, true, true, 7),      -- c: Laptops, a: operating_system
(46, 6, 10, 8, true, true, 8),     -- c: Laptops, a: height, group: Dimensiones
(47, 6, 27, 8, true, true, 9),     -- c: Laptops, a: width
(48, 6, 28, 8, true, true, 10),    -- c: Laptops, a: depth
(49, 7, 25, 1, true, true, 0),     -- c: Tablets, a: model_name
(50, 7, 26, 1, true, true, 1),     -- c: Tablets, a: brand
(51, 7, 1, 1, true, true, 2),      -- c: Tablets, a: ram_memory
(52, 7, 2, 1, true, true, 3),      -- c: Tablets, a: internal_storage
(53, 7, 3, 3, true, true, 4),      -- c: Tablets, a: screen_size
(54, 7, 4, 5, true, true, 5),      -- c: Tablets, a: battery_capacity
(55, 7, 8, 1, true, true, 6),      -- c: Tablets, a: processor
(56, 7, 6, 1, true, true, 7),      -- c: Tablets, a: operating_system
(57, 7, 10, 8, true, true, 8),     -- c: Tablets, a: height
(58, 7, 27, 8, true, true, 9),     -- c: Tablets, a: width
(59, 7, 28, 8, true, true, 10);    -- c: Tablets, a: depth


INSERT INTO comparable_categories (id, category_id_a, category_id_b) VALUES
(1, 7, 2),    -- Tablets comparable con Smartphones
(2, 7, 6);    -- Tablets comparable con Laptops


INSERT INTO products (
    id, name, description, product_condition, image_url,
    color, weight, size, rating, available_quantity, sold_quantity, category_id, price_id, shipping_id
) VALUES
('MLA2001234567',
 'Apple iPhone 15 Pro Max 256 GB Titanio Negro',
 'iPhone 15 Pro Max con chip A17 Pro, pantalla 6,7", cámara 48 MP, batería 4422 mAh.',
 'NEW', 'https://http2.mlstatic.com/D_NQ_NP_iphone15promax.jpg',
 'Titanio Negro', 221, '159.9 x 76.7 x 8.25 mm', 4.8, 45, 312, 2, 1, 1),    -- category: Smartphones

('MLA1987654321',
 'Apple iPhone 13 128 GB Medianoche',
 'iPhone 13 con A15 Bionic, pantalla 6,1", cámara dual 12 MP. Usado en excelente estado.',
 'USED', 'https://http2.mlstatic.com/D_NQ_NP_iphone13.jpg',
 'Medianoche', 174, '146.7 x 71.5 x 7.65 mm', 4.5, 1, 0, 2, 2, 2),       -- category: Smartphones

('MLA2009876543',
 'Samsung Galaxy S24 Ultra 256 GB',
 'Galaxy S24 Ultra con S Pen, cámara 200 MP, pantalla 6,8", batería 5000 mAh.',
 'NEW', 'https://http2.mlstatic.com/D_NQ_NP_samsungs24ultra.jpg',
 'Negro', 232, '162.3 x 79 x 8.6 mm', 4.7, 38, 287, 2, 3, 3),    -- category: Smartphones

('MLA1956473829',
 'Samsung Galaxy A54 5G 128 GB',
 'Galaxy A54 con pantalla 6,4", triple cámara 50 MP, batería 5000 mAh, IP67.',
 'NEW', 'https://http2.mlstatic.com/D_NQ_NP_samsungA54.jpg',
 'Grafito', 202, '158.2 x 76.7 x 8.2 mm', 4.6, 120, 1453, 2, 4, 4),  -- category: Smartphones

('MLA4578234567',
 'Raspberry Pi 4 Model B 8GB RAM',
 'Placa de desarrollo ARMv8, 8 GB RAM, 4 núcleos, conectividad Gigabit.',
 'NEW', 'http://image.com/rpi4.jpg',
 NULL, 500, '85 x 56 x 17 mm', 4.8, 50, 1200, 5, 5, 5),   -- category: Microcontroladores

('MLA9274837857',
 'Cafetera Express Oster PrimaLatte',
 'Cafetera automática espresso/cappuccino, 15 bar, depósito 1,5 L.',
 'NEW', 'http://image.com/oster.jpg',
 'Rojo', 4500, '32 x 22 x 28 cm', 4.7, 100, 5000, 4, 6, 6),  -- category: Cafeteras

('MLA3012345678',
 'Lenovo ThinkPad E14 Gen 4 14" 16GB 512GB SSD',
 'Laptop Lenovo ThinkPad E14, Intel Core i5-1335U, 16 GB RAM, 512 GB SSD.',
 'NEW', 'https://http2.mlstatic.com/D_NQ_NP_thinkpad.jpg',
 'Negro', 1590, '324 x 218 x 19.9 mm', 4.6, 25, 89, 6, 7, 7),  -- category: Laptops

('MLA3087654321',
 'Samsung Galaxy Tab S9 11" 128GB WiFi',
 'Tablet Samsung Galaxy Tab S9, Snapdragon 8 Gen 2, pantalla 11" AMOLED.',
 'NEW', 'https://http2.mlstatic.com/D_NQ_NP_tabs9.jpg',
 'Grafito', 498, '254 x 165.8 x 5.9 mm', 4.8, 40, 234, 7, 8, 8);  -- category: Tablets


-- iPhone 15 Pro Max
INSERT INTO product_attributes (id, product_id, attribute_def_id, raw_value, raw_unit, normalized_value, normalized_unit_id, display_value) VALUES
(1, 'MLA2001234567', 25, 'iPhone 15 Pro Max', NULL, NULL, NULL, 'iPhone 15 Pro Max'),        -- p: iPhone 15 Pro Max, a: model_name
(2, 'MLA2001234567', 26, 'Apple', NULL, NULL, NULL, 'Apple'),                                -- p: iPhone 15 Pro Max, a: brand
(3, 'MLA2001234567', 1, '8192', 'MB', 8.0, 1, '8 GB'),                                       -- p: iPhone 15 Pro Max, a: ram_memory, u: GB
(4, 'MLA2001234567', 2, '256', 'GB', 256.0, 1, '256 GB'),                                    -- p: iPhone 15 Pro Max, a: internal_storage, u: GB
(5, 'MLA2001234567', 3, '6.7', 'in', 6.7, 4, '6.7 pulgadas'),                                -- p: iPhone 15 Pro Max, a: screen_size, u: in
(6, 'MLA2001234567', 4, '4422', 'mAh', 4422.0, 6, '4422 mAh'),                               -- p: iPhone 15 Pro Max, a: battery_capacity, u: mAh
(7, 'MLA2001234567', 5, '48', NULL, 48.0, NULL, '48 MP'),                                    -- p: iPhone 15 Pro Max, a: main_camera_mp
(8, 'MLA2001234567', 6, 'iOS 17', NULL, NULL, NULL, 'iOS 17'),                               -- p: iPhone 15 Pro Max, a: operating_system
(9, 'MLA2001234567', 7, '5G', NULL, NULL, NULL, '5G'),                                       -- p: iPhone 15 Pro Max, a: connectivity
(10, 'MLA2001234567', 8, 'Apple A17 Pro', NULL, NULL, NULL, 'Apple A17 Pro'),                 -- p: iPhone 15 Pro Max, a: processor
(11, 'MLA2001234567', 9, '120', NULL, 120.0, NULL, '120 Hz'),                                -- p: iPhone 15 Pro Max, a: refresh_rate
(12, 'MLA2001234567', 14, 'Super Retina XDR OLED', NULL, NULL, NULL, 'Super Retina XDR OLED'), -- p: iPhone 15 Pro Max, a: screen_type
(13, 'MLA2001234567', 15, '5x', NULL, NULL, NULL, '5x'),                                      -- p: iPhone 15 Pro Max, a: zoom_optical
(14, 'MLA2001234567', 16, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: iPhone 15 Pro Max, a: dual_sim
(15, 'MLA2001234567', 17, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: iPhone 15 Pro Max, a: nfc
(16, 'MLA2001234567', 18, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: iPhone 15 Pro Max, a: waterproof
(17, 'MLA2001234567', 20, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: iPhone 15 Pro Max, a: fast_charging
(18, 'MLA2001234567', 21, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: iPhone 15 Pro Max, a: wireless_charging
(19, 'MLA2001234567', 22, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: iPhone 15 Pro Max, a: fingerprint_sensor
(20, 'MLA2001234567', 23, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: iPhone 15 Pro Max, a: face_unlock
(84, 'MLA2001234567', 10, '159.9', 'mm', 159.9, 12, '159.9 mm'),                              -- p: iPhone 15 Pro Max, a: height
(85, 'MLA2001234567', 27, '76.7', 'mm', 76.7, 12, '76.7 mm'),                                 -- p: iPhone 15 Pro Max, a: width
(86, 'MLA2001234567', 28, '8.25', 'mm', 8.25, 12, '8.25 mm');                                 -- p: iPhone 15 Pro Max, a: depth

-- iPhone 13
INSERT INTO product_attributes (id, product_id, attribute_def_id, raw_value, raw_unit, normalized_value, normalized_unit_id, display_value) VALUES
(21, 'MLA1987654321', 25, 'iPhone 13', NULL, NULL, NULL, 'iPhone 13'),                          -- p: iPhone 13, a: model_name
(22, 'MLA1987654321', 26, 'Apple', NULL, NULL, NULL, 'Apple'),                                  -- p: iPhone 13, a: brand
(23, 'MLA1987654321', 1, '4096', 'MB', 4.0, 1, '4 GB'),                                       -- p: iPhone 13, a: ram_memory, u: GB
(24, 'MLA1987654321', 2, '128', 'GB', 128.0, 1, '128 GB'),                                    -- p: iPhone 13, a: internal_storage, u: GB
(25, 'MLA1987654321', 3, '6.1', 'in', 6.1, 4, '6.1 pulgadas'),                                -- p: iPhone 13, a: screen_size, u: in
(26, 'MLA1987654321', 4, '3227', 'mAh', 3227.0, 6, '3227 mAh'),                               -- p: iPhone 13, a: battery_capacity, u: mAh
(27, 'MLA1987654321', 5, '12', NULL, 12.0, NULL, '12 MP'),                                    -- p: iPhone 13, a: main_camera_mp
(28, 'MLA1987654321', 6, 'iOS 17', NULL, NULL, NULL, 'iOS 17'),                               -- p: iPhone 13, a: operating_system
(29, 'MLA1987654321', 7, '5G', NULL, NULL, NULL, '5G'),                                       -- p: iPhone 13, a: connectivity
(30, 'MLA1987654321', 8, 'Apple A15 Bionic', NULL, NULL, NULL, 'Apple A15 Bionic'),            -- p: iPhone 13, a: processor
(31, 'MLA1987654321', 9, '60', NULL, 60.0, NULL, '60 Hz'),                                    -- p: iPhone 13, a: refresh_rate
(32, 'MLA1987654321', 14, 'Super Retina XDR OLED', NULL, NULL, NULL, 'Super Retina XDR OLED'), -- p: iPhone 13, a: screen_type
(33, 'MLA1987654321', 15, '2x', NULL, NULL, NULL, '2x'),                                      -- p: iPhone 13, a: zoom_optical
(34, 'MLA1987654321', 16, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: iPhone 13, a: dual_sim
(35, 'MLA1987654321', 17, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: iPhone 13, a: nfc
(36, 'MLA1987654321', 18, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: iPhone 13, a: waterproof
(87, 'MLA1987654321', 10, '146.7', 'mm', 146.7, 12, '146.7 mm'),                              -- p: iPhone 13, a: height
(88, 'MLA1987654321', 27, '71.5', 'mm', 71.5, 12, '71.5 mm'),                                 -- p: iPhone 13, a: width
(89, 'MLA1987654321', 28, '7.65', 'mm', 7.65, 12, '7.65 mm');                                 -- p: iPhone 13, a: depth

-- Samsung Galaxy S24 Ultra
INSERT INTO product_attributes (id, product_id, attribute_def_id, raw_value, raw_unit, normalized_value, normalized_unit_id, display_value) VALUES
(37, 'MLA2009876543', 25, 'Galaxy S24 Ultra', NULL, NULL, NULL, 'Galaxy S24 Ultra'),            -- p: S24 Ultra, a: model_name
(38, 'MLA2009876543', 26, 'Samsung', NULL, NULL, NULL, 'Samsung'),                             -- p: S24 Ultra, a: brand
(39, 'MLA2009876543', 1, '12288', 'MB', 12.0, 1, '12 GB'),                                    -- p: S24 Ultra, a: ram_memory, u: GB
(40, 'MLA2009876543', 2, '256', 'GB', 256.0, 1, '256 GB'),                                    -- p: S24 Ultra, a: internal_storage, u: GB
(41, 'MLA2009876543', 3, '6.8', 'in', 6.8, 4, '6.8 pulgadas'),                                -- p: S24 Ultra, a: screen_size, u: in
(42, 'MLA2009876543', 4, '5000', 'mAh', 5000.0, 6, '5000 mAh'),                               -- p: S24 Ultra, a: battery_capacity, u: mAh
(43, 'MLA2009876543', 5, '200', NULL, 200.0, NULL, '200 MP'),                                 -- p: S24 Ultra, a: main_camera_mp
(44, 'MLA2009876543', 6, 'Android 14', NULL, NULL, NULL, 'Android 14'),                       -- p: S24 Ultra, a: operating_system
(45, 'MLA2009876543', 7, '5G', NULL, NULL, NULL, '5G'),                                       -- p: S24 Ultra, a: connectivity
(46, 'MLA2009876543', 8, 'Snapdragon 8 Gen 3', NULL, NULL, NULL, 'Snapdragon 8 Gen 3'),        -- p: S24 Ultra, a: processor
(47, 'MLA2009876543', 9, '120', NULL, 120.0, NULL, '120 Hz'),                                 -- p: S24 Ultra, a: refresh_rate
(48, 'MLA2009876543', 14, 'Dynamic AMOLED 2X', NULL, NULL, NULL, 'Dynamic AMOLED 2X'),         -- p: S24 Ultra, a: screen_type
(49, 'MLA2009876543', 15, '5x', NULL, NULL, NULL, '5x'),                                      -- p: S24 Ultra, a: zoom_optical
(50, 'MLA2009876543', 16, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: S24 Ultra, a: dual_sim
(51, 'MLA2009876543', 17, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: S24 Ultra, a: nfc
(52, 'MLA2009876543', 18, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: S24 Ultra, a: waterproof
(53, 'MLA2009876543', 19, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: S24 Ultra, a: storage_expandable
(54, 'MLA2009876543', 20, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: S24 Ultra, a: fast_charging
(55, 'MLA2009876543', 21, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: S24 Ultra, a: wireless_charging
(56, 'MLA2009876543', 22, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: S24 Ultra, a: fingerprint_sensor
(90, 'MLA2009876543', 10, '162.3', 'mm', 162.3, 12, '162.3 mm'),                              -- p: S24 Ultra, a: height
(91, 'MLA2009876543', 27, '79', 'mm', 79, 12, '79 mm'),                                       -- p: S24 Ultra, a: width
(92, 'MLA2009876543', 28, '8.6', 'mm', 8.6, 12, '8.6 mm');                                    -- p: S24 Ultra, a: depth

-- Samsung Galaxy A54
INSERT INTO product_attributes (id, product_id, attribute_def_id, raw_value, raw_unit, normalized_value, normalized_unit_id, display_value) VALUES
(57, 'MLA1956473829', 25, 'Galaxy A54 5G', NULL, NULL, NULL, 'Galaxy A54 5G'),                 -- p: Galaxy A54, a: model_name
(58, 'MLA1956473829', 26, 'Samsung', NULL, NULL, NULL, 'Samsung'),                             -- p: Galaxy A54, a: brand
(59, 'MLA1956473829', 1, '8192', 'MB', 8.0, 1, '8 GB'),                                       -- p: Galaxy A54, a: ram_memory, u: GB
(60, 'MLA1956473829', 2, '128', 'GB', 128.0, 1, '128 GB'),                                    -- p: Galaxy A54, a: internal_storage, u: GB
(61, 'MLA1956473829', 3, '6.4', 'in', 6.4, 4, '6.4 pulgadas'),                                -- p: Galaxy A54, a: screen_size, u: in
(62, 'MLA1956473829', 4, '5000', 'mAh', 5000.0, 6, '5000 mAh'),                               -- p: Galaxy A54, a: battery_capacity, u: mAh
(63, 'MLA1956473829', 5, '50', NULL, 50.0, NULL, '50 MP'),                                    -- p: Galaxy A54, a: main_camera_mp
(64, 'MLA1956473829', 6, 'Android 13', NULL, NULL, NULL, 'Android 13'),                       -- p: Galaxy A54, a: operating_system
(65, 'MLA1956473829', 7, '5G', NULL, NULL, NULL, '5G'),                                       -- p: Galaxy A54, a: connectivity
(66, 'MLA1956473829', 8, 'Samsung Exynos 1380', NULL, NULL, NULL, 'Exynos 1380'),              -- p: Galaxy A54, a: processor
(67, 'MLA1956473829', 9, '120', NULL, 120.0, NULL, '120 Hz'),                                 -- p: Galaxy A54, a: refresh_rate
(68, 'MLA1956473829', 14, 'Super AMOLED', NULL, NULL, NULL, 'Super AMOLED'),                   -- p: Galaxy A54, a: screen_type
(69, 'MLA1956473829', 15, '3x', NULL, NULL, NULL, '3x'),                                      -- p: Galaxy A54, a: zoom_optical
(70, 'MLA1956473829', 16, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: Galaxy A54, a: dual_sim
(71, 'MLA1956473829', 17, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: Galaxy A54, a: nfc
(72, 'MLA1956473829', 18, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: Galaxy A54, a: waterproof
(73, 'MLA1956473829', 19, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: Galaxy A54, a: storage_expandable
(74, 'MLA1956473829', 20, 'true', NULL, 1, NULL, 'Sí'),                                    -- p: Galaxy A54, a: fast_charging
(93, 'MLA1956473829', 10, '158.2', 'mm', 158.2, 12, '158.2 mm'),                              -- p: Galaxy A54, a: height
(94, 'MLA1956473829', 27, '76.7', 'mm', 76.7, 12, '76.7 mm'),                                 -- p: Galaxy A54, a: width
(95, 'MLA1956473829', 28, '8.2', 'mm', 8.2, 12, '8.2 mm');                                    -- p: Galaxy A54, a: depth

-- Raspberry Pi 4
INSERT INTO product_attributes (id, product_id, attribute_def_id, raw_value, raw_unit, normalized_value, normalized_unit_id, display_value) VALUES
(75, 'MLA4578234567', 25, '4 Model B', NULL, NULL, NULL, '4 Model B'),          -- p: Raspberry Pi 4, a: model_name
(76, 'MLA4578234567', 26, 'Raspberry Pi', NULL, NULL, NULL, 'Raspberry Pi'),    -- p: Raspberry Pi 4, a: brand
(77, 'MLA4578234567', 1, '8192', 'MB', 8.0, 1, '8 GB'),                       -- p: Raspberry Pi 4, a: ram_memory, u: GB
(78, 'MLA4578234567', 11, 'ARMv8', NULL, NULL, NULL, 'ARMv8'),                -- p: Raspberry Pi 4, a: architecture
(79, 'MLA4578234567', 24, '4', NULL, 4.0, NULL, '4'),                         -- p: Raspberry Pi 4, a: cores
(96, 'MLA4578234567', 10, '85', 'mm', 85, 12, '85 mm'),                       -- p: Raspberry Pi 4, a: height
(97, 'MLA4578234567', 27, '56', 'mm', 56, 12, '56 mm'),                       -- p: Raspberry Pi 4, a: width
(98, 'MLA4578234567', 28, '17', 'mm', 17, 12, '17 mm');                       -- p: Raspberry Pi 4, a: depth

-- Cafetera Oster
INSERT INTO product_attributes (id, product_id, attribute_def_id, raw_value, raw_unit, normalized_value, normalized_unit_id, display_value) VALUES
(80, 'MLA9274837857', 25, 'PrimaLatte', NULL, NULL, NULL, 'PrimaLatte'),    -- p: Cafetera Oster, a: model_name
(81, 'MLA9274837857', 26, 'Oster', NULL, NULL, NULL, 'Oster'),              -- p: Cafetera Oster, a: brand
(82, 'MLA9274837857', 12, '1.5', 'L', 1.5, 7, '1.5 L'),                     -- p: Cafetera Oster, a: water_capacity, u: L
(83, 'MLA9274837857', 13, '15', 'bar', 15.0, 9, '15 bar'),                  -- p: Cafetera Oster, a: pressure, u: bar
(99, 'MLA9274837857', 10, '32', 'cm', 320, 12, '320 mm'),                  -- p: Cafetera Oster, a: height
(100, 'MLA9274837857', 27, '22', 'cm', 220, 12, '220 mm'),                 -- p: Cafetera Oster, a: width
(101, 'MLA9274837857', 28, '28', 'cm', 280, 12, '280 mm');                 -- p: Cafetera Oster, a: depth

-- Lenovo ThinkPad E14 (Laptop)
INSERT INTO product_attributes (id, product_id, attribute_def_id, raw_value, raw_unit, normalized_value, normalized_unit_id, display_value) VALUES
(102, 'MLA3012345678', 25, 'ThinkPad E14 Gen 4', NULL, NULL, NULL, 'ThinkPad E14 Gen 4'),
(103, 'MLA3012345678', 26, 'Lenovo', NULL, NULL, NULL, 'Lenovo'),
(104, 'MLA3012345678', 1, '16384', 'MB', 16.0, 1, '16 GB'),
(105, 'MLA3012345678', 2, '512', 'GB', 512.0, 1, '512 GB'),
(106, 'MLA3012345678', 3, '14', 'in', 14.0, 4, '14 pulgadas'),
(107, 'MLA3012345678', 4, '15400', 'mAh', 15400.0, 6, '1540 mAh'),
(108, 'MLA3012345678', 8, 'Intel Core i5-1335U', NULL, NULL, NULL, 'Intel Core i5-1335U'),
(109, 'MLA3012345678', 6, 'Windows 11', NULL, NULL, NULL, 'Windows 11'),
(110, 'MLA3012345678', 10, '324', 'mm', 324.0, 12, '324 mm'),
(111, 'MLA3012345678', 27, '218', 'mm', 218.0, 12, '218 mm'),
(112, 'MLA3012345678', 28, '19.9', 'mm', 19.9, 12, '19.9 mm');

-- Samsung Galaxy Tab S9 (Tablet)
INSERT INTO product_attributes (id, product_id, attribute_def_id, raw_value, raw_unit, normalized_value, normalized_unit_id, display_value) VALUES
(113, 'MLA3087654321', 25, 'Galaxy Tab S9', NULL, NULL, NULL, 'Galaxy Tab S9'),
(114, 'MLA3087654321', 26, 'Samsung', NULL, NULL, NULL, 'Samsung'),
(115, 'MLA3087654321', 1, '8192', 'MB', 8.0, 1, '8 GB'),
(116, 'MLA3087654321', 2, '128', 'GB', 128.0, 1, '128 GB'),
(117, 'MLA3087654321', 3, '11', 'in', 11.0, 4, '11 pulgadas'),
(118, 'MLA3087654321', 4, '8400', 'mAh', 8400.0, 6, '8400 mAh'),
(119, 'MLA3087654321', 8, 'Snapdragon 8 Gen 2', NULL, NULL, NULL, 'Snapdragon 8 Gen 2'),
(120, 'MLA3087654321', 6, 'Android 13', NULL, NULL, NULL, 'Android 13'),
(121, 'MLA3087654321', 10, '254', 'mm', 254.0, 12, '254 mm'),
(122, 'MLA3087654321', 27, '165.8', 'mm', 165.8, 12, '165.8 mm'),
(123, 'MLA3087654321', 28, '5.9', 'mm', 5.9, 12, '5.9 mm');