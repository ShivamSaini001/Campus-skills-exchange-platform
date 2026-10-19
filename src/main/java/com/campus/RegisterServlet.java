package com.campus;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String name = request.getParameter("full_name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String course = request.getParameter("course");
        String semesterText = request.getParameter("semester");

        if (name == null || email == null || password == null ||
                course == null || semesterText == null ||
                name.isBlank() || email.isBlank() || password.length() < 6 ||
                course.isBlank() || semesterText.isBlank()) {
            response.sendRedirect("index.html?message=Please%20fill%20all%20fields%20and%20use%206%2B%20password%20characters");
            return;
        }

        int semester;
        try {
            semester = Integer.parseInt(semesterText);
            if (semester < 1 || semester > 12) {
                response.sendRedirect("index.html?message=Enter%20a%20valid%20semester");
                return;
            }
        } catch (NumberFormatException e) {
            response.sendRedirect("index.html?message=Enter%20a%20valid%20semester");
            return;
        }

        String sql = "INSERT INTO students (full_name, email, password_hash, course, semester) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBconn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, email.trim().toLowerCase());
            ps.setString(3, BCrypt.hashpw(password, BCrypt.gensalt()));
            ps.setString(4, course.trim());
            ps.setInt(5, semester);
            ps.executeUpdate();
            response.sendRedirect("index.html?message=Account%20created%20successfully.%20You%20can%20login%20now");
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                response.sendRedirect("index.html?message=This%20email%20is%20already%20registered");
            } else {
                getServletContext().log("Registration failed", e);
                response.sendRedirect("index.html?message=Registration%20failed.%20Check%20database%20settings");
            }
        }
    }
}
