-- ============================================
-- Study Buddy Database Schema (PostgreSQL / Supabase)
-- ============================================

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
    tutor_id    INTEGER NOT NULL,
    subject_id  INTEGER NOT NULL,
    PRIMARY KEY (tutor_id, subject_id),
    FOREIGN KEY (tutor_id)   REFERENCES users(user_id),
    FOREIGN KEY (subject_id) REFERENCES subjects(subject_id)
);

-- SESSIONS table
CREATE TABLE IF NOT EXISTS sessions (
    session_id  SERIAL PRIMARY KEY,
    tutor_id    INTEGER NOT NULL,
    student_id  INTEGER NOT NULL,
    subject_id  INTEGER NOT NULL,
    start_time  TEXT NOT NULL,
    end_time    TEXT NOT NULL,
    status      TEXT NOT NULL DEFAULT 'PENDING'
                CHECK(status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tutor_id)   REFERENCES users(user_id),
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (subject_id) REFERENCES subjects(subject_id)
);

-- FEEDBACK table
CREATE TABLE IF NOT EXISTS feedback (
    feedback_id SERIAL PRIMARY KEY,
    session_id  INTEGER NOT NULL UNIQUE,
    student_id  INTEGER NOT NULL,
    tutor_id    INTEGER NOT NULL,
    rating      INTEGER NOT NULL CHECK(rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES sessions(session_id),
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (tutor_id)   REFERENCES users(user_id)
);

-- NOTIFICATIONS table
CREATE TABLE IF NOT EXISTS notifications (
    notification_id SERIAL PRIMARY KEY,
    user_id         INTEGER NOT NULL,
    message         TEXT NOT NULL,
    is_read         INTEGER DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
