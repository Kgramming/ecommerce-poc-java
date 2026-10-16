-- E-commerce POC: 8 fixed seed users (names/emails must match exactly)
INSERT INTO users (id, name, email) VALUES (1, 'Aarav Sharma', 'aarav.sharma@example.com');
INSERT INTO users (id, name, email) VALUES (2, 'Priya Patel', 'priya.patel@example.com');
INSERT INTO users (id, name, email) VALUES (3, 'Kabir Malhotra', 'kabir.malhotra@example.com');
INSERT INTO users (id, name, email) VALUES (4, 'Ananya Iyer', 'ananya.iyer@example.com');
INSERT INTO users (id, name, email) VALUES (5, 'Vikram Reddy', 'vikram.reddy@example.com');
INSERT INTO users (id, name, email) VALUES (6, 'Neha Gupta', 'neha.gupta@example.com');
INSERT INTO users (id, name, email) VALUES (7, 'Arjun Nair', 'arjun.nair@example.com');
INSERT INTO users (id, name, email) VALUES (8, 'Divya Menon', 'divya.menon@example.com');
SELECT setval('users_id_seq', 8);
