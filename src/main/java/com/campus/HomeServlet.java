
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

@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("studentId") == null) {
            response.sendRedirect("index.html?message=Please%20login%20first");
            return;
        }

        response.setContentType("text/html;charset=UTF-8");

        String name = (String) session.getAttribute("studentName");
        int studentId = Integer.parseInt(session.getAttribute("studentId").toString());

        try (PrintWriter out = response.getWriter()) {

            out.println("<!doctype html><html><head><meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<title>Campus Skill Exchange</title>");
            out.println("<link rel='stylesheet' href='style.css'></head><body>");

            // Common Navbar
            Navbar.printNavbar(out, session);

            // Welcome Section
            out.println("<main class='page'><section class='welcome'><p class='eyebrow'>STUDENT SPACE</p>");
            out.println("<h1>Hi, " + escape(name) + "!</h1>");
            out.println("<p>Learn something new. Share what you know. Meet people from your campus.</p></section>");

            // Dashboard Statistics
            int totalSkills = 0;
            int teachingSkills = 0;
            int learningSkills = 0;
            int pendingRequests = 0;

            String statsSql
                    = "SELECT "
                    + "(SELECT COUNT(*) FROM skills) AS total_skills, "
                    + "(SELECT COUNT(*) FROM student_skills WHERE student_id = ? AND skill_type = 'TEACH') AS teaching_skills, "
                    + "(SELECT COUNT(*) FROM student_skills WHERE student_id = ? AND skill_type = 'LEARN') AS learning_skills, "
                    + "(SELECT COUNT(*) FROM exchange_requests WHERE status = 'PENDING' AND (from_student = ? OR to_student = ?)) AS pending_requests";

            try (Connection con = DBconn.getConnection();
                    PreparedStatement ps = con.prepareStatement(statsSql)) {

                ps.setInt(1, studentId);
                ps.setInt(2, studentId);
                ps.setInt(3, studentId);
                ps.setInt(4, studentId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalSkills = rs.getInt("total_skills");
                        teachingSkills = rs.getInt("teaching_skills");
                        learningSkills = rs.getInt("learning_skills");
                        pendingRequests = rs.getInt("pending_requests");
                    }
                }

            } catch (SQLException e) {
                getServletContext().log("Could not load dashboard statistics", e);
            }

            out.println("<section class='stats-section'>");
            out.println("<div class='stats-heading'>");
            out.println("<p class='eyebrow'>YOUR ACTIVITY</p>");
            out.println("<h2>Your Dashboard Overview</h2>");
            out.println("</div>");

            out.println("<div class='stats-grid'>");

            out.println("<article class='stat-card stat-purple'>");
            out.println("<span class='stat-icon'>✦</span>");
            out.println("<p>Total Platform Skills</p>");
            out.println("<h3>" + totalSkills + "</h3>");
            out.println("</article>");

            out.println("<article class='stat-card stat-green'>");
            out.println("<span class='stat-icon'>↗</span>");
            out.println("<p>Skills I Can Teach</p>");
            out.println("<h3>" + teachingSkills + "</h3>");
            out.println("</article>");

            out.println("<article class='stat-card stat-blue'>");
            out.println("<span class='stat-icon'>◎</span>");
            out.println("<p>Skills I Want to Learn</p>");
            out.println("<h3>" + learningSkills + "</h3>");
            out.println("</article>");

            out.println("<article class='stat-card stat-orange'>");
            out.println("<span class='stat-icon'>◷</span>");
            out.println("<p>Pending Requests</p>");
            out.println("<h3>" + pendingRequests + "</h3>");
            out.println("</article>");

            out.println("</div>");
            out.println("</section>");

            // Explore Skills Section
            out.println("<h2>Skills you can explore</h2>");
            out.println("<div class='skill-grid'>");

            String sql = "SELECT skill_name, category, description FROM skills ORDER BY skill_name";

            try (Connection con = DBconn.getConnection();
                    PreparedStatement ps = con.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    out.println("<article class='skill-card'><span class='tag'>"
                            + escape(rs.getString("category")) + "</span>");

                    out.println("<h3>" + escape(rs.getString("skill_name")) + "</h3>");
                    out.println("<p>" + escape(rs.getString("description")) + "</p></article>");
                }

            } catch (SQLException e) {
                getServletContext().log("Could not load skills", e);
                out.println("<p>Could not load skills. Check your database setup.</p>");
            }

            out.println("</div></main>");

            // Close profile dropdown when clicking outside
            out.println("<script>");
            out.println("document.addEventListener('click', function(event) {");
            out.println("    document.querySelectorAll('.profile-dropdown').forEach(function(menu) {");
            out.println("        if (!menu.contains(event.target)) {");
            out.println("            menu.removeAttribute('open');");
            out.println("        }");
            out.println("    });");
            out.println("});");
            out.println("</script>");

            out.println("</body></html>");
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
