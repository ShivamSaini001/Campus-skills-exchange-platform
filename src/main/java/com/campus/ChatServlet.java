package com.campus;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/chat")
public class ChatServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // Check login.
        if (session == null || session.getAttribute("studentId") == null) {
            response.sendRedirect("index.html?message=Please%20login%20first");
            return;
        }

        int studentId = (Integer) session.getAttribute("studentId");

        int requestId;

        try {
            requestId = Integer.parseInt(request.getParameter("requestId"));
        } catch (NumberFormatException e) {
            response.sendRedirect("my-requests");
            return;
        }

        response.setContentType("text/html;charset=UTF-8");

        try (Connection con = DBconn.getConnection()) {

            // Verify that the logged-in student belongs to this
            // accepted skill request.
            String checkSql
                    = "SELECT er.from_student, er.to_student, "
                    + "st1.full_name AS learner_name, "
                    + "st2.full_name AS teacher_name "
                    + "FROM exchange_requests er "
                    + "JOIN students st1 ON er.from_student = st1.student_id "
                    + "JOIN students st2 ON er.to_student = st2.student_id "
                    + "WHERE er.request_id = ? "
                    + "AND er.status = 'ACCEPTED' "
                    + "AND (er.from_student = ? OR er.to_student = ?)";

            int learnerId;
            int teacherId;
            String learnerName;
            String teacherName;

            try (PreparedStatement ps = con.prepareStatement(checkSql)) {

                ps.setInt(1, requestId);
                ps.setInt(2, studentId);
                ps.setInt(3, studentId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        response.sendError(
                                HttpServletResponse.SC_FORBIDDEN,
                                "You are not allowed to access this chat."
                        );
                        return;
                    }

                    learnerId = rs.getInt("from_student");
                    teacherId = rs.getInt("to_student");
                    learnerName = rs.getString("learner_name");
                    teacherName = rs.getString("teacher_name");
                }
            }

            // Handle background requests for new messages.
            if ("true".equals(request.getParameter("ajax"))) {
                sendNewMessages(con, request, response, requestId);
                return;
            }

