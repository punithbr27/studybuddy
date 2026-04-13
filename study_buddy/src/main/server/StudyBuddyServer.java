package main.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import main.pattern.DatabaseManager;
import main.controller.*;
import main.model.*;
import main.pattern.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * StudyBuddyServer — HTTP Server using Java's built-in HttpServer.
 * Serves REST API endpoints and static frontend files.
 */
public class StudyBuddyServer {

    private HttpServer server;
    private AuthController authController;
    private SubjectController subjectController;
    private SearchController searchController;
    private BookingController bookingController;
    private FeedbackController feedbackController;

    public StudyBuddyServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        authController = new AuthController();
        subjectController = new SubjectController();
        searchController = new SearchController();
        bookingController = new BookingController();
        feedbackController = new FeedbackController();

        // API endpoints
        server.createContext("/api/register", this::handleRegister);
        server.createContext("/api/login", this::handleLogin);
        server.createContext("/api/subjects", this::handleSubjects);
        server.createContext("/api/tutor/subjects", this::handleTutorSubjects);
        server.createContext("/api/search", this::handleSearch);
        server.createContext("/api/booking", this::handleBooking);
        server.createContext("/api/booking/accept", this::handleAcceptBooking);
        server.createContext("/api/booking/cancel", this::handleCancelBooking);
        server.createContext("/api/booking/complete", this::handleCompleteBooking);
        server.createContext("/api/sessions", this::handleSessions);
        server.createContext("/api/feedback", this::handleFeedback);
        server.createContext("/api/notifications", this::handleNotifications);
        server.createContext("/api/seed", this::handleSeedData);

        // Admin Endpoints
        server.createContext("/api/admin/unverified", this::handleGetUnverifiedUsers);
        server.createContext("/api/admin/verify", this::handleVerifyUser);
        server.createContext("/api/admin/reject", this::handleRejectUser);

        // Static files (HTML, CSS, JS)
        server.createContext("/", this::handleStaticFiles);

