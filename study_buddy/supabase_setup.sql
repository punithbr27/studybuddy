-- ============================================
-- Study Buddy — Supabase Setup SQL
-- ============================================
-- Paste this ENTIRE block into the Supabase SQL Editor and click "Run"

-- USERS table
CREATE TABLE IF NOT EXISTS users (
    user_id     SERIAL PRIMARY KEY,
    name        TEXT NOT NULL,
    email       TEXT NOT NULL UNIQUE,
    password    TEXT NOT NULL,
    role        TEXT NOT NULL CHECK(role IN ('STUDENT', 'TUTOR', 'ADMIN')),
    semester    INTEGER,
    cgpa        REAL,
    verified    INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- SUBJECTS table
CREATE TABLE IF NOT EXISTS subjects (
    subject_id   SERIAL PRIMARY KEY,
    subject_code TEXT NOT NULL UNIQUE,
    name         TEXT NOT NULL,
    semester     INTEGER NOT NULL
);

-- TUTOR_SUBJECTS junction table
CREATE TABLE IF NOT EXISTS tutor_subjects (
    tutor_id    INTEGER NOT NULL REFERENCES users(user_id),
    subject_id  INTEGER NOT NULL REFERENCES subjects(subject_id),
    PRIMARY KEY (tutor_id, subject_id)
);

-- SESSIONS table
CREATE TABLE IF NOT EXISTS sessions (
    session_id  SERIAL PRIMARY KEY,
    tutor_id    INTEGER NOT NULL REFERENCES users(user_id),
    student_id  INTEGER NOT NULL REFERENCES users(user_id),
    subject_id  INTEGER NOT NULL REFERENCES subjects(subject_id),
    start_time  TEXT NOT NULL,
    end_time    TEXT NOT NULL,
    status      TEXT NOT NULL DEFAULT 'PENDING'
                CHECK(status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- FEEDBACK table
CREATE TABLE IF NOT EXISTS feedback (
    feedback_id SERIAL PRIMARY KEY,
    session_id  INTEGER NOT NULL UNIQUE REFERENCES sessions(session_id),
    student_id  INTEGER NOT NULL REFERENCES users(user_id),
    tutor_id    INTEGER NOT NULL REFERENCES users(user_id),
    rating      INTEGER NOT NULL CHECK(rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- NOTIFICATIONS table
CREATE TABLE IF NOT EXISTS notifications (
    notification_id SERIAL PRIMARY KEY,
    user_id         INTEGER NOT NULL REFERENCES users(user_id),
    message         TEXT NOT NULL,
    is_read         INTEGER DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Enable Row Level Security (required by Supabase)
-- We'll allow all operations for now (public access)
-- ============================================
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE subjects ENABLE ROW LEVEL SECURITY;
ALTER TABLE tutor_subjects ENABLE ROW LEVEL SECURITY;
ALTER TABLE sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE feedback ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- Allow public read/write for all tables
CREATE POLICY "Allow all on users" ON users FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all on subjects" ON subjects FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all on tutor_subjects" ON tutor_subjects FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all on sessions" ON sessions FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all on feedback" ON feedback FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all on notifications" ON notifications FOR ALL USING (true) WITH CHECK (true);

-- ============================================
-- SEED DATA — Sample subjects, tutors, students, admin
-- ============================================

-- Subjects
INSERT INTO subjects (subject_code, name, semester) VALUES
('CS301', 'Data Structures', 3),
('CS302', 'Algorithms', 3),
('CS501', 'Machine Learning', 5),
('CS502', 'Cloud Computing', 5),
('CS601', 'OOAD', 6),
('CS602', 'Big Data', 6),
('CS603', 'Computer Networks', 6),
('MA301', 'Discrete Mathematics', 3)
ON CONFLICT (subject_code) DO NOTHING;

-- Admin user (auto-verified)
INSERT INTO users (name, email, password, role, semester, cgpa, verified) VALUES
('System Admin', 'admin@pesu.pes.edu', 'admin123', 'ADMIN', 0, 0, 1)
ON CONFLICT (email) DO NOTHING;

-- Sample tutors (auto-verified for demo)
INSERT INTO users (name, email, password, role, semester, cgpa, verified) VALUES
('Aarav Sharma', 'aarav@pesu.pes.edu', 'pass123', 'TUTOR', 6, 9.2, 1),
('Priya Patel', 'priya@pesu.pes.edu', 'pass123', 'TUTOR', 6, 8.8, 1),
('Rahul Kumar', 'rahul@pesu.pes.edu', 'pass123', 'TUTOR', 5, 9.0, 1)
ON CONFLICT (email) DO NOTHING;

-- Sample student
INSERT INTO users (name, email, password, role, semester, cgpa, verified) VALUES
('Punith BR', 'punith@pesu.pes.edu', 'pass123', 'STUDENT', 6, 8.5, 1)
ON CONFLICT (email) DO NOTHING;

-- Link tutors to subjects (using subqueries to get IDs)
INSERT INTO tutor_subjects (tutor_id, subject_id)
SELECT u.user_id, s.subject_id FROM users u, subjects s
WHERE u.email = 'aarav@pesu.pes.edu' AND s.subject_code IN ('CS601', 'CS301', 'CS602')
ON CONFLICT DO NOTHING;

INSERT INTO tutor_subjects (tutor_id, subject_id)
SELECT u.user_id, s.subject_id FROM users u, subjects s
WHERE u.email = 'priya@pesu.pes.edu' AND s.subject_code IN ('CS601', 'CS501', 'CS603')
ON CONFLICT DO NOTHING;

INSERT INTO tutor_subjects (tutor_id, subject_id)
SELECT u.user_id, s.subject_id FROM users u, subjects s
WHERE u.email = 'rahul@pesu.pes.edu' AND s.subject_code IN ('CS302', 'CS501', 'CS502')
ON CONFLICT DO NOTHING;
