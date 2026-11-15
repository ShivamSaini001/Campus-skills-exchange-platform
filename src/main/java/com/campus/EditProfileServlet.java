
package com.campus;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/edit-profile")
public class EditProfileServlet extends HttpServlet {

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

        String name = "";
        String course = "";
        int semester = 1;

        try (Connection con = DBconn.getConnection()) {

            String sql = "SELECT full_name, course, semester "
                       + "FROM students WHERE student_id = ?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, studentId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        name = rs.getString("full_name");
                        course = rs.getString("course");
                        semester = rs.getInt("semester");
                    } else {
                        response.sendRedirect("home");
                        return;
                    }
                }
            }

        } catch (SQLException e) {
            getServletContext().log("Could not load edit profile page", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to load profile.");
            return;
        }

        try (PrintWriter out = response.getWriter()) {

            out.println("<!DOCTYPE html>");
            out.println("<html><head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<title>Edit Profile</title>");
            out.println("<link rel='stylesheet' href='style.css'>");
            out.println("</head><body>");

            out.println("<header class='topbar'>");
            out.println("<a class='brand' href='home'>Campus Skill Exchange</a>");
            out.println("<a class='nav-link' href='profile'>My Profile</a>");
            out.println("</header>");

            out.println("<main class='page'>");

            out.println("<section class='welcome'>");
            out.println("<p class='eyebrow'>ACCOUNT SETTINGS</p>");
            out.println("<h1>Edit Your Profile</h1>");
            out.println("<p>Update your personal and academic information.</p>");
            out.println("</section>");

            String error = request.getParameter("error");

            if ("incorrect".equals(error)) {
                out.println("<p class='message'>Current password is incorrect.</p>");
            } else if ("invalid".equals(error)) {
                out.println("<p class='message'>Please check your details and try again.</p>");
            } else if ("password".equals(error)) {
                out.println("<p class='message'>New password and confirmation do not match.</p>");
            } else if ("server".equals(error)) {
                out.println("<p class='message'>Something went wrong. Please try again.</p>");
            }

            out.println("<section class='skill-card edit-profile-card'>");

            out.println("<form action='edit-profile' method='post' class='edit-profile-form'>");

            out.println("<h2>Personal Information</h2>");

            out.println("<label for='fullName'>Full Name</label>");
            out.println("<input type='text' id='fullName' name='fullName' "
                    + "value='" + escape(name) + "' maxlength='100' required>");

            out.println("<label for='course'>Course</label>");
            out.println("<input type='text' id='course' name='course' "
                    + "value='" + escape(course) + "' maxlength='80' required>");

            out.println("<label for='semester'>Semester</label>");
            out.println("<select id='semester' name='semester' required>");

            for (int i = 1; i <= 8; i++) {
                out.println("<option value='" + i + "' "
                        + (semester == i ? "selected" : "") + ">"
                        + i + "</option>");
            }

            out.println("</select>");

            out.println("<h2>Security Verification</h2>");
            out.println("<p class='muted'>Enter your current password to save changes.</p>");

            out.println("<label for='currentPassword'>Current Password</label>");
            out.println("<input type='password' id='currentPassword' "
                    + "name='currentPassword' required>");

            out.println("<h2>Change Password (Optional)</h2>");

            out.println("<label for='newPassword'>New Password</label>");
            out.println("<input type='password' id='newPassword' "
                    + "name='newPassword' minlength='6'>");

            out.println("<label for='confirmPassword'>Confirm New Password</label>");
            out.println("<input type='password' id='confirmPassword' "
                    + "name='confirmPassword' minlength='6'>");

            out.println("<button type='submit'>Save Changes</button>");

            out.println("<p><a href='profile'>Cancel and Go Back</a></p>");

            out.println("</form>");
            out.println("</section>");

            out.println("</main>");
            out.println("</body></html>");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("studentId") == null) {
            response.sendRedirect("index.html?message=Please%20login%20first");
            return;
        }

        int studentId = Integer.parseInt(
                session.getAttribute("studentId").toString());

        String name = request.getParameter("fullName");
        String course = request.getParameter("course");
        String semesterValue = request.getParameter("semester");
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        if (name == null || course == null || semesterValue == null
                || currentPassword == null
                || name.isBlank() || course.isBlank()
                || currentPassword.isBlank()
                || name.trim().length() > 100
                || course.trim().length() > 80) {

            response.sendRedirect("edit-profile?error=invalid");
            return;
        }

        int semester;

        try {
            semester = Integer.parseInt(semesterValue);

            if (semester < 1 || semester > 8) {
                response.sendRedirect("edit-profile?error=invalid");
                return;
            }

        } catch (NumberFormatException e) {
            response.sendRedirect("edit-profile?error=invalid");
            return;
        }

        if (newPassword == null) {
            newPassword = "";
        }

        if (confirmPassword == null) {
            confirmPassword = "";
        }

        if (!newPassword.isEmpty() || !confirmPassword.isEmpty()) {

            if (newPassword.length() < 6
                    || !newPassword.equals(confirmPassword)) {

                response.sendRedirect("edit-profile?error=password");
                return;
            }
        }

        try (Connection con = DBconn.getConnection()) {

            // Get current password hash
            String checkSql =
                    "SELECT password_hash FROM students WHERE student_id = ?";

            String storedHash;

            try (PreparedStatement ps = con.prepareStatement(checkSql)) {

                ps.setInt(1, studentId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        response.sendRedirect("home");
                        return;
                    }

                    storedHash = rs.getString("password_hash");
                }
            }

            // Verify current password
            if (!BCrypt.checkpw(currentPassword, storedHash)) {
                response.sendRedirect("edit-profile?error=incorrect");
                return;
            }

            int rowsUpdated;

            if (!newPassword.isEmpty()) {

                // Hash the new password
                String newHash = BCrypt.hashpw(
                        newPassword, BCrypt.gensalt());

                String updateSql =
                        "UPDATE students SET full_name = ?, course = ?, "
                        + "semester = ?, password_hash = ? "
                        + "WHERE student_id = ?";

                try (PreparedStatement ps = con.prepareStatement(updateSql)) {

                    ps.setString(1, name.trim());
                    ps.setString(2, course.trim());
                    ps.setInt(3, semester);
                    ps.setString(4, newHash);
                    ps.setInt(5, studentId);

                    rowsUpdated = ps.executeUpdate();
                }

            } else {

                String updateSql =
                        "UPDATE students SET full_name = ?, course = ?, "
                        + "semester = ? WHERE student_id = ?";

                try (PreparedStatement ps = con.prepareStatement(updateSql)) {

                    ps.setString(1, name.trim());
                    ps.setString(2, course.trim());
                    ps.setInt(3, semester);
                    ps.setInt(4, studentId);

                    rowsUpdated = ps.executeUpdate();
                }
            }

            if (rowsUpdated == 1) {

                // Update the name stored in the login session
                session.setAttribute("studentName", name.trim());

                response.sendRedirect("profile");
            } else {
                response.sendRedirect("edit-profile?error=server");
            }

        } catch (SQLException e) {
            getServletContext().log("Profile update failed", e);
            response.sendRedirect("edit-profile?error=server");
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
