-- V2: Seed data for development and demo
-- Passwords: Admin@123, User@123, Operator@123 (BCrypt encoded)

-- Admin user
INSERT INTO users (email, password_hash, role, full_name, phone)
VALUES ('admin@demo.com', '$2b$10$dCpHfXogDSyLoPndK0YOxuj/q7rwEQpNMTgpFUfbT3TkON60rJLG.', 'ADMIN', 'Admin User', '0901234567');

-- Customer user
INSERT INTO users (email, password_hash, role, full_name, phone)
VALUES ('user@demo.com', '$2b$10$BuOU4ZBlojdb34b6fVfWgOMeoJkGJ.CaOrFC1fmA14YOqrQYeCpFi', 'CUSTOMER', 'Test Customer', '0907654321');

-- Operator user
INSERT INTO users (email, password_hash, role, full_name, phone)
VALUES ('operator@demo.com', '$2b$10$cc2xByisynLB1.RHYemJ8O..WUtIQU73s34HAZVkJDau3SRr9Wz2C', 'OPERATOR', 'Operator User', '0909876543');

-- Sample concerts
INSERT INTO concerts (title, description, venue, starts_at, ends_at, status, created_by)
VALUES
('Rock Festival 2026', 'The biggest rock festival of the year featuring top international bands', 'National Stadium, Hanoi', '2026-07-15 18:00:00+07', '2026-07-15 23:00:00+07', 'PUBLISHED', 1),
('Jazz Night', 'An evening of smooth jazz with renowned artists', 'Opera House, HCMC', '2026-08-20 19:00:00+07', '2026-08-20 22:00:00+07', 'PUBLISHED', 1),
('EDM Rave Party', 'Electronic dance music festival with world-class DJs', 'Beach Arena, Da Nang', '2026-09-10 20:00:00+07', '2026-09-11 04:00:00+07', 'DRAFT', 1);

-- Ticket categories for Rock Festival
INSERT INTO ticket_categories (concert_id, name, price_amount, price_currency, total_quantity, available_quantity, sort_order)
VALUES
(1, 'VIP', 3000000, 'VND', 500, 500, 1),
(1, 'Standard', 1500000, 'VND', 5000, 5000, 2),
(1, 'Economy', 800000, 'VND', 10000, 10000, 3);

-- Ticket categories for Jazz Night
INSERT INTO ticket_categories (concert_id, name, price_amount, price_currency, total_quantity, available_quantity, sort_order)
VALUES
(2, 'VIP', 2000000, 'VND', 200, 200, 1),
(2, 'Standard', 1000000, 'VND', 800, 800, 2);

-- Ticket categories for EDM Rave
INSERT INTO ticket_categories (concert_id, name, price_amount, price_currency, total_quantity, available_quantity, sort_order)
VALUES
(3, 'VIP', 2500000, 'VND', 300, 300, 1),
(3, 'Standard', 1200000, 'VND', 3000, 3000, 2),
(3, 'Economy', 600000, 'VND', 7000, 7000, 3);

-- Sample vouchers
INSERT INTO vouchers (code, type, value, min_order_amount, max_discount_amount, max_uses, used_count, max_uses_per_user, valid_from, valid_until, status)
VALUES
('SUMMER10', 'PERCENT', 10, 1000000, 500000, 100, 0, 2, '2026-06-01 00:00:00+07', '2026-12-31 23:59:59+07', 'ACTIVE'),
('FLAT200K', 'FIXED', 200000, 500000, NULL, 50, 0, 1, '2026-06-01 00:00:00+07', '2026-12-31 23:59:59+07', 'ACTIVE'),
('VIP50', 'PERCENT', 50, 5000000, 2000000, 10, 0, 1, '2026-06-01 00:00:00+07', '2026-08-31 23:59:59+07', 'ACTIVE');
