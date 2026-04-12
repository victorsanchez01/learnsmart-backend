-- Domains
INSERT INTO domains (id, code, name, description)
VALUES 
('550e8400-e29b-41d4-a716-446655440000', 'BACKEND', 'Backend Development', 'Server-side logic and databases'),
('550e8400-e29b-41d4-a716-446655440100', 'FRONTEND', 'Frontend Development', 'User interfaces and client-side logic')
ON CONFLICT (code) DO NOTHING;

-- Skills
INSERT INTO skills (id, domain_id, code, name, description, level, created_at)
VALUES
('550e8400-e29b-41d4-a716-446655440010', '550e8400-e29b-41d4-a716-446655440000', 'JAVA_BASICS', 'Java Basics', 'Core Java syntax and concepts', 'BEGINNER', NOW()),
('550e8400-e29b-41d4-a716-446655440020', '550e8400-e29b-41d4-a716-446655440000', 'SPRING_BOOT', 'Spring Boot', 'Building microservices with Spring Boot', 'INTERMEDIATE', NOW()),
('550e8400-e29b-41d4-a716-446655440110', '550e8400-e29b-41d4-a716-446655440100', 'REACT_BASICS', 'React Basics', 'Components, JSX, and Virtual DOM', 'BEGINNER', NOW()),
('550e8400-e29b-41d4-a716-446655440120', '550e8400-e29b-41d4-a716-446655440100', 'REACT_HOOKS', 'React Hooks', 'Managing state with Hooks', 'INTERMEDIATE', NOW())
ON CONFLICT (domain_id, code) DO NOTHING;

-- Content Items
INSERT INTO content_items (id, domain_id, type, title, description, estimated_minutes, difficulty, metadata, is_active, created_at, updated_at)
VALUES
('550e8400-e29b-41d4-a716-446655440201', '550e8400-e29b-41d4-a716-446655440000', 'ARTICLE', 'Introducción a Java', 'Guía completa de la sintaxis de Java y los fundamentos de la programación orientada a objetos.', 15, 0.2, '{"thumbnail": "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=640&q=80", "tags": ["java", "programacion", "principiantes"]}', true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440202', '550e8400-e29b-41d4-a716-446655440000', 'VIDEO', 'Configuración de Spring Boot', 'Tutorial en video sobre cómo configurar un proyecto Spring Boot con APIs REST e integración de base de datos.', 20, 0.4, '{"thumbnail": "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=640&q=80", "tags": ["spring", "backend", "microservicios"]}', true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440203', '550e8400-e29b-41d4-a716-446655440100', 'ARTICLE', 'Componentes de React 101', 'Aprende a crear componentes funcionales, trabajar con props y componer interfaces en React.', 10, 0.3, '{"thumbnail": "https://images.unsplash.com/photo-1633356122544-f134324a6cee?w=640&q=80", "tags": ["react", "frontend", "javascript"]}', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Map Content to Skills
INSERT INTO content_item_skills (content_item_id, skill_id, weight)
VALUES
('550e8400-e29b-41d4-a716-446655440201', '550e8400-e29b-41d4-a716-446655440010', 1.0),
('550e8400-e29b-41d4-a716-446655440202', '550e8400-e29b-41d4-a716-446655440020', 1.0),
('550e8400-e29b-41d4-a716-446655440203', '550e8400-e29b-41d4-a716-446655440110', 1.0)
ON CONFLICT (content_item_id, skill_id) DO NOTHING;
