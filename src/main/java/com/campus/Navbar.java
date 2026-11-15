
package com.campus;

import java.io.PrintWriter;

import jakarta.servlet.http.HttpSession;

public class Navbar {

    public static void printNavbar(PrintWriter out, HttpSession session) {

        String name = (String) session.getAttribute("studentName");
        Object studentId = session.getAttribute("studentId");

        String initial = (name == null || name.trim().isEmpty())
                ? "S" : name.trim().substring(0, 1).toUpperCase();

        out.println("<header class='topbar'>");

        out.println("<a class='brand' href='home'>Campus Skill Exchange</a>");

        out.println("<div class='nav-actions'>");

        out.println("<nav class='nav-menu'>");
        out.println("<a class='nav-button' href='home'>Home</a>");
        out.println("<a class='nav-button' href='browse-skills'>Browse Skills</a>");
        out.println("<a class='nav-button' href='my-skills'>My Skills</a>");
        out.println("<a class='nav-button' href='my-requests'>My Requests</a>");
        out.println("<a class='nav-button' href='received-requests'>Received Requests</a>");
        out.println("<a class='nav-button' href='profile'>My Profile</a>");
        out.println("<a class='nav-button' href='logout'>Logout</a>");
        out.println("</nav>");

        // Profile dropdown
        out.println("<details class='profile-dropdown'>");
        out.println("<summary class='profile-trigger'>" + escape(initial) + "</summary>");

        out.println("<div class='profile-card'>");

        out.println("<div class='profile-info'>");
        out.println("<h3>" + escape(name) + "</h3>");
        out.println("<p>Student ID: " + escape(String.valueOf(studentId)) + "</p>");
        out.println("</div>");

        out.println("<div class='profile-links'>");
        out.println("<a href='profile'>My Profile</a>");
        out.println("<a href='logout' class='logout-button'>Log out</a>");
        out.println("</div>");

        out.println("</div>");
        out.println("</details>");

        out.println("</div>");
        out.println("</header>");

        // Automatically highlight the current page
        out.println("<script>");
        out.println("document.addEventListener('DOMContentLoaded', function() {");

        out.println("var currentPath = window.location.pathname;");
        out.println("var links = document.querySelectorAll('.nav-menu .nav-button');");

        out.println("links.forEach(function(link) {");

        out.println("var linkPath = new URL(link.href).pathname;");

        out.println("if (currentPath === linkPath || currentPath.endsWith(linkPath)) {");
        out.println("link.classList.add('active-nav');");
        out.println("link.setAttribute('aria-current', 'page');");
        out.println("}");

        // Edit Profile also highlights My Profile
        out.println("if (currentPath.endsWith('/edit-profile') && linkPath.endsWith('/profile')) {");
        out.println("link.classList.add('active-nav');");
        out.println("link.setAttribute('aria-current', 'page');");
        out.println("}");

        out.println("});");
        out.println("});");
        out.println("</script>");
    }

    private static String escape(String value) {

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
