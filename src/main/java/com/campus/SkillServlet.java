package com.campus;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/my-skills")

public class SkillServlet extends HttpServlet {

    @Override

    protected void doGet(HttpServletRequest request,

            HttpServletResponse response)

            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("studentId") == null) {

            response.sendRedirect("index.html?message=Please%20login%20first");

            return;

        }

        int studentId = (Integer) session.getAttribute("studentId");

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter();

             Connection con = DBconn.getConnection()) {

            out.println("<!doctype html><html lang='en'><head>");

            out.println("<meta charset='UTF-8'>");

            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");

            out.println("<title>My Skills</title>");

            out.println("<link rel='stylesheet' href='style.css'>");

            // Small page-specific styles for the unified skill form.

            out.println("<style>");

            out.println(".new-skill-fields { display: none; }");

            out.println(".skill-hint { color: #777b8b; font-size: 13px; margin-top: 6px; }");

            out.println("</style>");

            out.println("</head><body>");

            // Common Navbar used across all student pages.
            Navbar.printNavbar(out, session);

            out.println("<main class='page'>");

            out.println("<section class='welcome'>");

            out.println("<p class='eyebrow'>YOUR SKILL PROFILE</p>");

            out.println("<h1>My Skills</h1>");

            out.println("<p>Search for a skill, select an existing one, or add a new skill.</p>");

            out.println("</section>");

            String message = request.getParameter("message");

            if ("added".equals(message)) {

                out.println("<p class='message'>Existing skill added to your profile successfully!</p>");

            } else if ("created".equals(message)) {

                out.println("<p class='message'>New skill created and added to your profile. It is now available to everyone!</p>");

            } else if ("removed".equals(message)) {

                out.println("<p class='message'>Skill removed successfully!</p>");

            } else if ("exists".equals(message)) {

                out.println("<p class='message'>You have already selected this skill and type.</p>");

            } else if ("skillexists".equals(message)) {

                out.println("<p class='message'>This skill already exists. Search for it and select the existing skill.</p>");

            } else if ("invalid".equals(message)) {

                out.println("<p class='message'>Please enter a valid skill name and choose Teach or Learn.</p>");

            } else if ("error".equals(message)) {

                out.println("<p class='message'>Something went wrong. Please try again.</p>");

            }

            // One unified form for selecting an existing skill or creating a new one.

            out.println("<section class='form-card'>");

            out.println("<h2>Add a Skill</h2>");

            out.println("<p>Start typing. If the skill is already available, select it from the suggestions.</p>");

            out.println("<form action='my-skills' method='post' id='skillForm'>");

            out.println("<input type='hidden' name='action' value='save'>");

            out.println("<label for='skillName'>Skill Name *</label>");

            out.println("<input type='text' id='skillName' name='skillName' "

                    + "list='skillOptions' placeholder='Search or enter a skill name' "

                    + "required maxlength='100' autocomplete='off'>");

            out.println("<datalist id='skillOptions'>");

            String skillSql =

                    "SELECT skill_id, skill_name, category FROM skills ORDER BY skill_name";

            try (PreparedStatement ps = con.prepareStatement(skillSql);

                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    String skillName = rs.getString("skill_name");

                    String category = rs.getString("category");

                    out.println("<option value='" + escape(skillName) + "'>"

                            + escape(category == null ? "" : category) + "</option>");

                }

            }

            out.println("</datalist>");

            out.println("<p class='skill-hint' id='skillHint'>Choose a suggested skill or type a new one.</p>");

            // These fields appear only when the typed skill is not in the shared list.

            out.println("<div class='new-skill-fields' id='newSkillFields'>");

            out.println("<p class='skill-hint'>This skill is not in the list yet. You can add it for everyone.</p>");

            out.println("<label for='category'>Category (Optional)</label>");

            out.println("<input type='text' id='category' name='category' "

                    + "placeholder='e.g. Programming' maxlength='100'>");

            out.println("<label for='description'>Description (Optional)</label>");

            out.println("<textarea id='description' name='description' "

                    + "placeholder='Describe this skill'></textarea>");

            out.println("</div>");

            out.println("<label for='skillType'>I want to</label>");

            out.println("<select id='skillType' name='skillType' required>");

            out.println("<option value=''>Choose an option</option>");

            out.println("<option value='TEACH'>Teach this skill</option>");

            out.println("<option value='LEARN'>Learn this skill</option>");

            out.println("</select>");

            out.println("<button type='submit' id='skillSubmit'>Add to My Skills</button>");

            out.println("</form>");

            out.println("</section>");

            // Display the student's selected skills.

            out.println("<h2>My Selected Skills</h2>");

            out.println("<div class='skill-grid'>");

            String selectedSql =

                    "SELECT ss.id, ss.skill_type, s.skill_name, s.category, s.description "

                    + "FROM student_skills ss "

                    + "JOIN skills s ON ss.skill_id = s.skill_id "

                    + "WHERE ss.student_id = ? "

                    + "ORDER BY s.skill_name, ss.skill_type";

            try (PreparedStatement ps = con.prepareStatement(selectedSql)) {

                ps.setInt(1, studentId);

                try (ResultSet rs = ps.executeQuery()) {

                    boolean found = false;

                    while (rs.next()) {

                        found = true;

                        out.println("<article class='skill-card'>");

                        out.println("<span class='tag'>"

                                + escape(rs.getString("category")) + "</span>");

                        out.println("<h3>" + escape(rs.getString("skill_name")) + "</h3>");

                        out.println("<p>" + escape(rs.getString("description")) + "</p>");

                        out.println("<p><strong>"

                                + ("TEACH".equals(rs.getString("skill_type"))

                                ? "I Can Teach" : "I Want to Learn")

                                + "</strong></p>");

                        out.println("<form action='my-skills' method='post'>");

                        out.println("<input type='hidden' name='action' value='remove'>");

                        out.println("<input type='hidden' name='selectionId' value='"

                                + rs.getInt("id") + "'>");

                        out.println("<button type='submit'>Remove</button>");

                        out.println("</form></article>");

                    }

                    if (!found) {

                        out.println("<p>You haven't selected any skills yet.</p>");

                    }

                }

            }

            // Detect whether the typed skill is already in the shared list.

            out.println("<script>");

            out.println("const skillInput = document.getElementById('skillName');");

            out.println("const skillOptions = document.getElementById('skillOptions');");

            out.println("const newSkillFields = document.getElementById('newSkillFields');");

            out.println("const skillHint = document.getElementById('skillHint');");

            out.println("const skillSubmit = document.getElementById('skillSubmit');");

            out.println("function checkSkillName() {");

            out.println("  const typedName = skillInput.value.trim();");

            out.println("  let existingSkill = false;");

            out.println("  for (const option of skillOptions.options) {");

            out.println("    if (option.value.trim().toLowerCase() === typedName.toLowerCase() && typedName !== '') {");

            out.println("      existingSkill = true;");

            out.println("      break;");

            out.println("    }");

            out.println("  }");

            out.println("  if (existingSkill) {");

            out.println("    newSkillFields.style.display = 'none';");

            out.println("    skillHint.textContent = 'This skill already exists. It will be added to your profile.';");

            out.println("    skillSubmit.textContent = 'Add to My Skills';");

            out.println("  } else if (typedName !== '') {");

            out.println("    newSkillFields.style.display = 'block';");

            out.println("    skillHint.textContent = 'New skill: it will be added to the shared platform list.';");

            out.println("    skillSubmit.textContent = 'Create Skill & Add to My Skills';");

            out.println("  } else {");

            out.println("    newSkillFields.style.display = 'none';");

            out.println("    skillHint.textContent = 'Choose a suggested skill or type a new one.';");

            out.println("    skillSubmit.textContent = 'Add to My Skills';");

            out.println("  }");

            out.println("}");

            out.println("skillInput.addEventListener('input', checkSkillName);");

            out.println("skillInput.addEventListener('change', checkSkillName);");

            out.println("</script>");

            // Close the profile dropdown when clicking outside it.
            out.println("<script>");
            out.println("document.addEventListener('click', function(event) {");
            out.println("    document.querySelectorAll('.profile-dropdown').forEach(function(menu) {");
            out.println("        if (!menu.contains(event.target)) {");
            out.println("            menu.removeAttribute('open');");
            out.println("        }");
            out.println("    });");
            out.println("});");
            out.println("</script>");

            out.println("</main></body></html>");

        } catch (SQLException e) {

            getServletContext().log("Could not load student skills", e);

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,

                    "Could not load your skills. Check the server logs.");

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

        int studentId = (Integer) session.getAttribute("studentId");

        String action = request.getParameter("action");

        try (Connection con = DBconn.getConnection()) {

            if ("save".equals(action)) {

                String skillName = request.getParameter("skillName");

                String category = request.getParameter("category");

                String description = request.getParameter("description");

                String skillType = request.getParameter("skillType");

                if (skillName == null || skillName.trim().isEmpty()

                        || !("TEACH".equals(skillType) || "LEARN".equals(skillType))) {

                    response.sendRedirect("my-skills?message=invalid");

                    return;

                }

                skillName = skillName.trim();

                if (category == null || category.trim().isEmpty()) {

                    category = "General";

                } else {

                    category = category.trim();

                }

                if (description == null) {

                    description = "";

                } else {

                    description = description.trim();

                }

                con.setAutoCommit(false);

                try {

                    int skillId;

                    boolean newSkill = false;

                    // Check the shared list first. The database collation makes

                    // this comparison case-insensitive.

                    String findSql =

                            "SELECT skill_id FROM skills WHERE skill_name = ? LIMIT 1";

                    try (PreparedStatement ps = con.prepareStatement(findSql)) {

                        ps.setString(1, skillName);

                        try (ResultSet rs = ps.executeQuery()) {

                            if (rs.next()) {

                                skillId = rs.getInt("skill_id");

                            } else {

                                skillId = -1;

                            }

                        }

                    }

                    // Create the skill only if it does not already exist.

                    if (skillId == -1) {

                        String insertSkillSql =

                                "INSERT INTO skills (skill_name, category, description) "

                                + "VALUES (?, ?, ?)";

                        try (PreparedStatement ps = con.prepareStatement(

                                insertSkillSql, Statement.RETURN_GENERATED_KEYS)) {

                            ps.setString(1, skillName);

                            ps.setString(2, category);

                            ps.setString(3, description);

                            ps.executeUpdate();

                            try (ResultSet rs = ps.getGeneratedKeys()) {

                                if (!rs.next()) {

                                    throw new SQLException("Could not get the new skill ID.");

                                }

                                skillId = rs.getInt(1);

                            }

                        }

                        newSkill = true;

                    }

                    // Add the existing or newly created skill to this student's profile.

                    String studentSql =

                            "INSERT INTO student_skills (student_id, skill_id, skill_type) "

                            + "VALUES (?, ?, ?)";

                    try (PreparedStatement ps = con.prepareStatement(studentSql)) {

                        ps.setInt(1, studentId);

                        ps.setInt(2, skillId);

                        ps.setString(3, skillType);

                        ps.executeUpdate();

                    }

                    con.commit();

                    response.sendRedirect(newSkill

                            ? "my-skills?message=created"

                            : "my-skills?message=added");

                } catch (SQLException e) {

                    try {

                        con.rollback();

                    } catch (SQLException rollbackError) {

                        e.addSuppressed(rollbackError);

                    }

                    throw e;

                } finally {

                    try {

                        con.setAutoCommit(true);

                    } catch (SQLException resetError) {

                        getServletContext().log("Could not reset database transaction", resetError);

                    }

                }

            } else if ("remove".equals(action)) {

                String selectionText = request.getParameter("selectionId");

                if (selectionText == null || !selectionText.matches("\\d+")) {

                    response.sendRedirect("my-skills?message=invalid");

                    return;

                }

                int selectionId = Integer.parseInt(selectionText);

                String sql =

                        "DELETE FROM student_skills WHERE id = ? AND student_id = ?";

                try (PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setInt(1, selectionId);

                    ps.setInt(2, studentId);

                    int rows = ps.executeUpdate();

                    response.sendRedirect(rows > 0

                            ? "my-skills?message=removed"

                            : "my-skills?message=invalid");

                }

            } else {

                response.sendRedirect("my-skills?message=invalid");

            }

        } catch (SQLException e) {

            if (e.getErrorCode() == 1062) {

                response.sendRedirect("my-skills?message=exists");

            } else {

                getServletContext().log("Could not update student skills", e);

                response.sendRedirect("my-skills?message=error");

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
