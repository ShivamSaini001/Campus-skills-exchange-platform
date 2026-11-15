
package com.campus;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/received-requests")
public class ReceivedRequestsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        // Check whether the student is logged in.
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("studentId") == null) {
            response.sendRedirect("index.html?message=Please%20login%20first");
            return;
        }

        // Get the ID of the logged-in student.
        int teacherId = (Integer) session.getAttribute("studentId");

        response.setContentType("text/html;charset=UTF-8");

        try (Connection con = DBconn.getConnection();
                PrintWriter out = response.getWriter()) {

            out.println("<!doctype html><html lang='en'><head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<title>Received Requests</title>");
            out.println("<link rel='stylesheet' href='style.css'>");
            out.println("</head><body>");

            // Shared navbar
            Navbar.printNavbar(out, session);

            out.println("<main class='page'>");

            out.println("<section class='welcome'>");
            out.println("<p class='eyebrow'>TEACHER SPACE</p>");
            out.println("<h1>Received Requests</h1>");
            out.println("<p>Manage requests from students who want to learn your skills.</p>");
            out.println("</section>");

            // Get requests received by the logged-in student.
            String sql
                    = "SELECT er.request_id, er.request_message, er.status, "
                    + "er.created_at, s.skill_name, st.full_name AS learner_name, "
                    + "st.course, st.semester "
                    + "FROM exchange_requests er "
                    + "JOIN students st ON er.from_student = st.student_id "
                    + "JOIN skills s ON er.skill_id = s.skill_id "
                    + "WHERE er.to_student = ? "
                    + "ORDER BY er.created_at DESC";

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                // Only show requests addressed to this teacher.
                ps.setInt(1, teacherId);

                try (ResultSet rs = ps.executeQuery()) {

                    boolean found = false;

                    out.println("<div class='skill-grid'>");

                    while (rs.next()) {

                        found = true;

                        int requestId = rs.getInt("request_id");

                        String status = rs.getString("status");

                        String cardClass = "skill-card request-card";

                        if ("ACCEPTED".equals(status)) {
                            cardClass += " status-accepted";
                        } else if ("REJECTED".equals(status)) {
                            cardClass += " status-rejected";
                        } else {
                            cardClass += " status-pending";
                        }

                        String cardStyle = "";

                        if ("ACCEPTED".equals(status)) {
                            cardStyle = "background-color: rgba(34, 197, 94, 0.12);"
                                    + "border: 1px solid rgba(34, 197, 94, 0.5);";
                        } else if ("REJECTED".equals(status)) {
                            cardStyle = "background-color: rgba(239, 68, 68, 0.12);"
                                    + "border: 1px solid rgba(239, 68, 68, 0.5);";
                        } else {
                            cardStyle = "background-color: #f5f3ff;"
                                    + "border: 1px solid #e8e6f1;";
                        }

                        out.println("<article class='" + cardClass
                                + "' style='" + cardStyle + "'>");

                        out.println("<span class='tag'>"
                                + escape(status) + "</span>");

                        out.println("<h3>"
                                + escape(rs.getString("skill_name"))
                                + "</h3>");

                        out.println("<p>Learner: <strong>"
                                + escape(rs.getString("learner_name"))
                                + "</strong></p>");

                        out.println("<p>Course: "
                                + escape(rs.getString("course"))
                                + "</p>");

                        out.println("<p>Semester: "
                                + rs.getInt("semester")
                                + "</p>");

                        String message = rs.getString("request_message");

                        if (message != null && !message.isBlank()) {
                            out.println("<p>Message: "
                                    + escape(message)
                                    + "</p>");
                        }

                        out.println("<p><small>Requested: "
                                + escape(String.valueOf(
                                        rs.getTimestamp("created_at")))
                                + "</small></p>");

                        // Show buttons according to request status.
                        if ("PENDING".equals(status)) {

                            out.println("<form action='update-request' method='post'>");

                            out.println("<input type='hidden' name='requestId' value='"
                                    + requestId + "'>");

                            out.println("<button type='submit' name='action' "
                                    + "value='accept'>Accept</button>");

                            out.println("<button type='submit' name='action' "
                                    + "value='reject'>Reject</button>");

                            out.println("</form>");

                        } else if ("ACCEPTED".equals(status)) {

                            // Chat is available only for accepted requests.
                            out.println("<a href='chat?requestId=" + requestId
                                    + "' class='edit-profile-btn'>Open Chat</a>");
                        }

                        out.println("</article>");
                    }

                    out.println("</div>");

                    if (!found) {
                        out.println("<p>You have not received any skill requests yet.</p>");
                    }
                }
            }

            out.println("</main>");
            out.println("</body></html>");

        } catch (SQLException e) {

            getServletContext().log("Could not load received requests", e);

            if (!response.isCommitted()) {
                response.reset();

                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Could not load received requests. Check server logs."
                );
            }
        }
    }

    // Escape HTML characters before displaying database values.
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