        server.setExecutor(null);
    }

    // ─── CORS Preflight Handler ───
    private void handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    public void start() {
        server.start();
        System.out.println("🌐 Server started at http://localhost:" + server.getAddress().getPort());
    }

    // ═══════════════════════════════════════════
    // API HANDLERS
    // ═══════════════════════════════════════════

    private void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> params = parseBody(exchange);
        User user = authController.register(
                params.getOrDefault("role", "STUDENT"),
                params.getOrDefault("name", ""),
                params.getOrDefault("email", ""),
                params.getOrDefault("password", ""),
                Integer.parseInt(params.getOrDefault("semester", "1")),
                Double.parseDouble(params.getOrDefault("cgpa", "0")));
        if (user != null) {
            sendResponse(exchange, 200, "{\"success\":true,\"userId\":" + user.getUserId() +
                    ",\"name\":\"" + user.getName() + "\",\"email\":\"" + user.getEmail() +
                    "\",\"role\":\"" + user.getRole() + "\"}");
        } else {
            sendResponse(exchange, 400, "{\"error\":\"Registration failed. Email may already exist.\"}");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> params = parseBody(exchange);
        User user = authController.login(
                params.getOrDefault("email", ""),
                params.getOrDefault("password", ""));
        if (user != null) {
            String extra = "";
            if (user instanceof Tutor) {
                double rating = feedbackController.getTutorRating(user.getUserId());
                extra = ",\"rating\":" + String.format("%.1f", rating);
            }
            String response = "{\"success\":true,\"userId\":" + user.getUserId() +
                    ",\"name\":\"" + user.getName() + "\",\"email\":\"" + user.getEmail() + "\"" + extra + "," +
                    "\"role\":\"" + user.getRole() + "\"," +
                    "\"verified\":" + user.isVerified() + "," +
                    "\"semester\":" + (user instanceof Student ? ((Student) user).getSemester() : 0) + "," +
                    "\"cgpa\":" + (user instanceof Student ? ((Student) user).getCgpa() : 0.0) +
                    "}";
            sendResponse(exchange, 200, response);
        } else {
            sendResponse(exchange, 401, "{\"success\":false, \"error\":\"Invalid email or password.\"}");
        }
    }

    // ─── ADMIN ENDPOINTS ───

    private void handleGetUnverifiedUsers(HttpExchange exchange) throws IOException {
        String sql = "SELECT * FROM users WHERE verified = 0 AND role != 'ADMIN'";
        List<Map<String, Object>> users = new ArrayList<>();
        try (PreparedStatement pstmt = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> u = new HashMap<>();
                u.put("userId", rs.getInt("user_id"));
                u.put("name", rs.getString("name"));
                u.put("email", rs.getString("email"));
                u.put("role", rs.getString("role"));
                users.add(u);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendJsonResponse(exchange, users);
    }

    private void handleVerifyUser(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseBody(exchange);
        int userId = Integer.parseInt(params.get("userId"));
        String sql = "UPDATE users SET verified = 1 WHERE user_id = ?";
        try (PreparedStatement pstmt = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            sendResponse(exchange, 200, "{\"success\":true}");
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"success\":false, \"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleRejectUser(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseBody(exchange);
        int userId = Integer.parseInt(params.get("userId"));
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            sendResponse(exchange, 200, "{\"success\":true}");
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"success\":false, \"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleSubjects(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            List<Subject> subjects = subjectController.getAllSubjects();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < subjects.size(); i++) {
                Subject s = subjects.get(i);
                if (i > 0)
                    json.append(",");
                json.append("{\"subjectId\":").append(s.getSubjectId())
                        .append(",\"subjectCode\":\"").append(s.getSubjectCode())
                        .append("\",\"name\":\"").append(s.getName())
                        .append("\",\"semester\":").append(s.getSemester()).append("}");
            }
            json.append("]");
            sendResponse(exchange, 200, json.toString());
        } else if ("POST".equals(exchange.getRequestMethod())) {
            Map<String, String> params = parseBody(exchange);
            Subject subject = subjectController.addSubject(
                    params.getOrDefault("subjectCode", ""),
                    params.getOrDefault("name", ""),
                    Integer.parseInt(params.getOrDefault("semester", "1")));
            if (subject != null) {
                sendResponse(exchange, 200, "{\"success\":true,\"subjectId\":" + subject.getSubjectId() + "}");
            } else {
                sendResponse(exchange, 400, "{\"error\":\"Failed to add subject\"}");
            }
        }
    }

    private void handleTutorSubjects(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            Map<String, String> params = parseBody(exchange);
            boolean result = subjectController.linkTutorToSubject(
                    Integer.parseInt(params.getOrDefault("tutorId", "0")),
                    Integer.parseInt(params.getOrDefault("subjectId", "0")));
            sendResponse(exchange, 200, "{\"success\":" + result + "}");
        } else if ("GET".equals(exchange.getRequestMethod())) {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);
            int tutorId = Integer.parseInt(params.getOrDefault("tutorId", "0"));
            List<Subject> subjects = subjectController.getSubjectsByTutor(tutorId);
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < subjects.size(); i++) {
                Subject s = subjects.get(i);
                if (i > 0)
                    json.append(",");
                json.append("{\"subjectId\":").append(s.getSubjectId())
                        .append(",\"subjectCode\":\"").append(s.getSubjectCode())
                        .append("\",\"name\":\"").append(s.getName())
                        .append("\",\"semester\":").append(s.getSemester()).append("}");
            }
            json.append("]");
            sendResponse(exchange, 200, json.toString());
        }
    }

    private void handleSearch(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query);
        String keyword = params.getOrDefault("keyword", "");
        String strategy = params.getOrDefault("strategy", "subject");

        List<Tutor> tutors;
        switch (strategy.toLowerCase()) {
            case "rating":
                tutors = searchController.searchByRating(keyword);
                break;
            case "availability":
                tutors = searchController.searchByAvailability(keyword);
                break;
            default:
                tutors = searchController.searchBySubject(keyword);
        }

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < tutors.size(); i++) {
            Tutor t = tutors.get(i);
            if (i > 0)
                json.append(",");
            // Get subjects for this tutor
            List<Subject> subs = subjectController.getSubjectsByTutor(t.getUserId());
            StringBuilder subsJson = new StringBuilder("[");
            for (int j = 0; j < subs.size(); j++) {
                if (j > 0)
                    subsJson.append(",");
                subsJson.append("\"").append(subs.get(j).getName()).append("\"");
            }
            subsJson.append("]");

            json.append("{\"userId\":").append(t.getUserId())
                    .append(",\"name\":\"").append(t.getName())
                    .append("\",\"email\":\"").append(t.getEmail())
                    .append("\",\"semester\":").append(t.getSemester())
                    .append(",\"cgpa\":").append(t.getCgpa())
                    .append(",\"rating\":").append(String.format("%.1f", t.getReputationScore()))
                    .append(",\"totalRatings\":").append(t.getTotalRatings())
                    .append(",\"subjects\":").append(subsJson)
                    .append("}");
        }
        json.append("]");
        sendResponse(exchange, 200, json.toString());
    }

    private void handleBooking(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> params = parseBody(exchange);
        Session session = bookingController.createBooking(
                Integer.parseInt(params.getOrDefault("tutorId", "0")),
                Integer.parseInt(params.getOrDefault("studentId", "0")),
                Integer.parseInt(params.getOrDefault("subjectId", "0")),
                params.getOrDefault("startTime", ""),
                params.getOrDefault("endTime", ""));
        if (session != null) {
            sendResponse(exchange, 200, "{\"success\":true,\"sessionId\":" + session.getSessionId() + "}");
        } else {
            sendResponse(exchange, 400, "{\"error\":\"Booking failed. Possible time conflict.\"}");
        }
    }

    private void handleAcceptBooking(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseBody(exchange);
        int sessionId = Integer.parseInt(params.getOrDefault("sessionId", "0"));
        boolean result = bookingController.acceptBooking(sessionId);
        sendResponse(exchange, 200, "{\"success\":" + result + "}");
    }

    private void handleCancelBooking(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseBody(exchange);
        int sessionId = Integer.parseInt(params.getOrDefault("sessionId", "0"));
        boolean result = bookingController.cancelBooking(sessionId);
        sendResponse(exchange, 200, "{\"success\":" + result + "}");
    }

    private void handleCompleteBooking(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseBody(exchange);
        int sessionId = Integer.parseInt(params.getOrDefault("sessionId", "0"));
        boolean result = bookingController.completeSession(sessionId);
        sendResponse(exchange, 200, "{\"success\":" + result + "}");
    }

    private void handleSessions(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query);
        int userId = Integer.parseInt(params.getOrDefault("userId", "0"));

        List<Session> sessions = bookingController.getSessionsForUser(userId);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < sessions.size(); i++) {
            Session s = sessions.get(i);
            if (i > 0)
                json.append(",");
            json.append("{\"sessionId\":").append(s.getSessionId())
                    .append(",\"tutorId\":").append(s.getTutorId())
                    .append(",\"studentId\":").append(s.getStudentId())
                    .append(",\"subjectId\":").append(s.getSubjectId())
                    .append(",\"startTime\":\"").append(s.getStartTime())
                    .append("\",\"endTime\":\"").append(s.getEndTime())
                    .append("\",\"status\":\"").append(s.getStatus())
                    .append("\",\"tutorName\":\"").append(s.getTutorName() != null ? s.getTutorName() : "")
                    .append("\",\"studentName\":\"").append(s.getStudentName() != null ? s.getStudentName() : "")
                    .append("\",\"subjectName\":\"").append(s.getSubjectName() != null ? s.getSubjectName() : "")
                    .append("\",\"hasFeedback\":").append(s.hasFeedback())
                    .append("}");
        }
        json.append("]");
        sendResponse(exchange, 200, json.toString());
    }

    private void handleFeedback(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            Map<String, String> params = parseBody(exchange);
            Feedback feedback = feedbackController.submitFeedback(
                    Integer.parseInt(params.getOrDefault("sessionId", "0")),
                    Integer.parseInt(params.getOrDefault("studentId", "0")),
                    Integer.parseInt(params.getOrDefault("tutorId", "0")),
                    Integer.parseInt(params.getOrDefault("rating", "5")),
                    params.getOrDefault("comment", ""));
            if (feedback != null) {
                sendResponse(exchange, 200, "{\"success\":true}");
            } else {
                sendResponse(exchange, 400, "{\"error\":\"Feedback failed\"}");
            }
        }
    }

    private void handleNotifications(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query);
        int userId = Integer.parseInt(params.getOrDefault("userId", "0"));

        NotificationService ns = bookingController.getNotificationService();
        List<String[]> notifs = ns.getNotifications(userId);
        int unread = ns.getUnreadCount(userId);
        ns.markAllRead(userId);

        StringBuilder json = new StringBuilder("{\"unread\":").append(unread).append(",\"items\":[");
        for (int i = 0; i < notifs.size(); i++) {
            if (i > 0)
                json.append(",");
            json.append("{\"message\":\"").append(notifs.get(i)[0].replace("\"", "\\\""))
                    .append("\",\"status\":\"").append(notifs.get(i)[1])
                    .append("\",\"time\":\"").append(notifs.get(i)[2]).append("\"}");
        }
        json.append("]}");
        sendResponse(exchange, 200, json.toString());
    }

    private void handleSeedData(HttpExchange exchange) throws IOException {
        // Add sample subjects
        subjectController.addSubject("CS301", "Data Structures", 3);
        subjectController.addSubject("CS302", "Algorithms", 3);
        subjectController.addSubject("CS501", "Machine Learning", 5);
        subjectController.addSubject("CS502", "Cloud Computing", 5);
        subjectController.addSubject("CS601", "OOAD", 6);
        subjectController.addSubject("CS602", "Big Data", 6);
        subjectController.addSubject("CS603", "Computer Networks", 6);
        subjectController.addSubject("MA301", "Discrete Mathematics", 3);

        // Add sample tutors
        User t1 = authController.register("TUTOR", "Aarav Sharma", "aarav@pesu.pes.edu", "pass123", 6, 9.2);
        User t2 = authController.register("TUTOR", "Priya Patel", "priya@pesu.pes.edu", "pass123", 6, 8.8);
        User t3 = authController.register("TUTOR", "Rahul Kumar", "rahul@pesu.pes.edu", "pass123", 5, 9.0);

        // Link tutors to subjects
        if (t1 != null) {
            subjectController.linkTutorToSubject(t1.getUserId(), 5); // OOAD
            subjectController.linkTutorToSubject(t1.getUserId(), 1); // DS
            subjectController.linkTutorToSubject(t1.getUserId(), 6); // Big Data
        }
        if (t2 != null) {
            subjectController.linkTutorToSubject(t2.getUserId(), 5); // OOAD
            subjectController.linkTutorToSubject(t2.getUserId(), 3); // ML
            subjectController.linkTutorToSubject(t2.getUserId(), 7); // CN
        }
        if (t3 != null) {
            subjectController.linkTutorToSubject(t3.getUserId(), 2); // Algo
            subjectController.linkTutorToSubject(t3.getUserId(), 3); // ML
            subjectController.linkTutorToSubject(t3.getUserId(), 4); // Cloud
        }

        // Add a sample student
        authController.register("STUDENT", "Punith BR", "punith@pesu.pes.edu", "pass123", 6, 8.5);

        // Add an Admin
        authController.register("ADMIN", "System Admin", "admin@pesu.pes.edu", "admin123", 0, 0);

        sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Sample data loaded!\"}");
    }

    // ═══════════════════════════════════════════
    // STATIC FILE SERVER
    // ═══════════════════════════════════════════

    private void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/"))
            path = "/index.html";

        File file = new File("web" + path);
        if (!file.exists()) {
            sendResponse(exchange, 404, "File not found");
            return;
        }

        // Set content type
        String contentType = "text/html";
        if (path.endsWith(".css"))
            contentType = "text/css";
        else if (path.endsWith(".js"))
            contentType = "application/javascript";
        else if (path.endsWith(".png"))
            contentType = "image/png";
        else if (path.endsWith(".jpg"))
            contentType = "image/jpeg";

        byte[] bytes = Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    // ═══════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        // Handle CORS preflight
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleCors(exchange);
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        byte[] bytes = body.getBytes("UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private void sendJsonResponse(HttpExchange exchange, List<Map<String, Object>> list) throws IOException {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0)
                json.append(",");
            json.append("{");
            Map<String, Object> map = list.get(i);
            int j = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (j > 0)
                    json.append(",");
                json.append("\"").append(entry.getKey()).append("\":");
                if (entry.getValue() instanceof String) {
                    json.append("\"").append(entry.getValue().toString().replace("\"", "\\\"")).append("\"");
                } else {
                    json.append(entry.getValue());
                }
                j++;
            }
            json.append("}");
        }
        json.append("]");
        sendResponse(exchange, 200, json.toString());
    }

    private Map<String, String> parseBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes());
        return parseFormData(body);
    }

    private Map<String, String> parseQuery(String query) {
        if (query == null || query.isEmpty())
            return new HashMap<>();
        return parseFormData(query);
    }

    private Map<String, String> parseFormData(String data) {
        Map<String, String> params = new HashMap<>();
        if (data == null || data.isEmpty())
            return params;
        for (String pair : data.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    params.put(java.net.URLDecoder.decode(kv[0], "UTF-8"),
                            java.net.URLDecoder.decode(kv[1], "UTF-8"));
                } catch (Exception e) {
                    params.put(kv[0], kv[1]);
                }
            }
        }
        return params;
    }
}
