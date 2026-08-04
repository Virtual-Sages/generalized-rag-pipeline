-- Seed Data for GenRAG Pipeline
-- This file contains sample data for users, chats, and messages.
-- You can run this file directly against your PostgreSQL 'genrag' database.

-- Note: The password for 'seeduser' is 'password123' (BCrypt hashed)
INSERT INTO users (id, username, email, password, created_at, updated_at) 
VALUES (
    '11111111-1111-1111-1111-111111111111', 
    'seeduser', 
    'seed@example.com', 
    '$2a$10$TZ3h.M0p2U9BV3szdF7Qw.2guD3UbG/5bvk9mOKdO.uBApjMDxeSW', 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- Seed Chat 1
INSERT INTO chats (id, user_id, title, created_at, updated_at)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'First steps with AI',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- Seed Chat 2
INSERT INTO chats (id, user_id, title, created_at, updated_at)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'Understanding RAG pipelines',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- Messages for Chat 1
INSERT INTO messages (id, chat_id, role, content, created_at)
VALUES (
    '44444444-4444-4444-4444-444444444444',
    '22222222-2222-2222-2222-222222222222',
    'USER',
    'Hello, can you help me understand how this works?',
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

INSERT INTO messages (id, chat_id, role, content, created_at)
VALUES (
    '55555555-5555-5555-5555-555555555555',
    '22222222-2222-2222-2222-222222222222',
    'ASSISTANT',
    'Of course! I am an AI designed to assist you with retrieving information from your documents.',
    CURRENT_TIMESTAMP + interval '1 second'
) ON CONFLICT (id) DO NOTHING;

-- Messages for Chat 2
INSERT INTO messages (id, chat_id, role, content, created_at)
VALUES (
    '66666666-6666-6666-6666-666666666666',
    '33333333-3333-3333-3333-333333333333',
    'USER',
    'What does RAG stand for?',
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

INSERT INTO messages (id, chat_id, role, content, created_at)
VALUES (
    '77777777-7777-7777-7777-777777777777',
    '33333333-3333-3333-3333-333333333333',
    'ASSISTANT',
    'RAG stands for Retrieval-Augmented Generation. It is a technique that enhances large language models by retrieving relevant information from an external database before generating an answer.',
    CURRENT_TIMESTAMP + interval '1 second'
) ON CONFLICT (id) DO NOTHING;
