-- ============================================
-- Study Buddy Database Schema
-- ============================================
-- This file defines ALL the tables our app needs.
-- We create them all upfront so the database is ready.

-- USERS table: stores all registered users
-- Every Student, Tutor, and Admin is stored here.
-- The 'role' column tells us what type of user they are.
CREATE TABLE IF NOT EXISTS users (
    user_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    email       TEXT NOT NULL UNIQUE,
    password    TEXT NOT NULL,
    role        TEXT NOT NULL CHECK(role IN ('STUDENT', 'TUTOR', 'ADMIN')),
    semester    INTEGER,
    cgpa        REAL,
    verified    INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- SUBJECTS table: list of all available subjects
CREATE TABLE IF NOT EXISTS subjects (
    subject_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_code TEXT NOT NULL UNIQUE,
    name         TEXT NOT NULL,
    semester     INTEGER NOT NULL
);

-- TUTOR_SUBJECTS: which tutor teaches which subject
-- This is a "junction table" (many-to-many relationship)
-- One tutor can teach MANY subjects, one subject can have MANY tutors
CREATE TABLE IF NOT EXISTS tutor_subjects (
    tutor_id    INTEGER NOT NULL,
    subject_id  INTEGER NOT NULL,
    PRIMARY KEY (tutor_id, subject_id),
    FOREIGN KEY (tutor_id)   REFERENCES users(user_id),
    FOREIGN KEY (subject_id) REFERENCES subjects(subject_id)
);

-- SESSIONS table: a booked study session between a student and tutor
CREATE TABLE IF NOT EXISTS sessions (
    session_id  INTEGER PRIMARY KEY AUTOINCREMENT,
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

-- FEEDBACK table: ratings given by students to tutors after a session
CREATE TABLE IF NOT EXISTS feedback (
    feedback_id INTEGER PRIMARY KEY AUTOINCREMENT,
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

-- NOTIFICATIONS table: stores notifications for users
CREATE TABLE IF NOT EXISTS notifications (
    notification_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL,
    message         TEXT NOT NULL,
    is_read         INTEGER DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
