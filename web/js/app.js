/* ═══════════════════════════════════════════
   Study Buddy — Frontend JavaScript
   ═══════════════════════════════════════════ */

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

    // Update nav links
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));

    if (pageName === 'dashboard') loadDashboard();
    if (pageName === 'sessions') loadSessions();
    if (pageName === 'search') { }
    if (pageName === 'tutor-manage') loadTutorSubjects();
    if (pageName === 'admin') loadAdminDashboard();

    // Close notification panel
    document.getElementById('notifPanel').style.display = 'none';
}

// ─── AUTH ───
async function handleRegister(e) {
    e.preventDefault();
    const data = {
        name: document.getElementById('regName').value,
        email: document.getElementById('regEmail').value,
        password: document.getElementById('regPassword').value,
        role: document.getElementById('regRole').value,
        semester: document.getElementById('regSemester').value,
        cgpa: document.getElementById('regCgpa').value
    };

    try {
        const res = await apiPost('/api/register', data);
        if (res.success) {
            showMsg('registerMsg', 'Account created! Logging you in...', 'success');
            setTimeout(() => loginAs(res), 1000);
        } else {
            showMsg('registerMsg', res.error || 'Registration failed', 'error');
        }
    } catch (err) {
        showMsg('registerMsg', 'Server error. Is the backend running?', 'error');
    }
}

async function handleLogin(e) {
    e.preventDefault();
    const data = {
        email: document.getElementById('loginEmail').value,
        password: document.getElementById('loginPassword').value
    };

    try {
        const res = await apiPost('/api/login', data);
        if (res.success) {
            loginAs(res);
        } else {
            showMsg('loginMsg', res.error || 'Invalid credentials', 'error');
        }
    } catch (err) {
        showMsg('loginMsg', 'Server error. Is the backend running?', 'error');
    }
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

    // Update nav
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

    // Show tutor-only options
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

    const sessions = await apiGet('/api/sessions?userId=' + currentUser.userId);
    const container = document.getElementById('recentSessions');

    if (sessions.length === 0) {
        container.innerHTML = '<p class="empty-state">No sessions yet. Start by finding a tutor! 🔍</p>';
        return;
    }

    container.innerHTML = sessions.slice(0, 5).map(s => renderSessionCard(s)).join('');
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

    const tutors = await apiGet(`/api/search?keyword=${encodeURIComponent(keyword)}&strategy=${currentStrategy}`);
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

    // Load tutor's subjects into dropdown
    const subjects = await apiGet('/api/tutor/subjects?tutorId=' + tutor.userId);
    const select = document.getElementById('bookSubject');
    select.innerHTML = subjects.map(s => `<option value="${s.subjectId}">${s.subjectCode} - ${s.name}</option>`).join('');

    // Set default times
    const now = new Date();
    now.setMinutes(0);
    now.setHours(now.getHours() + 1);
    const startStr = now.toISOString().slice(0, 16);
    now.setHours(now.getHours() + 1);
    const endStr = now.toISOString().slice(0, 16);
    document.getElementById('bookStart').value = startStr;
    document.getElementById('bookEnd').value = endStr;

    document.getElementById('bookingModal').style.display = 'flex';
}

async function submitBooking(e) {
    e.preventDefault();
    const data = {
        tutorId: currentBookingTutor.userId,
        studentId: currentUser.userId,
        subjectId: document.getElementById('bookSubject').value,
        startTime: document.getElementById('bookStart').value,
        endTime: document.getElementById('bookEnd').value
    };

    const res = await apiPost('/api/booking', data);
    if (res.success) {
        closeModal();
        showToast('Session booked! Waiting for tutor confirmation. 📅', 'success');
        showPage('sessions');
    } else {
        showToast(res.error || 'Booking failed', 'error');
    }
}

// ─── SESSIONS ───
let allSessions = [];

async function loadSessions() {
    if (!currentUser) return;
    allSessions = await apiGet('/api/sessions?userId=' + currentUser.userId);
    renderSessions(allSessions);
}

