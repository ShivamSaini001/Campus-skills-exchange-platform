
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

@WebServlet("/my-requests")
public class MyRequestsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("studentId") == null) {
            response.sendRedirect("index.html?message=Please%20login%20first");
            return;
        }

        int studentId = (Integer) session.getAttribute("studentId");

        response.setContentType("text/html;charset=UTF-8");

        try (Connection con = DBconn.getConnection();
                PrintWriter out = response.getWriter()) {

            out.println("<!doctype html><html lang='en'><head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<title>My Requests</title>");
            out.println("<link rel='stylesheet' href='style.css'>");
            out.println("</head><body>");

            // Shared navbar
            Navbar.printNavbar(out, session);

            out.println("<main class='page'>");
            out.println("<section class='welcome'>");
            out.println("<p class='eyebrow'>YOUR ACTIVITY</p>");
            out.println("<h1>My Requests</h1>");
            out.println("<p>Track the skill lessons you have requested.</p>");
            out.println("</section>");

            // Display messages according to the URL parameter.
            String message = request.getParameter("message");

            if ("sent".equals(message)) {
                out.println("<p class='message'>Skill request sent successfully!</p>");
            } else if ("duplicate".equals(message)) {
                out.println("<p class='message'>You already have a pending request for this skill.</p>");
            }

            String sql
                    = "SELECT er.request_id, er.request_message, er.status, "
                    + "er.created_at, s.skill_name, st.full_name AS teacher_name "
                    + "FROM exchange_requests er "
                    + "JOIN skills s ON er.skill_id = s.skill_id "
                    + "JOIN students st ON er.to_student = st.student_id "
                    + "WHERE er.from_student = ? "
                    + "ORDER BY er.created_at DESC";

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, studentId);

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

                        out.println("<h3>"
                                + escape(rs.getString("skill_name"))
                                + "</h3>");

                        out.println("<p>Teacher: <strong>"
                                + escape(rs.getString("teacher_name"))
                                + "</strong></p>");

                        String requestMessage = rs.getString("request_message");

                        if (requestMessage != null && !requestMessage.isBlank()) {
                            out.println("<p>Message: "
                                    + escape(requestMessage) + "</p>");
                        }

                        out.println("<p>Status: <strong>"
                                + escape(status) + "</strong></p>");

                        out.println("<p><small>Requested: "
                                + escape(String.valueOf(rs.getTimestamp("created_at")))
                                + "</small></p>");

                        // Show Chat button only for accepted requests.
                        if ("ACCEPTED".equals(status)) {

                            out.println("<a href='chat?requestId=" + requestId
                                    + "' class='edit-profile-btn'>Open Chat</a>");
                        }

                        out.println("</article>");
                    }

                    out.println("</div>");

                    if (!found) {
                        out.println("<p>You have not sent any skill requests yet.</p>");
                    }
                }
            }

            out.println("</main>");
            out.println("</body></html>");

        } catch (SQLException e) {

            getServletContext().log("Could not load skill requests", e);

            if (!response.isCommitted()) {
                response.reset();
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Could not load requests. Check the server logs."
                );
            }
        }
    }

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
