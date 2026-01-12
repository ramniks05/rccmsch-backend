-- PostgreSQL SQL Script to Insert Case Types Master Data
-- Run this script to populate the case_types table with initial data

-- Insert Case Types
INSERT INTO case_types (name, code, description, is_active, created_at, updated_at)
VALUES
    ('Mutation (after Gift/Sale Deeds)', 'MUTATION_GIFT_SALE', 'Mutation after Gift or Sale Deeds', true, NOW(), NOW()),
    ('Mutation (after death of landowner)', 'MUTATION_DEATH', 'Mutation after death of landowner', true, NOW(), NOW()),
    ('Partition (division of land parcel)', 'PARTITION', 'Partition or division of land parcel', true, NOW(), NOW()),
    ('Change in Classification of Land (before 2014)', 'CLASSIFICATION_CHANGE_BEFORE_2014', 'Change in classification of land before 2014', true, NOW(), NOW()),
    ('Change in Classification of Land (after 2014)', 'CLASSIFICATION_CHANGE_AFTER_2014', 'Change in classification of land after 2014', true, NOW(), NOW()),
    ('Implementation of order passed by a Higher Court', 'HIGHER_COURT_ORDER', 'Implementation of order passed by a Higher Court', true, NOW(), NOW()),
    ('Allotment of Land', 'ALLOTMENT', 'Allotment of Land', true, NOW(), NOW()),
    ('Land Acquisition (under RFCTLARR Act, 2013 or National Highways Act, 1956)', 'LAND_ACQUISITION_RFCTLARR_NHA', 'Land Acquisition under RFCTLARR Act, 2013 or National Highways Act, 1956', true, NOW(), NOW()),
    ('Land Acquisition (under Direct Purchase)', 'LAND_ACQUISITION_DIRECT_PURCHASE', 'Land Acquisition under Direct Purchase', true, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- Verify inserted data
SELECT id, name, code, is_active, created_at FROM case_types ORDER BY name;