function filterSessions(btn, status) {
    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    if (status === 'all') {
        renderSessions(allSessions);
    } else {
        renderSessions(allSessions.filter(s => s.status === status));
    }
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
    const isTutor = currentUser && currentUser.userId === s.tutorId;
    const isStudent = currentUser && currentUser.userId === s.studentId;

    let actions = '';
    if (s.status === 'PENDING' && isTutor) {
        actions = `
            <button class="btn btn-success btn-sm" onclick="acceptSession(${s.sessionId})">✅ Accept</button>
            <button class="btn btn-danger btn-sm" onclick="cancelSession(${s.sessionId})">❌ Reject</button>
        `;
    } else if (s.status === 'PENDING' && isStudent) {
        actions = `<button class="btn btn-danger btn-sm" onclick="cancelSession(${s.sessionId})">Cancel</button>`;
    } else if (s.status === 'CONFIRMED' && isTutor) {
        actions = `
            <button class="btn btn-primary btn-sm" onclick="completeSession(${s.sessionId})">✅ Mark Complete</button>
            <button class="btn btn-danger btn-sm" onclick="cancelSession(${s.sessionId})">Cancel</button>
        `;
    } else if (s.status === 'CONFIRMED' && isStudent) {
        actions = `<button class="btn btn-danger btn-sm" onclick="cancelSession(${s.sessionId})">Cancel</button>`;
    } else if (s.status === 'COMPLETED' && isStudent) {
        actions = `<button class="btn btn-primary btn-sm" onclick="openFeedbackModal(${s.sessionId}, ${s.tutorId})">⭐ Rate</button>`;
    }

    return `
        <div class="session-card">
            <div class="session-info">
                <h4>${s.subjectName || 'Subject'} 
                    <span class="status-badge status-${s.status}">${s.status}</span>
                </h4>
                <p>${isTutor ? '👨‍🎓 Student: ' + s.studentName : '👨‍🏫 Tutor: ' + s.tutorName}</p>
                <p>📅 ${formatTime(s.startTime)} → ${formatTime(s.endTime)}</p>
            </div>
            <div class="session-actions">${actions}</div>
        </div>
    `;
}

async function acceptSession(sessionId) {
    const res = await apiPost('/api/booking/accept', { sessionId });
    if (res.success) {
        showToast('Session confirmed! 🎉', 'success');
        loadSessions();
        loadDashboard();
    }
}

async function cancelSession(sessionId) {
    const res = await apiPost('/api/booking/cancel', { sessionId });
    if (res.success) {
        showToast('Session cancelled.', 'success');
        loadSessions();
        loadDashboard();
    }
}

async function completeSession(sessionId) {
    const res = await apiPost('/api/booking/complete', { sessionId });
    if (res.success) {
        showToast('Session completed! Student will be asked to rate. ⭐', 'success');
        loadSessions();
    }
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

    const res = await apiPost('/api/feedback', {
        sessionId: currentFeedbackSession.sessionId,
        studentId: currentUser.userId,
        tutorId: currentFeedbackSession.tutorId,
        rating: selectedRating,
        comment: document.getElementById('feedbackComment').value
    });

    if (res.success) {
        closeModal();
        showToast('Thanks for your feedback! ⭐', 'success');
        loadSessions();
    } else {
        showToast(res.error || 'Feedback failed', 'error');
    }
}

// ─── TUTOR SUBJECT MANAGEMENT ───
async function loadTutorSubjects() {
    if (!currentUser || currentUser.role !== 'TUTOR') return;

    // Load subjects the tutor teaches
    const mySubjects = await apiGet('/api/tutor/subjects?tutorId=' + currentUser.userId);
    const mySubjectsContainer = document.getElementById('mySubjects');

    if (mySubjects.length === 0) {
        mySubjectsContainer.innerHTML = '<p class="empty-state" style="padding:16px">No subjects added yet.</p>';
    } else {
        mySubjectsContainer.innerHTML = mySubjects.map(s =>
            `<span class="subject-chip">${s.subjectCode} - ${s.name}</span>`
        ).join('');
    }

    // Load all available subjects
    const allSubjects = await apiGet('/api/subjects');
    const mySubjectIds = mySubjects.map(s => s.subjectId);
    const available = allSubjects.filter(s => !mySubjectIds.includes(s.subjectId));

    const container = document.getElementById('availableSubjects');
    if (available.length === 0) {
        container.innerHTML = '<p class="empty-state" style="padding:16px">You\'re teaching all available subjects! 🏆</p>';
    } else {
        container.innerHTML = available.map(s => `
            <div class="subject-list-item">
                <span>${s.subjectCode} - ${s.name} (Sem ${s.semester})</span>
                <button class="btn btn-primary btn-sm" onclick="addSubjectToTeach(${s.subjectId})">+ Add</button>
            </div>
        `).join('');
    }
}

