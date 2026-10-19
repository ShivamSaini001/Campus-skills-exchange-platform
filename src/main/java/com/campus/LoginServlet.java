package com.campus;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            response.sendRedirect("index.html?message=Enter%20your%20email%20and%20password");
            return;
        }

        String sql = "SELECT student_id, full_name, password_hash FROM students WHERE email = ?";

        try (Connection con = DBconn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && BCrypt.checkpw(password, rs.getString("password_hash"))) {
                    HttpSession session = request.getSession(true);
                    session.setAttribute("studentId", rs.getInt("student_id"));
                    session.setAttribute("studentName", rs.getString("full_name"));
                    response.sendRedirect("home");
                } else {
                    response.sendRedirect("index.html?message=Email%20or%20password%20is%20incorrect");
                }
            }
        } catch (SQLException e) {
            getServletContext().log("Login failed", e);
            response.sendRedirect("index.html?message=Login%20failed.%20Check%20database%20settings");
        }
    }
}