// Mark received messages as delivered and seen.
            String readSql
                    = "UPDATE chat_messages "
                    + "SET is_delivered = TRUE, "
                    + "delivered_at = COALESCE(delivered_at, CURRENT_TIMESTAMP), "
                    + "is_read = TRUE, "
                    + "read_at = COALESCE(read_at, CURRENT_TIMESTAMP) "
                    + "WHERE request_id = ? AND sender_id != ?";

            try (PreparedStatement ps = con.prepareStatement(readSql)) {

                ps.setInt(1, requestId);
                ps.setInt(2, studentId);

                ps.executeUpdate();
            }

            // Create a CSRF token for secure message submission.
            String csrfToken = (String) session.getAttribute("chatCsrfToken");

            if (csrfToken == null) {
                csrfToken = UUID.randomUUID().toString();
                session.setAttribute("chatCsrfToken", csrfToken);
            }

            try (PrintWriter out = response.getWriter()) {

                out.println("<!doctype html>");
                out.println("<html lang='en'>");
                out.println("<head>");

                out.println("<meta charset='UTF-8'>");
                out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
                out.println("<title>Private Chat</title>");
                out.println("<link rel='stylesheet' href='style.css'>");

                out.println("</head>");
                out.println("<body>");

                // Shared navbar.
                Navbar.printNavbar(out, session);

                out.println("<main class='page'>");

                out.println("<section class='welcome'>");
                out.println("<p class='eyebrow'>PRIVATE CONVERSATION</p>");
                out.println("<h1>Skill Exchange Chat</h1>");
                out.println("<p>Communicate with your skill exchange partner.</p>");
                out.println("</section>");

                out.println("<section class='chat-container'>");

                String partnerName;

                if (studentId == learnerId) {
                    partnerName = teacherName;
                } else {
                    partnerName = learnerName;
                }

                out.println("<div class='chat-header'>");
                out.println("<h3>" + escape(partnerName) + "</h3>");
                out.println("<p>Private conversation</p>");
                out.println("</div>");

                // Display all messages of this accepted request.
                String messageSql
                        = "SELECT cm.message_id, cm.sender_id, cm.message_text, "
                        + "cm.sent_at, cm.is_delivered, cm.is_read, cm.read_at, "
                        + "st.full_name AS sender_name "
                        + "FROM chat_messages cm "
                        + "JOIN students st ON cm.sender_id = st.student_id "
                        + "WHERE cm.request_id = ? "
                        + "ORDER BY cm.sent_at ASC, cm.message_id ASC";

                long lastMessageId = 0;
                try (PreparedStatement ps = con.prepareStatement(messageSql)) {

                    ps.setInt(1, requestId);

                    try (ResultSet rs = ps.executeQuery()) {

                        out.println("<div class='chat-messages' id='chatMessages' "
                                + "data-last-message-id='0' "
                                + "data-current-student-id='" + studentId + "'>");

                        boolean hasMessages = false;

                        while (rs.next()) {

                            hasMessages = true;
                            long messageId = rs.getLong("message_id");

                            if (messageId > lastMessageId) {
                                lastMessageId = messageId;
                            }

                            int senderId = rs.getInt("sender_id");

                            String messageClass;

                            if (senderId == studentId) {
                                messageClass = "chat-message sent";
                            } else {
                                messageClass = "chat-message received";
                            }

                            out.println("<div class='" + messageClass
                                    + "' data-message-id='" + messageId + "'>");

                            out.println("<p class='chat-sender'>"
                                    + escape(rs.getString("sender_name"))
                                    + "</p>");

                            out.println("<p>"
                                    + escape(rs.getString("message_text"))
                                    + "</p>");

                            out.println("<small>"
                                    + escape(String.valueOf(rs.getTimestamp("sent_at")))
                                    + "</small>");

                            if (senderId == studentId) {

                                String messageStatus;

                                if (rs.getBoolean("is_read")) {
                                    messageStatus = "Seen";
                                } else if (rs.getBoolean("is_delivered")) {
                                    messageStatus = "Delivered";
                                } else {
                                    messageStatus = "Sent";
                                }

                                out.println("<small class='message-status' id='status-"
                                        + messageId + "'>"
                                        + messageStatus + "</small>");
                            }

                            out.println("</div>");
                        }

                        out.println("<script>");
                        out.println("document.getElementById('chatMessages').dataset.lastMessageId = '"
                                + lastMessageId + "';");
                        out.println("</script>");

                        if (!hasMessages) {
                            out.println("<p class='empty-chat'>");
                            out.println("No messages yet. Start your conversation!");
                            out.println("</p>");
                        }

                        out.println("</div>");
                    }
                }

                // Message sending form.
                out.println("<form action='chat' method='post' class='chat-form'>");

                out.println("<input type='hidden' name='requestId' value='"
                        + requestId + "'>");

                out.println("<input type='hidden' name='csrfToken' value='"
                        + escape(csrfToken) + "'>");

                out.println("<textarea name='message' rows='3' "
                        + "maxlength='2000' required "
                        + "placeholder='Write your message...'></textarea>");

                out.println("<button type='submit'>Send Message</button>");

                out.println("</form>");

                out.println("</section>");

                out.println("</main>");
                out.println("<script>");
                out.println("(function () {");

                out.println("const chatBox = document.getElementById('chatMessages');");
                out.println("const requestId = '" + requestId + "';");
                out.println("const currentStudentId = Number(chatBox.dataset.currentStudentId);");
                out.println("let lastMessageId = Number(chatBox.dataset.lastMessageId);");
                out.println("let polling = false;");

                out.println("function addMessage(message) {");

                out.println("const emptyMessage = chatBox.querySelector('.empty-chat');");
                out.println("if (emptyMessage) emptyMessage.remove();");

                out.println("const wasNearBottom = chatBox.scrollHeight - chatBox.scrollTop - chatBox.clientHeight < 100;");

                out.println("const messageDiv = document.createElement('div');");
                out.println("messageDiv.className = message.senderId === currentStudentId "
                        + "? 'chat-message sent' : 'chat-message received';");

                out.println("const sender = document.createElement('p');");
                out.println("sender.className = 'chat-sender';");
                out.println("sender.textContent = message.senderName;");

                out.println("const messageText = document.createElement('p');");
                out.println("messageText.textContent = message.messageText;");

                out.println("const time = document.createElement('small');");
                out.println("time.textContent = message.sentAt;");

                out.println("messageDiv.appendChild(sender);");
                out.println("messageDiv.appendChild(messageText);");
                out.println("messageDiv.appendChild(time);");
                out.println("chatBox.appendChild(messageDiv);");

                out.println("lastMessageId = message.id;");

                out.println("if (wasNearBottom) {");
                out.println("chatBox.scrollTop = chatBox.scrollHeight;");
                out.println("}");

                out.println("}");

                out.println("async function loadNewMessages() {");

                out.println("if (polling) return;");
                out.println("polling = true;");

                out.println("try {");

                out.println("const url = 'chat?requestId=' + encodeURIComponent(requestId)"
                        + " + '&ajax=true&afterId=' + lastMessageId;");

                out.println("const result = await fetch(url, {");
                out.println("credentials: 'same-origin',");
                out.println("cache: 'no-store'");
                out.println("});");

                out.println("if (result.status === 403 || result.status === 401) {");
                out.println("clearInterval(chatTimer);");
                out.println("return;");
                out.println("}");

                out.println("if (!result.ok) return;");

                out.println("const messages = await result.json();");

                out.println("messages.forEach(function (message) {");

                out.println("if (message.type === 'status') {");

                out.println("const statusElement = document.getElementById('status-' + message.id);");

                out.println("if (statusElement) {");

                out.println("let status = 'Sent';");

                out.println("if (message.isRead) {");
                out.println("status = 'Seen';");
                out.println("} else if (message.isDelivered) {");
                out.println("status = 'Delivered';");
                out.println("}");

                out.println("statusElement.textContent = status;");
                out.println("}");

                out.println("} else if (message.id > lastMessageId) {");

                out.println("addMessage(message);");

                out.println("}");

                out.println("});");

                out.println("} catch (error) {");
                out.println("console.log('Could not check for new messages.');");
                out.println("} finally {");
                out.println("polling = false;");
                out.println("}");

                out.println("}");

                out.println("const chatTimer = setInterval(loadNewMessages, 3000);");

                out.println("chatBox.scrollTop = chatBox.scrollHeight;");

                out.println("})();");
                out.println("</script>");

                out.println("</body>");
                out.println("</html>");
            }

        } catch (SQLException e) {

            getServletContext().log("Could not load chat", e);

            if (!response.isCommitted()) {
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Could not load chat."
                );
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        // Check login.
        if (session == null || session.getAttribute("studentId") == null) {
            response.sendRedirect("index.html?message=Please%20login%20first");
            return;
        }

        int studentId = (Integer) session.getAttribute("studentId");

        int requestId;

        try {
            requestId = Integer.parseInt(request.getParameter("requestId"));
        } catch (NumberFormatException e) {
            response.sendRedirect("my-requests");
            return;
        }

        String message = request.getParameter("message");

        // Check CSRF token.
        String sessionToken = (String) session.getAttribute("chatCsrfToken");
        String submittedToken = request.getParameter("csrfToken");

        if (sessionToken == null || submittedToken == null
                || !sessionToken.equals(submittedToken)) {

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Invalid form submission. Refresh the chat and try again."
            );
            return;
        }

        // Validate message.
        if (message == null || message.trim().isEmpty()
                || message.length() > 2000) {

            response.sendRedirect("chat?requestId=" + requestId);
            return;
        }

        try (Connection con = DBconn.getConnection()) {

            // Verify the student is a participant and the request
            // is still accepted.
            String checkSql
                    = "SELECT request_id FROM exchange_requests "
                    + "WHERE request_id = ? "
                    + "AND status = 'ACCEPTED' "
                    + "AND (from_student = ? OR to_student = ?)";

            boolean allowed = false;

            try (PreparedStatement ps = con.prepareStatement(checkSql)) {

                ps.setInt(1, requestId);
                ps.setInt(2, studentId);
                ps.setInt(3, studentId);

                try (ResultSet rs = ps.executeQuery()) {
                    allowed = rs.next();
                }
            }

            if (!allowed) {
                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "You are not allowed to send messages in this chat."
                );
                return;
            }

            // Save message in database.
            String insertSql
                    = "INSERT INTO chat_messages "
                    + "(request_id, sender_id, message_text) "
                    + "VALUES (?, ?, ?)";

            try (PreparedStatement ps = con.prepareStatement(insertSql)) {

                ps.setInt(1, requestId);
                ps.setInt(2, studentId);
                ps.setString(3, message.trim());

                ps.executeUpdate();
            }

            response.sendRedirect("chat?requestId=" + requestId);

        } catch (SQLException e) {

            getServletContext().log("Could not send chat message", e);

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not send message."
            );
        }
    }

    private void sendNewMessages(Connection con,
            HttpServletRequest request,
            HttpServletResponse response,
            int requestId) throws SQLException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");

        long afterId = 0;

        try {
            String value = request.getParameter("afterId");

            if (value != null) {
                afterId = Long.parseLong(value);
            }

            if (afterId < 0) {
                afterId = 0;
            }

        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int studentId = (Integer) request.getSession(false)
                .getAttribute("studentId");

        // Verify that the student still belongs to this accepted chat.
        String checkSql
                = "SELECT request_id FROM exchange_requests "
                + "WHERE request_id = ? "
                + "AND status = 'ACCEPTED' "
                + "AND (from_student = ? OR to_student = ?)";

        boolean allowed = false;

        try (PreparedStatement ps = con.prepareStatement(checkSql)) {

            ps.setInt(1, requestId);
            ps.setInt(2, studentId);
            ps.setInt(3, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                allowed = rs.next();
            }
        }

        if (!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

// Mark newly received messages as delivered and seen.
// First, mark newly received messages as Delivered.
        String deliverySql
                = "UPDATE chat_messages "
                + "SET is_delivered = TRUE, "
                + "delivered_at = COALESCE(delivered_at, CURRENT_TIMESTAMP) "
                + "WHERE request_id = ? "
                + "AND sender_id != ? "
                + "AND is_delivered = FALSE";

        try (PreparedStatement ps = con.prepareStatement(deliverySql)) {

            ps.setInt(1, requestId);
            ps.setInt(2, studentId);

            ps.executeUpdate();
        }

// Mark previously delivered messages as Seen.
        String seenSql
                = "UPDATE chat_messages "
                + "SET is_read = TRUE, "
                + "read_at = COALESCE(read_at, CURRENT_TIMESTAMP) "
                + "WHERE request_id = ? "
                + "AND sender_id != ? "
                + "AND is_delivered = TRUE "
                + "AND is_read = FALSE "
                + "AND message_id <= ?";

        try (PreparedStatement ps = con.prepareStatement(seenSql)) {

            ps.setInt(1, requestId);
            ps.setInt(2, studentId);
            ps.setLong(3, afterId);

            ps.executeUpdate();
        }

        String sql
                = "SELECT cm.message_id, cm.sender_id, cm.message_text, "
                + "cm.sent_at, cm.is_delivered, cm.is_read, "
                + "st.full_name AS sender_name "
                + "FROM chat_messages cm "
                + "JOIN students st ON cm.sender_id = st.student_id "
                + "WHERE cm.request_id = ? AND cm.message_id > ? "
                + "ORDER BY cm.message_id ASC LIMIT 100";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, requestId);
            ps.setLong(2, afterId);

            try (ResultSet rs = ps.executeQuery()) {

                PrintWriter out = response.getWriter();

                out.print("[");

                boolean first = true;

                while (rs.next()) {

                    if (!first) {
                        out.print(",");
                    }

                    first = false;

                    out.print("{");

                    out.print("\"id\":" + rs.getLong("message_id") + ",");
                    out.print("\"senderId\":" + rs.getInt("sender_id") + ",");
                    out.print("\"senderName\":\""
                            + escapeJson(rs.getString("sender_name")) + "\",");
                    out.print("\"messageText\":\""
                            + escapeJson(rs.getString("message_text")) + "\",");

                    out.print("\"sentAt\":\""
                            + escapeJson(String.valueOf(rs.getTimestamp("sent_at"))) + "\",");

                    out.print("\"isDelivered\":" + rs.getBoolean("is_delivered") + ",");
                    out.print("\"isRead\":" + rs.getBoolean("is_read"));

                    out.print("}");

                }

// Get updated statuses of messages sent by the logged-in student.
                String statusSql
                        = "SELECT message_id, is_delivered, is_read "
                        + "FROM chat_messages "
                        + "WHERE request_id = ? AND sender_id = ? "
                        + "AND message_id <= ? "
                        + "ORDER BY message_id ASC";

                try (PreparedStatement statusPs = con.prepareStatement(statusSql)) {

                    statusPs.setInt(1, requestId);
                    statusPs.setInt(2, studentId);

                    long latestMessageId = afterId;

                    statusPs.setLong(3, latestMessageId);

                    try (ResultSet statusRs = statusPs.executeQuery()) {

                        while (statusRs.next()) {

                            if (!first) {
                                out.print(",");
                            }

                            first = false;

                            out.print("{");
                            out.print("\"type\":\"status\",");
                            out.print("\"id\":" + statusRs.getLong("message_id") + ",");
                            out.print("\"isDelivered\":" + statusRs.getBoolean("is_delivered") + ",");
                            out.print("\"isRead\":" + statusRs.getBoolean("is_read"));
                            out.print("}");
                        }
                    }
                }

                out.print("]");
            }
        }
    }

    // Escape characters before placing database values in JSON.
    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {

            char c = value.charAt(i);

            switch (c) {
                case '"':
                    result.append("\\\"");
                    break;

                case '\\':
                    result.append("\\\\");
                    break;

                case '\n':
                    result.append("\\n");
                    break;

                case '\r':
                    result.append("\\r");
                    break;

                case '\t':
                    result.append("\\t");
                    break;

                default:
                    if (c < 32) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                    break;
            }
        }

        return result.toString();
    }

    // Escape HTML characters to prevent HTML and JavaScript injection.
    private String escape(String value) {

        if (value == null) {
            return "";
        }

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
