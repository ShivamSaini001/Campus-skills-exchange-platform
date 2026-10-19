package com.campus;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

        try (PrintWriter out = response.getWriter()) {
            out.println("<!doctype html><html><head><meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<title>Campus Skill Exchange</title><link rel='stylesheet' href='style.css'></head><body>");
            out.println("<header class='topbar'><a class='brand' href='home'>Campus Skill Exchange</a><a class='nav-link' href='logout'>Log out</a></header>");
            out.println("<main class='page'><section class='welcome'><p class='eyebrow'>STUDENT SPACE</p>");
            out.println("<h1>Hi, " + escape(name) + "!</h1>");
            out.println("<p>Learn something new. Share what you know. Meet people from your campus.</p></section>");
            out.println("<h2>Skills you can explore</h2><div class='skill-grid'>");

            String sql = "SELECT skill_name, category, description FROM skills ORDER BY skill_name";
            try (Connection con = DBconn.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.println("<article class='skill-card'><span class='tag'>" + escape(rs.getString("category")) + "</span>");
                    out.println("<h3>" + escape(rs.getString("skill_name")) + "</h3>");
                    out.println("<p>" + escape(rs.getString("description")) + "</p></article>");
                }
            } catch (SQLException e) {
                getServletContext().log("Could not load skills", e);
                out.println("<p>Could not load skills. Check your database setup.</p>");
            }
            out.println("</div><p class='small-note'>This is the first version. We can add skill requests and profiles next.</p></main></body></html>");
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
