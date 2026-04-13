/* ═══════════════════════════════════════════
   Study Buddy — Frontend JavaScript (Java Backend Edition)
   ═══════════════════════════════════════════ */

// ─── STATE ───
let currentUser = null;
let currentStrategy = 'subject';
let selectedRating = 0;
let currentBookingTutor = null;
let currentFeedbackSession = null;

// ─── API WRAPPER ───
const API = {
    async request(endpoint, method = 'GET', data = null) {
        const options = {
            method,
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        };

        if (data) {
            const params = new URLSearchParams();
            for (const key in data) {
                params.append(key, data[key]);
            }
            if (method === 'GET') {
                endpoint += '?' + params.toString();
            } else {
                options.body = params.toString();
            }
        }

        try {
            const response = await fetch(endpoint, options);
            const result = await response.json();
            if (!response.ok) throw new Error(result.error || 'Server error');
            return result;
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    }
};

// ─── PAGE NAVIGATION ───
function showPage(pageName) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.getElementById('page-' + pageName).classList.add('active');
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));

    // Save current page for restoration on reload
    if (pageName !== 'login' && pageName !== 'register' && pageName !== 'unverified') {
        localStorage.setItem('studybuddy_page', pageName);
    }

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
    const semester = document.getElementById('regSemester').value;
    const cgpa = document.getElementById('regCgpa').value;

    if (!email.toLowerCase().endsWith('@pesu.pes.edu')) {
        showMsg('registerMsg', 'Only @pesu.pes.edu emails are allowed!', 'error');
        return;
    }

    try {
        const result = await API.request('/api/register', 'POST', {
            name, email, password, role, semester, cgpa
        });
        showMsg('registerMsg', 'Account created! Logging you in...', 'success');
        setTimeout(() => loginAs({
            userId: result.userId, name: result.name, email: result.email,
            role: result.role, verified: false, semester: parseInt(semester), cgpa: parseFloat(cgpa)
        }), 1000);
    } catch (err) {
        showMsg('registerMsg', err.message, 'error');
    }
}

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;

    try {
        const result = await API.request('/api/login', 'POST', { email, password });
        loginAs(result);
    } catch (err) {
        showMsg('loginMsg', err.message, 'error');
    }
}

function loginAs(user, targetPage = null) {
    currentUser = user;
    localStorage.setItem('studybuddy_user', JSON.stringify(user));
    document.getElementById('userName').textContent = user.name;
    const badge = document.getElementById('userRoleBadge');
    badge.textContent = user.role;
    badge.className = 'user-role-badge ' + user.role;
    document.getElementById('navUser').style.display = 'flex';

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
        showPage(targetPage || 'admin');
    } else {
        navLinks.innerHTML = `
            <a href="#" onclick="showPage('dashboard')" class="nav-link active">Dashboard</a>
            <a href="#" onclick="showPage('search')" class="nav-link">Find Tutors</a>
            <a href="#" onclick="showPage('sessions')" class="nav-link">Sessions</a>
        `;
        showPage(targetPage || 'dashboard');
    }

    if (user.role === 'TUTOR') {
        document.querySelectorAll('.tutor-only').forEach(el => el.style.display = 'block');
    } else {
        document.querySelectorAll('.tutor-only').forEach(el => el.style.display = 'none');
    }

    showToast('Welcome, ' + user.name + '! 🎉', 'success');
    loadNotifications();
}

function logout() {
    currentUser = null;
    localStorage.removeItem('studybuddy_user');
    localStorage.removeItem('studybuddy_page');
    document.getElementById('navUser').style.display = 'none';
    document.getElementById('navLinks').innerHTML = '<a href="#" onclick="showPage(\'home\')" class="nav-link active">Home</a>';
    document.querySelectorAll('.tutor-only').forEach(el => el.style.display = 'none');
    showPage('home');
}

