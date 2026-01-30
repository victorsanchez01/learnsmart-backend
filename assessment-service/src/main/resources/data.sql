-- Semilla de datos para pruebas
INSERT INTO assessment_items (id, domain_id, type, stem, difficulty, is_active)
VALUES ('550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440000', 'multiple_choice', 'What is the default port for Tomcat in Spring Boot?', 0.3, true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO assessment_item_options (id, assessment_item_id, label, statement, is_correct, feedback_template)
VALUES ('550e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', 'A', '8080', true, 'Correct! 8080 is the default port.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO assessment_item_options (id, assessment_item_id, label, statement, is_correct, feedback_template)
VALUES ('550e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001', 'B', '80', false, 'Incorrect. 80 is for HTTP.')
ON CONFLICT (id) DO NOTHING;
