
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

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("studentId") == null) {
            response.sendRedirect("index.html?message=Please%20login%20first");
            return;
        }

        int studentId = Integer.parseInt(
                session.getAttribute("studentId").toString());

        response.setContentType("text/html;charset=UTF-8");

        String studentName = "";
        String email = "";
        String course = "";
        int semester = 0;
        String createdAt = "";

        try (PrintWriter out = response.getWriter()) {

            try (Connection con = DBconn.getConnection()) {

                // Get student details
                String studentSql
                        = "SELECT student_id, full_name, email, course, semester, created_at "
                        + "FROM students WHERE student_id = ?";

                try (PreparedStatement ps = con.prepareStatement(studentSql)) {

                    ps.setInt(1, studentId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {
                            studentName = rs.getString("full_name");
                            email = rs.getString("email");
                            course = rs.getString("course");
                            semester = rs.getInt("semester");
                            createdAt = rs.getString("created_at");
                        } else {
                            response.sendRedirect("home");
                            return;
                        }
                    }
                }

                out.println("<!DOCTYPE html><html><head>");
                out.println("<meta charset='UTF-8'>");
                out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
                out.println("<title>My Profile</title>");
                out.println("<link rel='stylesheet' href='style.css'>");
                out.println("</head><body>");

                // Shared navbar
                Navbar.printNavbar(out, session);

                out.println("<main class='page profile-page'>");

                // Profile action buttons below navbar
                out.println("<div class='profile-actions'>");
                out.println("<a href='edit-profile' class='edit-profile-btn'>✎ Edit My Profile</a>");
                out.println("<a href='my-skills' class='edit-profile-btn secondary-btn'>Manage My Skills</a>");
                out.println("</div>");

                out.println("<section class='welcome'>");
                out.println("<p class='eyebrow'>STUDENT PROFILE</p>");
                out.println("<h1>" + escape(studentName) + "</h1>");
                out.println("<p>Welcome to your student profile.</p>");
                out.println("</section>");

                // Personal Information
                out.println("<h2>Personal Information</h2>");
                out.println("<div class='skill-grid'>");

                out.println("<article class='skill-card'>");
                out.println("<h3>Student ID</h3>");
                out.println("<p>" + studentId + "</p>");
                out.println("</article>");

                out.println("<article class='skill-card'>");
                out.println("<h3>Email Address</h3>");
                out.println("<p>" + escape(email) + "</p>");
                out.println("</article>");

                out.println("<article class='skill-card'>");
                out.println("<h3>Course</h3>");
                out.println("<p>" + escape(course) + "</p>");
                out.println("</article>");

                out.println("<article class='skill-card'>");
                out.println("<h3>Semester</h3>");
                out.println("<p>" + semester + "</p>");
                out.println("</article>");

                out.println("<article class='skill-card'>");
                out.println("<h3>Joined On</h3>");
                out.println("<p>" + escape(createdAt) + "</p>");
                out.println("</article>");

                out.println("</div>");

                // Display skills offered and skills wanted
                String skillSql
                        = "SELECT s.skill_name, ss.skill_type "
                        + "FROM student_skills ss "
                        + "JOIN skills s ON ss.skill_id = s.skill_id "
                        + "WHERE ss.student_id = ? "
                        + "ORDER BY ss.skill_type, s.skill_name";

                try (PreparedStatement ps = con.prepareStatement(skillSql)) {

                    ps.setInt(1, studentId);

                    try (ResultSet rs = ps.executeQuery()) {

                        out.println("<h2>My Skills</h2>");
                        out.println("<div class='skill-grid'>");

                        out.println("<article class='skill-card'>");
                        out.println("<h3>Skills I Can Teach</h3>");

                        boolean hasTeachSkills = false;

                        while (rs.next()) {

                            if ("TEACH".equalsIgnoreCase(rs.getString("skill_type"))) {
                                hasTeachSkills = true;

                                out.println("<p>• "
                                        + escape(rs.getString("skill_name"))
                                        + "</p>");
                            }
                        }

                        if (!hasTeachSkills) {
                            out.println("<p>You haven't added any teaching skills yet.</p>");
                        }

                        out.println("</article>");

                        // Separate query for learning skills
                        String learnSql
                                = "SELECT s.skill_name "
                                + "FROM student_skills ss "
                                + "JOIN skills s ON ss.skill_id = s.skill_id "
                                + "WHERE ss.student_id = ? "
                                + "AND ss.skill_type = 'LEARN' "
                                + "ORDER BY s.skill_name";

                        out.println("<article class='skill-card'>");
                        out.println("<h3>Skills I Want to Learn</h3>");

                        boolean hasLearnSkills = false;

                        try (PreparedStatement learnPs = con.prepareStatement(learnSql)) {

                            learnPs.setInt(1, studentId);

                            try (ResultSet learnRs = learnPs.executeQuery()) {

                                while (learnRs.next()) {
                                    hasLearnSkills = true;

                                    out.println("<p>• "
                                            + escape(learnRs.getString("skill_name"))
                                            + "</p>");
                                }
                            }
                        }

                        if (!hasLearnSkills) {
                            out.println("<p>You haven't added any learning skills yet.</p>");
                        }

                        out.println("</article>");
                        out.println("</div>");
                    }
                }

                out.println("</main>");
                out.println("</body></html>");

            } catch (SQLException e) {

                getServletContext().log("Could not load student profile", e);

                out.println("<h2>Unable to load your profile.</h2>");
                out.println("<p>Please try again later.</p>");
            }
        }
    }

    // Escape HTML characters
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