// ─── DASHBOARD ───
async function loadDashboard() {
    if (!currentUser) return;
    document.getElementById('dashWelcome').textContent =
        `Welcome back, ${currentUser.name}! You are logged in as ${currentUser.role.toLowerCase()}.`;

    try {
        const sessions = await API.request('/api/sessions', 'GET', { userId: currentUser.userId });
        const container = document.getElementById('recentSessions');
        if (sessions.length === 0) {
            container.innerHTML = '<p class="empty-state">No sessions yet. Start by finding a tutor! 🔍</p>';
            return;
        }
        container.innerHTML = sessions.slice(0, 5).map(s => renderSessionCard(s)).join('');
    } catch (err) {
        console.error('Failed to load dashboard:', err);
    }
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
    if (!keyword.trim() && currentStrategy === 'subject') {
        document.getElementById('searchResults').innerHTML = '<p class="empty-state">Enter a subject to find tutors 🎯</p>';
        return;
    }

    try {
        const tutors = await API.request('/api/search', 'GET', { keyword, strategy: currentStrategy });
        const container = document.getElementById('searchResults');
        if (tutors.length === 0) {
            container.innerHTML = `<p class="empty-state">No tutors found for "${keyword}" 😕<br>Try a different subject or strategy.</p>`;
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
    } catch (err) {
        showToast('Search failed: ' + err.message, 'error');
    }
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

    try {
        const subjects = await API.request('/api/tutor/subjects', 'GET', { tutorId: tutor.userId });
        const select = document.getElementById('bookSubject');
        select.innerHTML = subjects.map(s =>
            `<option value="${s.subjectId}">${s.subjectCode} - ${s.name}</option>`
        ).join('');

        const now = new Date();
        now.setMinutes(0);
        now.setHours(now.getHours() + 1);
        document.getElementById('bookStart').value = now.toISOString().slice(0, 16);
        now.setHours(now.getHours() + 1);
        document.getElementById('bookEnd').value = now.toISOString().slice(0, 16);

        document.getElementById('bookingModal').style.display = 'flex';
    } catch (err) {
        showToast('Failed to load tutor subjects', 'error');
    }
}

async function submitBooking(e) {
    e.preventDefault();
    try {
        await API.request('/api/booking', 'POST', {
            tutorId: currentBookingTutor.userId,
            studentId: currentUser.userId,
            subjectId: document.getElementById('bookSubject').value,
            startTime: document.getElementById('bookStart').value,
            endTime: document.getElementById('bookEnd').value
        });
        closeModal();
        showToast('Session booked! Waiting for tutor confirmation. 📅', 'success');
        showPage('sessions');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ─── SESSIONS ───
async function loadSessions() {
    if (!currentUser) return;
    try {
        const sessions = await API.request('/api/sessions', 'GET', { userId: currentUser.userId });
        renderSessions(sessions);
    } catch (err) {
        console.error('Failed to load sessions:', err);
    }
}

function filterSessions(btn, status) {
    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    loadSessions().then(() => {
        // Simple client-side filter for now
        if (status !== 'all') {
            const list = document.getElementById('sessionsList');
            const cards = list.querySelectorAll('.session-card');
            cards.forEach(card => {
                const badge = card.querySelector('.status-badge');
                if (badge.textContent.trim() !== status) {
                    card.style.display = 'none';
                } else {
                    card.style.display = 'flex';
                }
            });
        }
    });
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
    const isTutor = currentUser && currentUser.userId == s.tutorId;
    const isStudent = currentUser && currentUser.userId == s.studentId;

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
        if (!s.hasFeedback) {
            actions = `<button class="btn btn-primary btn-sm" onclick="openFeedbackModal(${s.sessionId}, ${s.tutorId})">⭐ Rate</button>`;
        } else {
            actions = `<span style="color:var(--success); font-weight:600;">Rated: ★</span>`;
        }
    }

    return `
        <div class="session-card">
            <div class="session-info">
                <h4>${s.subjectName}
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
    try {
        await API.request('/api/booking/accept', 'POST', { sessionId });
        showToast('Session confirmed! 🎉', 'success');
        loadSessions();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function cancelSession(sessionId) {
    try {
        await API.request('/api/booking/cancel', 'POST', { sessionId });
        showToast('Session cancelled.', 'success');
        loadSessions();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function completeSession(sessionId) {
    try {
        await API.request('/api/booking/complete', 'POST', { sessionId });
        showToast('Session completed! Student will be asked to rate. ⭐', 'success');
        loadSessions();
    } catch (err) {
        showToast(err.message, 'error');
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

    try {
        await API.request('/api/feedback', 'POST', {
            sessionId: currentFeedbackSession.sessionId,
            studentId: currentUser.userId,
            tutorId: currentFeedbackSession.tutorId,
            rating: selectedRating,
            comment: document.getElementById('feedbackComment').value
        });
        closeModal();
        showToast('Thanks for your feedback! ⭐', 'success');
        loadSessions();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ─── TUTOR SUBJECT MANAGEMENT ───
async function loadTutorSubjects() {
    if (!currentUser || currentUser.role !== 'TUTOR') return;

    try {
        const mySubjects = await API.request('/api/tutor/subjects', 'GET', { tutorId: currentUser.userId });
        const mySubjectsContainer = document.getElementById('mySubjects');
        if (mySubjects.length === 0) {
            mySubjectsContainer.innerHTML = '<p class="empty-state" style="padding:16px">No subjects added yet.</p>';
        } else {
            mySubjectsContainer.innerHTML = mySubjects.map(s =>
                `<span class="subject-chip">${s.subjectCode} - ${s.name}</span>`
            ).join('');
        }

        const allSubjects = await API.request('/api/subjects', 'GET');
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
    } catch (err) {
        console.error('Tutor subject management error:', err);
    }
}

async function addSubjectToTeach(subjectId) {
    try {
        await API.request('/api/tutor/subjects', 'POST', {
            tutorId: currentUser.userId,
            subjectId: subjectId
        });
        showToast('Subject added! 📚', 'success');
        loadTutorSubjects();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ─── ADMIN DASHBOARD ───
async function loadAdminDashboard() {
    if (!currentUser || currentUser.role !== 'ADMIN') return;

    try {
        const unverified = await API.request('/api/admin/unverified', 'GET');
        const container = document.getElementById('adminPendingList');

        // Add Seed Button if Admin
        if (!document.getElementById('seedBtn')) {
            const h2 = container.previousElementSibling;
            const btn = document.createElement('button');
            btn.id = 'seedBtn';
            btn.className = 'btn btn-outline btn-sm';
            btn.style.float = 'right';
            btn.textContent = '🌱 Seed Sample Data';
            btn.onclick = seedData;
            h2.appendChild(btn);
        }

        if (unverified.length === 0) {
            container.innerHTML = '<p class="empty-state">No pending verifications. Good job! 🛡️</p>';
            return;
        }

        container.innerHTML = unverified.map(u => `
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
    } catch (err) {
        console.error('Admin dashboard error:', err);
    }
}

async function verifyUser(userId) {
    try {
        await API.request('/api/admin/verify', 'POST', { userId });
        showToast('Account verified! ✅', 'success');
        loadAdminDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function rejectUser(userId) {
    if (!confirm('Are you sure you want to reject and delete this account?')) return;
    try {
        await API.request('/api/admin/reject', 'POST', { userId });
        showToast('Account rejected.', 'success');
        loadAdminDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function seedData() {
    try {
        const result = await API.request('/api/seed', 'POST');
        showToast(result.message, 'success');
        loadAdminDashboard();
    } catch (err) {
        showToast('Seed failed: ' + err.message, 'error');
    }
}

// ─── NOTIFICATIONS ───
async function loadNotifications() {
    if (!currentUser) return;
    try {
        const result = await API.request('/api/notifications', 'GET', { userId: currentUser.userId });
        const badge = document.getElementById('notifBadge');
        if (result.unread > 0) {
            badge.textContent = result.unread;
            badge.style.display = 'flex';
        } else {
            badge.style.display = 'none';
        }
    } catch (err) {
        console.error('Failed to load notifications:', err);
    }
}

async function showNotifications() {
    const panel = document.getElementById('notifPanel');
    if (panel.style.display === 'block') { panel.style.display = 'none'; return; }

    try {
        const result = await API.request('/api/notifications', 'GET', { userId: currentUser.userId });
        const list = document.getElementById('notifList');
        if (result.items.length === 0) {
            list.innerHTML = '<p class="empty-state">No notifications yet.</p>';
        } else {
            list.innerHTML = result.items.map(n => `
                <div class="notif-item ${n.status === 'UNREAD' ? 'unread' : ''}">
                    ${n.message}
                    <div class="notif-time">${n.time}</div>
                </div>
            `).join('');
        }
        panel.style.display = 'block';
        document.getElementById('notifBadge').style.display = 'none';
    } catch (err) {
        console.error('Failed to show notifications:', err);
    }
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
        const options = { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' };
        return d.toLocaleDateString('en-IN', options);
    } catch { return dt; }
}

// ─── INIT ───
document.addEventListener('DOMContentLoaded', () => {
    console.log('Study Buddy loaded with Java Backend');

    // Check for persisted session
    const savedUser = localStorage.getItem('studybuddy_user');
    const savedPage = localStorage.getItem('studybuddy_page');

    if (savedUser) {
        try {
            const user = JSON.parse(savedUser);
            loginAs(user, savedPage);
        } catch (e) {
            console.error('Failed to restore session:', e);
            localStorage.removeItem('studybuddy_user');
            localStorage.removeItem('studybuddy_page');
            showPage('home');
        }
    } else {
        showPage('home');
    }
});
