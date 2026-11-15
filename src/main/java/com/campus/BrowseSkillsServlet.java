
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

@WebServlet("/browse-skills")
public class BrowseSkillsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("studentId") == null) {
            response.sendRedirect("index.html?message=Please%20login%20first");
            return;
        }

        int currentStudentId = (Integer) session.getAttribute("studentId");

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter();
                Connection con = DBconn.getConnection()) {

            out.println("<!doctype html><html lang='en'><head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<title>Browse Skills</title>");
            out.println("<link rel='stylesheet' href='style.css'></head><body>");

            // Common Navbar
            Navbar.printNavbar(out, session);

            // Page Heading
            out.println("<main class='page'>");
            out.println("<section class='welcome'>");
            out.println("<p class='eyebrow'>CAMPUS COMMUNITY</p>");
            out.println("<h1>Browse Skills</h1>");
            out.println("<p>Discover skills offered by other students.</p>");
            out.println("</section>");

            // Fetch skills offered by other students
            String sql
                    = "SELECT DISTINCT s.skill_id, s.skill_name, s.category, s.description, "
                    + "st.student_id, st.full_name "
                    + "FROM student_skills ss "
                    + "JOIN skills s ON ss.skill_id = s.skill_id "
                    + "JOIN students st ON ss.student_id = st.student_id "
                    + "WHERE ss.skill_type = 'TEACH' "
                    + "AND ss.student_id <> ? "
                    + "ORDER BY s.skill_name, st.full_name";

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, currentStudentId);

                try (ResultSet rs = ps.executeQuery()) {

                    boolean found = false;

                    out.println("<div class='skill-grid'>");

                    while (rs.next()) {

                        found = true;

                        out.println("<article class='skill-card'>");

                        out.println("<h3>"
                                + escape(rs.getString("skill_name")) + "</h3>");

                        out.println("<p>"
                                + escape(rs.getString("description")) + "</p>");

                        out.println("<p>Offered by: <strong>"
                                + escape(rs.getString("full_name")) + "</strong></p>");

                        out.println("<form action='send-request' method='get'>");

                        out.println("<input type='hidden' name='toStudent' value='"
                                + rs.getInt("student_id") + "'>");

                        out.println("<input type='hidden' name='skillId' value='"
                                + rs.getInt("skill_id") + "'>");

                        out.println("<button type='submit'>Request This Skill</button>");

                        out.println("</form>");

                        out.println("</article>");
                    }

                    out.println("</div>");

                    if (!found) {
                        out.println("<p>No skills offered by other students "
                                + "are available yet.</p>");
                    }
                }
            }

            out.println("</main>");

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

        } catch (SQLException e) {

            getServletContext().log("Could not browse skills", e);

            if (!response.isCommitted()) {
                response.reset();
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Could not load skills. Check the server logs."
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
