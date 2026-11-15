
package com.campus;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/update-request")
public class UpdateRequestServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
                          throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // Check whether the student is logged in
        if (session == null || session.getAttribute("studentId") == null) {
            response.sendRedirect("index.html?message=Please%20login%20first");
            return;
        }

        int teacherId = (Integer) session.getAttribute("studentId");

        String requestIdText = request.getParameter("requestId");
        String action = request.getParameter("action");

        // Validate request ID
        int requestId;

        try {
            requestId = Integer.parseInt(requestIdText);
        } catch (NumberFormatException e) {
            response.sendRedirect("received-requests?message=invalid");
            return;
        }

        // Convert button action into request status
        String newStatus;

        if ("accept".equals(action)) {
            newStatus = "ACCEPTED";
        } else if ("reject".equals(action)) {
            newStatus = "REJECTED";
        } else {
            response.sendRedirect("received-requests?message=invalid");
            return;
        }

        // Update only requests received by the logged-in student
        String sql = "UPDATE exchange_requests "
                   + "SET status = ? "
                   + "WHERE request_id = ? "
                   + "AND to_student = ? "
                   + "AND status = 'PENDING'";

        try (Connection con = DBconn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, requestId);
            ps.setInt(3, teacherId);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated == 1) {
                response.sendRedirect("received-requests?message="
                        + newStatus.toLowerCase());
            } else {
                response.sendRedirect("received-requests?message=notupdated");
            }

        } catch (SQLException e) {
            getServletContext().log("Could not update skill request", e);
            response.sendRedirect("received-requests?message=error");
        }
    }
}