async function addSubjectToTeach(subjectId) {
    const res = await apiPost('/api/tutor/subjects', {
        tutorId: currentUser.userId,
        subjectId: subjectId
    });
    if (res.success) {
        showToast('Subject added! 📚', 'success');
        loadTutorSubjects();
    }
}

// ─── ADMIN DASHBOARD ───
async function loadAdminDashboard() {
    if (!currentUser || currentUser.role !== 'ADMIN') return;
    const users = await apiGet('/api/admin/unverified');
    const container = document.getElementById('adminPendingList');

    if (users.length === 0) {
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
                <button class="btn btn-success btn-sm" onclick="verifyUser(${u.userId})">Approve</button>
                <button class="btn btn-danger btn-sm" onclick="rejectUser(${u.userId})">Reject</button>
            </div>
        </div>
    `).join('');
}

async function verifyUser(userId) {
    const res = await apiPost('/api/admin/verify', { userId });
    if (res.success) {
        showToast('Account verified! ✅', 'success');
        loadAdminDashboard();
    }
}

async function rejectUser(userId) {
    if (!confirm('Are you sure you want to reject and delete this account?')) return;
    const res = await apiPost('/api/admin/reject', { userId });
    if (res.success) {
        showToast('Account rejected.', 'success');
        loadAdminDashboard();
    }
}

// ─── NOTIFICATIONS ───
async function loadNotifications() {
    if (!currentUser) return;
    const data = await apiGet('/api/notifications?userId=' + currentUser.userId);
    const badge = document.getElementById('notifBadge');
    if (data.unread > 0) {
        badge.textContent = data.unread;
        badge.style.display = 'flex';
    } else {
        badge.style.display = 'none';
    }
}

async function showNotifications() {
    const panel = document.getElementById('notifPanel');
    if (panel.style.display === 'block') { panel.style.display = 'none'; return; }

    const data = await apiGet('/api/notifications?userId=' + currentUser.userId);
    const list = document.getElementById('notifList');

    if (data.items.length === 0) {
        list.innerHTML = '<p class="empty-state">No notifications yet.</p>';
    } else {
        list.innerHTML = data.items.map(n => `
            <div class="notif-item ${n.status}">
                ${n.message}
                <div class="notif-time">${n.time}</div>
            </div>
        `).join('');
    }

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

// ─── API HELPERS ───
async function apiGet(url) {
    try {
        const res = await fetch(url);
        return await res.json();
    } catch (err) {
        console.error('API GET error:', err);
        return [];
    }
}

async function apiPost(url, data) {
    try {
        const body = Object.entries(data).map(([k, v]) =>
            encodeURIComponent(k) + '=' + encodeURIComponent(v)
        ).join('&');

        const res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body
        });
        return await res.json();
    } catch (err) {
        console.error('API POST error:', err);
        return { error: 'Server not reachable' };
    }
}

// ─── SEED DATA (for demo) ───
async function seedData() {
    await apiPost('/api/seed', {});

    // Register Admin manually for demo (usually exists in system)
    // We register it via AuthController implicitly but let's just make sure 
    // it's clear how to access it.
    console.log("Admin setup: admin@pesu.pes.edu / admin123");

    showToast('Sample data loaded! 🎉', 'success');
}

// ─── INIT ───
document.addEventListener('DOMContentLoaded', () => {
    // Auto-load seed data on first visit
    fetch('/api/subjects').then(r => r.json()).then(subjects => {
        if (subjects.length === 0) {
            seedData();
        }
    }).catch(() => { });
});
