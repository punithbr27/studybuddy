/* ═══════════════════════════════════════════
   Study Buddy — Frontend JavaScript (Supabase Edition)
   ═══════════════════════════════════════════ */

// ─── SUPABASE CONFIG ───
// IMPORTANT: Replace these with your actual Supabase values
const SUPABASE_URL = 'https://gmzpodtztfehemotfxwf.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdtenBvZHR6dGZlaGVtb3RmeHdmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU2OTk0ODgsImV4cCI6MjA5MTI3NTQ4OH0.oHbZzfIXrJ2V5mu2gSwNdXyJxzA4YTlwnB5NPWhSEMM';

const supabase = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// ─── STATE ───
let currentUser = null;
let currentStrategy = 'subject';
let selectedRating = 0;
let currentBookingTutor = null;
let currentFeedbackSession = null;

// ─── PAGE NAVIGATION ───
function showPage(pageName) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.getElementById('page-' + pageName).classList.add('active');
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));

    if (pageName === 'dashboard') loadDashboard();
    if (pageName === 'sessions') loadSessions();
    if (pageName === 'tutor-manage') loadTutorSubjects();
    if (pageName === 'admin') loadAdminDashboard();

    document.getElementById('notifPanel').style.display = 'none';
}

// ─── AUTH ───
async function handleRegister(e) {
    e.preventDefault();
    const name = document.getElementById('regName').value;
    const email = document.getElementById('regEmail').value;
    const password = document.getElementById('regPassword').value;
    const role = document.getElementById('regRole').value;
    const semester = parseInt(document.getElementById('regSemester').value);
    const cgpa = parseFloat(document.getElementById('regCgpa').value);

    // Domain check
    if (!email.toLowerCase().endsWith('@pesu.pes.edu')) {
        showMsg('registerMsg', 'Only @pesu.pes.edu emails are allowed!', 'error');
        return;
    }

    // Check if email already exists
    const { data: existing } = await supabase.from('users').select('user_id').eq('email', email);
    if (existing && existing.length > 0) {
        showMsg('registerMsg', 'Email already registered!', 'error');
        return;
    }

    const verified = (role === 'ADMIN') ? 1 : 0;

    const { data, error } = await supabase.from('users').insert([
        { name, email, password, role, semester, cgpa, verified }
    ]).select();

    if (error) {
        showMsg('registerMsg', error.message || 'Registration failed', 'error');
        return;
    }

    if (data && data.length > 0) {
        showMsg('registerMsg', 'Account created! Logging you in...', 'success');
        const user = data[0];
        setTimeout(() => loginAs({
            userId: user.user_id, name: user.name, email: user.email,
            role: user.role, verified: user.verified === 1, semester: user.semester, cgpa: user.cgpa
        }), 1000);
    }
}

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;

    const { data, error } = await supabase.from('users')
        .select('*').eq('email', email).eq('password', password);

    if (error || !data || data.length === 0) {
        showMsg('loginMsg', 'Invalid email or password', 'error');
        return;
    }

    const user = data[0];
    loginAs({
        userId: user.user_id, name: user.name, email: user.email,
        role: user.role, verified: user.verified === 1, semester: user.semester, cgpa: user.cgpa
    });
}

function loginAs(user) {
    currentUser = user;
    document.getElementById('userName').textContent = user.name;
    const badge = document.getElementById('userRoleBadge');
    badge.textContent = user.role;
    badge.className = 'user-role-badge ' + user.role;
    document.getElementById('navUser').style.display = 'flex';

    // Verification check
    if (!user.verified && user.role !== 'ADMIN') {
        showPage('unverified');
        return;
    }

    const navLinks = document.getElementById('navLinks');
    if (user.role === 'ADMIN') {
        navLinks.innerHTML = `
            <a href="#" onclick="showPage('admin')" class="nav-link active">Admin Panel</a>
            <a href="#" onclick="showPage('dashboard')" class="nav-link">Dashboard</a>
        `;
        showPage('admin');
    } else {
        navLinks.innerHTML = `
            <a href="#" onclick="showPage('dashboard')" class="nav-link active">Dashboard</a>
            <a href="#" onclick="showPage('search')" class="nav-link">Find Tutors</a>
            <a href="#" onclick="showPage('sessions')" class="nav-link">Sessions</a>
        `;
        showPage('dashboard');
    }

    if (user.role === 'TUTOR') {
        document.querySelectorAll('.tutor-only').forEach(el => el.style.display = 'block');
    }

    showToast('Welcome, ' + user.name + '! 🎉', 'success');
    loadNotifications();
}

