
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

@WebServlet("/send-request")
public class SendRequestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("studentId") == null) {
            response.sendRedirect("index.html?message=Please%20login%20first");
            return;
        }

        String toText = request.getParameter("toStudent");
        String skillText = request.getParameter("skillId");

        if (toText == null || !toText.matches("\\d+")
                || skillText == null || !skillText.matches("\\d+")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid student or skill.");
            return;
        }

        int fromStudent = (Integer) session.getAttribute("studentId");
        int toStudent = Integer.parseInt(toText);
        int skillId = Integer.parseInt(skillText);

        if (fromStudent == toStudent) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "You cannot request your own skill.");
            return;
        }

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter();
             Connection con = DBconn.getConnection()) {

            String sql =
                "SELECT s.skill_name, st.full_name "
                + "FROM student_skills ss "
                + "JOIN skills s ON ss.skill_id = s.skill_id "
                + "JOIN students st ON st.student_id = ss.student_id "
                + "WHERE ss.student_id = ? AND ss.skill_id = ? "
                + "AND ss.skill_type = 'TEACH'";

            String skillName;
            String teacherName;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, toStudent);
                ps.setInt(2, skillId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                "This teaching skill is no longer available.");
                        return;
                    }

                    skillName = rs.getString("skill_name");
                    teacherName = rs.getString("full_name");
                }
            }

            out.println("<!doctype html><html lang='en'><head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<title>Request a Skill</title>");
            out.println("<link rel='stylesheet' href='style.css'></head><body>");
            out.println("<header class='topbar'>");
            out.println("<a class='brand' href='home'>Campus Skill Exchange</a>");
            out.println("<a class='nav-link' href='logout'>Log out</a>");
            out.println("</header><main class='page'>");
            out.println("<section class='welcome'>");
            out.println("<h1>Request a Skill</h1>");
            out.println("<p>Skill: <strong>" + escape(skillName) + "</strong></p>");
            out.println("<p>Teacher: <strong>" + escape(teacherName) + "</strong></p>");
            out.println("</section>");

            out.println("<section class='form-card'>");
            out.println("<form action='send-request' method='post'>");
            out.println("<input type='hidden' name='toStudent' value='" + toStudent + "'>");
            out.println("<input type='hidden' name='skillId' value='" + skillId + "'>");
            out.println("<label for='requestMessage'>Message (optional)</label>");
            out.println("<textarea id='requestMessage' name='requestMessage' maxlength='255' rows='4' placeholder='Hi, I would like to learn this skill.'></textarea>");
            out.println("<button type='submit'>Send Request</button>");
            out.println("</form></section>");
            out.println("<p><a href='browse-skills'>Back to Browse Skills</a></p>");
            out.println("</main></body></html>");

        } catch (SQLException e) {
            getServletContext().log("Could not open skill request form", e);

            if (!response.isCommitted()) {
                response.reset();
                response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not open the request form. Check server logs.");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("studentId") == null) {
            response.sendRedirect("index.html?message=Please%20login%20first");
            return;
        }

        String toText = request.getParameter("toStudent");
        String skillText = request.getParameter("skillId");
        String message = request.getParameter("requestMessage");

        if (toText == null || !toText.matches("\\d+")
                || skillText == null || !skillText.matches("\\d+")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid student or skill.");
            return;
        }

        int fromStudent = (Integer) session.getAttribute("studentId");
        int toStudent = Integer.parseInt(toText);
        int skillId = Integer.parseInt(skillText);

        if (fromStudent == toStudent) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "You cannot request your own skill.");
            return;
        }

        if (message != null && message.length() > 255) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Message must be 255 characters or fewer.");
            return;
        }

        String verifySql =
            "SELECT 1 FROM student_skills "
            + "WHERE student_id = ? AND skill_id = ? AND skill_type = 'TEACH'";

        String duplicateSql =
            "SELECT 1 FROM exchange_requests "
            + "WHERE from_student = ? AND to_student = ? "
            + "AND skill_id = ? AND status = 'PENDING' "
            + "LIMIT 1";

        String insertSql =
            "INSERT INTO exchange_requests "
            + "(from_student, to_student, skill_id, request_message, status) "
            + "VALUES (?, ?, ?, ?, 'PENDING')";

        try (Connection con = DBconn.getConnection()) {

            // Check whether the teaching skill is still available.
            try (PreparedStatement verify = con.prepareStatement(verifySql)) {
                verify.setInt(1, toStudent);
                verify.setInt(2, skillId);

                try (ResultSet rs = verify.executeQuery()) {
                    if (!rs.next()) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                "This teaching skill is no longer available.");
                        return;
                    }
                }
            }

            // Check whether a pending request already exists.
            try (PreparedStatement check = con.prepareStatement(duplicateSql)) {
                check.setInt(1, fromStudent);
                check.setInt(2, toStudent);
                check.setInt(3, skillId);

                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        response.sendRedirect("my-requests?message=duplicate");
                        return;
                    }
                }
            }

            // Insert the new request if no pending duplicate exists.
            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                ps.setInt(1, fromStudent);
                ps.setInt(2, toStudent);
                ps.setInt(3, skillId);
                ps.setString(4,
                        message == null || message.isBlank() ? null : message.trim());

                ps.executeUpdate();
            }

            response.sendRedirect("my-requests?message=sent");

        } catch (SQLException e) {

            // MySQL error 1062 means a duplicate entry was rejected.
            if (e.getErrorCode() == 1062) {
                response.sendRedirect("my-requests?message=duplicate");
                return;
            }

            getServletContext().log("Could not save skill request", e);

            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Could not send the request. Check the server logs.");
        }
    }

    private String escape(String value) {
        if (value == null) return "";

        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}