function logout() {
    currentUser = null;
    document.getElementById('navUser').style.display = 'none';
    document.getElementById('navLinks').innerHTML = '<a href="#" onclick="showPage(\'home\')" class="nav-link active">Home</a>';
    document.querySelectorAll('.tutor-only').forEach(el => el.style.display = 'none');
    showPage('home');
}

// ─── DASHBOARD ───
async function loadDashboard() {
    if (!currentUser) return;
    document.getElementById('dashWelcome').textContent =
        'Welcome back, ' + currentUser.name + '! You are logged in as ' + currentUser.role.toLowerCase() + '.';

    const { data: sessions } = await supabase.from('sessions')
        .select('*, tutor:users!sessions_tutor_id_fkey(name), student:users!sessions_student_id_fkey(name), subject:subjects(name)')
        .or(`student_id.eq.${currentUser.userId},tutor_id.eq.${currentUser.userId}`)
        .order('created_at', { ascending: false })
        .limit(5);

    const container = document.getElementById('recentSessions');
    if (!sessions || sessions.length === 0) {
        container.innerHTML = '<p class="empty-state">No sessions yet. Start by finding a tutor! 🔍</p>';
        return;
    }
    container.innerHTML = sessions.map(s => renderSessionCard(s)).join('');
}

// ─── SEARCH ───
function setStrategy(btn) {
    document.querySelectorAll('.strat-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    currentStrategy = btn.dataset.strategy;
    performSearch();
}

async function performSearch() {
    const keyword = document.getElementById('searchInput').value;
    if (!keyword.trim()) {
        document.getElementById('searchResults').innerHTML = '<p class="empty-state">Enter a subject to find tutors 🎯</p>';
        return;
    }

    let tutors = [];

    if (currentStrategy === 'subject') {
        // Find subjects matching keyword
        const { data: matchedSubjects } = await supabase.from('subjects')
            .select('subject_id').or(`name.ilike.%${keyword}%,subject_code.ilike.%${keyword}%`);

        if (matchedSubjects && matchedSubjects.length > 0) {
            const subjectIds = matchedSubjects.map(s => s.subject_id);
            const { data: tutorLinks } = await supabase.from('tutor_subjects')
                .select('tutor_id').in('subject_id', subjectIds);

            if (tutorLinks && tutorLinks.length > 0) {
                const tutorIds = [...new Set(tutorLinks.map(t => t.tutor_id))];
                const { data } = await supabase.from('users')
                    .select('*').in('user_id', tutorIds).eq('role', 'TUTOR').eq('verified', 1);
                tutors = data || [];
            }
        }
    } else if (currentStrategy === 'rating') {
        const { data } = await supabase.from('users')
            .select('*').eq('role', 'TUTOR').eq('verified', 1);
        tutors = data || [];
    } else if (currentStrategy === 'availability') {
        const { data } = await supabase.from('users')
            .select('*').eq('role', 'TUTOR').eq('verified', 1);
        tutors = data || [];
    }

    // Enrich tutors with ratings and subjects
    for (let t of tutors) {
        const { data: fb } = await supabase.from('feedback')
            .select('rating').eq('tutor_id', t.user_id);
        t.rating = fb && fb.length > 0 ? (fb.reduce((a, b) => a + b.rating, 0) / fb.length).toFixed(1) : '0.0';
        t.totalRatings = fb ? fb.length : 0;

        const { data: subs } = await supabase.from('tutor_subjects')
            .select('subjects(name)').eq('tutor_id', t.user_id);
        t.subjects = subs ? subs.map(s => s.subjects.name) : [];
        t.userId = t.user_id;
    }

    // Sort by rating if strategy is rating
    if (currentStrategy === 'rating') {
        tutors.sort((a, b) => parseFloat(b.rating) - parseFloat(a.rating));
    }

    const container = document.getElementById('searchResults');
    if (tutors.length === 0) {
        container.innerHTML = '<p class="empty-state">No tutors found for "' + keyword + '" 😕<br>Try a different subject or strategy.</p>';
        return;
    }

    container.innerHTML = tutors.map(t => `
        <div class="tutor-card">
            <div class="tutor-card-header">
                <div>
                    <div class="tutor-name">${t.name}</div>
                    <div class="tutor-meta">Semester ${t.semester} • CGPA: ${t.cgpa}</div>
                </div>
                <div class="tutor-rating">⭐ ${t.rating} <span style="color: var(--text-muted); font-size: 12px">(${t.totalRatings})</span></div>
            </div>
            <div class="tutor-subjects">
                ${t.subjects.map(s => `<span class="subject-tag">${s}</span>`).join('')}
            </div>
            ${currentUser && currentUser.role === 'STUDENT' ?
            `<button class="btn btn-primary btn-sm" onclick='openBookingModal(${JSON.stringify(t)})'>Book Session</button>` :
            (currentUser ? '' : '<p style="font-size:12px;color:var(--text-muted)">Login as student to book</p>')}
        </div>
    `).join('');
}

// ─── BOOKING ───
async function openBookingModal(tutor) {
    currentBookingTutor = tutor;
    document.getElementById('bookingTutorInfo').innerHTML = `
        <div class="tutor-card" style="margin-bottom:16px;">
            <div class="tutor-name">${tutor.name}</div>
            <div class="tutor-meta">⭐ ${tutor.rating} • Semester ${tutor.semester}</div>
        </div>
    `;

    const { data: subjects } = await supabase.from('tutor_subjects')
        .select('subjects(subject_id, subject_code, name)').eq('tutor_id', tutor.userId);

    const select = document.getElementById('bookSubject');
    select.innerHTML = subjects.map(s =>
        `<option value="${s.subjects.subject_id}">${s.subjects.subject_code} - ${s.subjects.name}</option>`
    ).join('');

    const now = new Date();
    now.setMinutes(0);
    now.setHours(now.getHours() + 1);
    document.getElementById('bookStart').value = now.toISOString().slice(0, 16);
    now.setHours(now.getHours() + 1);
    document.getElementById('bookEnd').value = now.toISOString().slice(0, 16);

    document.getElementById('bookingModal').style.display = 'flex';
}

async function submitBooking(e) {
    e.preventDefault();
    const sessionData = {
        tutor_id: currentBookingTutor.userId,
        student_id: currentUser.userId,
        subject_id: parseInt(document.getElementById('bookSubject').value),
        start_time: document.getElementById('bookStart').value,
        end_time: document.getElementById('bookEnd').value,
        status: 'PENDING'
    };

    const { error } = await supabase.from('sessions').insert([sessionData]);
    if (!error) {
        closeModal();
        showToast('Session booked! Waiting for tutor confirmation. 📅', 'success');
        showPage('sessions');
    } else {
        showToast(error.message || 'Booking failed', 'error');
    }
}

// ─── SESSIONS ───
let allSessions = [];

async function loadSessions() {
    if (!currentUser) return;

    const { data } = await supabase.from('sessions')
        .select('*, tutor:users!sessions_tutor_id_fkey(name), student:users!sessions_student_id_fkey(name), subject:subjects(name)')
        .or(`student_id.eq.${currentUser.userId},tutor_id.eq.${currentUser.userId}`)
        .order('created_at', { ascending: false });

    allSessions = data || [];
    renderSessions(allSessions);
}

function filterSessions(btn, status) {
    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    if (status === 'all') renderSessions(allSessions);
    else renderSessions(allSessions.filter(s => s.status === status));
}

function renderSessions(sessions) {
    const container = document.getElementById('sessionsList');
    if (sessions.length === 0) {
        container.innerHTML = '<p class="empty-state">No sessions found.</p>';
        return;
    }
    container.innerHTML = sessions.map(s => renderSessionCard(s)).join('');
}

function renderSessionCard(s) {
    const isTutor = currentUser && currentUser.userId === s.tutor_id;
    const isStudent = currentUser && currentUser.userId === s.student_id;
    const tutorName = s.tutor ? s.tutor.name : '';
    const studentName = s.student ? s.student.name : '';
    const subjectName = s.subject ? s.subject.name : 'Subject';

    let actions = '';
    if (s.status === 'PENDING' && isTutor) {
        actions = `
            <button class="btn btn-success btn-sm" onclick="acceptSession(${s.session_id})">✅ Accept</button>
            <button class="btn btn-danger btn-sm" onclick="cancelSession(${s.session_id})">❌ Reject</button>
        `;
    } else if (s.status === 'PENDING' && isStudent) {
        actions = `<button class="btn btn-danger btn-sm" onclick="cancelSession(${s.session_id})">Cancel</button>`;
    } else if (s.status === 'CONFIRMED' && isTutor) {
        actions = `
            <button class="btn btn-primary btn-sm" onclick="completeSession(${s.session_id})">✅ Mark Complete</button>
            <button class="btn btn-danger btn-sm" onclick="cancelSession(${s.session_id})">Cancel</button>
        `;
    } else if (s.status === 'CONFIRMED' && isStudent) {
        actions = `<button class="btn btn-danger btn-sm" onclick="cancelSession(${s.session_id})">Cancel</button>`;
    } else if (s.status === 'COMPLETED' && isStudent) {
        actions = `<button class="btn btn-primary btn-sm" onclick="openFeedbackModal(${s.session_id}, ${s.tutor_id})">⭐ Rate</button>`;
    }

    return `
        <div class="session-card">
            <div class="session-info">
                <h4>${subjectName}
                    <span class="status-badge status-${s.status}">${s.status}</span>
                </h4>
                <p>${isTutor ? '👨‍🎓 Student: ' + studentName : '👨‍🏫 Tutor: ' + tutorName}</p>
                <p>📅 ${formatTime(s.start_time)} → ${formatTime(s.end_time)}</p>
            </div>
            <div class="session-actions">${actions}</div>
        </div>
    `;
}

async function acceptSession(sessionId) {
    await supabase.from('sessions').update({ status: 'CONFIRMED' }).eq('session_id', sessionId);
    showToast('Session confirmed! 🎉', 'success');
    loadSessions();
    loadDashboard();
}

async function cancelSession(sessionId) {
    await supabase.from('sessions').update({ status: 'CANCELLED' }).eq('session_id', sessionId);
    showToast('Session cancelled.', 'success');
    loadSessions();
    loadDashboard();
}

async function completeSession(sessionId) {
    await supabase.from('sessions').update({ status: 'COMPLETED' }).eq('session_id', sessionId);
    showToast('Session completed! Student will be asked to rate. ⭐', 'success');
    loadSessions();
}

// ─── FEEDBACK ───
function openFeedbackModal(sessionId, tutorId) {
    currentFeedbackSession = { sessionId, tutorId };
    selectedRating = 0;
    document.querySelectorAll('#starRating .star').forEach(s => s.classList.remove('active'));
    document.getElementById('feedbackComment').value = '';
    document.getElementById('feedbackModal').style.display = 'flex';
}

function setRating(rating) {
    selectedRating = rating;
    document.querySelectorAll('#starRating .star').forEach(s => {
        s.classList.toggle('active', parseInt(s.dataset.rating) <= rating);
    });
}

async function submitFeedback() {
    if (selectedRating === 0) { showToast('Please select a rating', 'error'); return; }

    const { error } = await supabase.from('feedback').insert([{
        session_id: currentFeedbackSession.sessionId,
        student_id: currentUser.userId,
        tutor_id: currentFeedbackSession.tutorId,
        rating: selectedRating,
        comment: document.getElementById('feedbackComment').value
    }]);

    if (!error) {
        closeModal();
        showToast('Thanks for your feedback! ⭐', 'success');
        loadSessions();
    } else {
        showToast(error.message || 'Feedback failed', 'error');
    }
}

// ─── TUTOR SUBJECT MANAGEMENT ───
async function loadTutorSubjects() {
    if (!currentUser || currentUser.role !== 'TUTOR') return;

    const { data: mySubjects } = await supabase.from('tutor_subjects')
        .select('subjects(subject_id, subject_code, name)').eq('tutor_id', currentUser.userId);

    const mySubjectsContainer = document.getElementById('mySubjects');
    if (!mySubjects || mySubjects.length === 0) {
        mySubjectsContainer.innerHTML = '<p class="empty-state" style="padding:16px">No subjects added yet.</p>';
    } else {
        mySubjectsContainer.innerHTML = mySubjects.map(s =>
            `<span class="subject-chip">${s.subjects.subject_code} - ${s.subjects.name}</span>`
        ).join('');
    }

    const { data: allSubjects } = await supabase.from('subjects').select('*');
    const mySubjectIds = mySubjects ? mySubjects.map(s => s.subjects.subject_id) : [];
    const available = (allSubjects || []).filter(s => !mySubjectIds.includes(s.subject_id));

    const container = document.getElementById('availableSubjects');
    if (available.length === 0) {
        container.innerHTML = '<p class="empty-state" style="padding:16px">You\'re teaching all available subjects! 🏆</p>';
    } else {
        container.innerHTML = available.map(s => `
            <div class="subject-list-item">
                <span>${s.subject_code} - ${s.name} (Sem ${s.semester})</span>
                <button class="btn btn-primary btn-sm" onclick="addSubjectToTeach(${s.subject_id})">+ Add</button>
            </div>
        `).join('');
    }
}

async function addSubjectToTeach(subjectId) {
    const { error } = await supabase.from('tutor_subjects').insert([{
        tutor_id: currentUser.userId, subject_id: subjectId
    }]);
    if (!error) {
        showToast('Subject added! 📚', 'success');
        loadTutorSubjects();
    }
}

// ─── ADMIN DASHBOARD ───
async function loadAdminDashboard() {
    if (!currentUser || currentUser.role !== 'ADMIN') return;

    const { data: users } = await supabase.from('users')
        .select('*').eq('verified', 0).neq('role', 'ADMIN');

    const container = document.getElementById('adminPendingList');
    if (!users || users.length === 0) {
        container.innerHTML = '<p class="empty-state">No pending verifications. Good job! 🛡️</p>';
        return;
    }

    container.innerHTML = users.map(u => `
        <div class="session-card">
            <div class="session-info">
                <h4>${u.name} <span class="status-badge status-PENDING">${u.role}</span></h4>
                <p>📧 ${u.email}</p>
            </div>
            <div class="session-actions">
                <button class="btn btn-success btn-sm" onclick="verifyUser(${u.user_id})">Approve</button>
                <button class="btn btn-danger btn-sm" onclick="rejectUser(${u.user_id})">Reject</button>
            </div>
        </div>
    `).join('');
}

async function verifyUser(userId) {
    await supabase.from('users').update({ verified: 1 }).eq('user_id', userId);
    showToast('Account verified! ✅', 'success');
    loadAdminDashboard();
}

async function rejectUser(userId) {
    if (!confirm('Are you sure you want to reject and delete this account?')) return;
    await supabase.from('users').delete().eq('user_id', userId);
    showToast('Account rejected.', 'success');
    loadAdminDashboard();
}

// ─── NOTIFICATIONS ───
async function loadNotifications() {
    if (!currentUser) return;
    const { data } = await supabase.from('notifications')
        .select('*').eq('user_id', currentUser.userId).eq('is_read', 0);

    const badge = document.getElementById('notifBadge');
    const count = data ? data.length : 0;
    if (count > 0) {
        badge.textContent = count;
        badge.style.display = 'flex';
    } else {
        badge.style.display = 'none';
    }
}

async function showNotifications() {
    const panel = document.getElementById('notifPanel');
    if (panel.style.display === 'block') { panel.style.display = 'none'; return; }

    const { data } = await supabase.from('notifications')
        .select('*').eq('user_id', currentUser.userId).order('created_at', { ascending: false });

    const list = document.getElementById('notifList');
    if (!data || data.length === 0) {
        list.innerHTML = '<p class="empty-state">No notifications yet.</p>';
    } else {
        list.innerHTML = data.map(n => `
            <div class="notif-item ${n.is_read ? '' : 'unread'}">
                ${n.message}
                <div class="notif-time">${formatTime(n.created_at)}</div>
            </div>
        `).join('');
    }

    // Mark all as read
    await supabase.from('notifications')
        .update({ is_read: 1 }).eq('user_id', currentUser.userId).eq('is_read', 0);

    panel.style.display = 'block';
    document.getElementById('notifBadge').style.display = 'none';
}

// ─── UTILITIES ───
function closeModal() {
    document.getElementById('bookingModal').style.display = 'none';
    document.getElementById('feedbackModal').style.display = 'none';
}

function showMsg(elementId, text, type) {
    const el = document.getElementById(elementId);
    el.textContent = text;
    el.className = 'msg ' + type;
}

function showToast(message, type) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast ' + type + ' show';
    setTimeout(() => toast.className = 'toast', 3000);
}

function formatTime(dt) {
    if (!dt) return '';
    try {
        const d = new Date(dt);
        return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) +
            ' ' + d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
    } catch { return dt; }
}

// ─── INIT ───
document.addEventListener('DOMContentLoaded', () => {
    console.log('Study Buddy loaded with Supabase backend');
    console.log('Admin login: admin@pesu.pes.edu / admin123');
});
