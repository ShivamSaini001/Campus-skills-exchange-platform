CAMPUS SKILL EXCHANGE PLATFORM - START HERE

This is a starter version using:
- HTML, CSS and JavaScript
- Java Servlets and JDBC
- MySQL
- Maven
- Tomcat 10.1

FOLDER LOCATION
Put this whole folder somewhere simple, for example:
C:\Users\vidhi\Desktop\Campus Skill Exchange Platform

IMPORTANT SETTINGS
1. Open src/main/java/com/campus/DBconn.java
2. Check USER and PASSWORD. The starter currently uses:
   USER = root
   PASSWORD = 1234
   Change PASSWORD to your real MySQL root password if it is different.
3. Run the SQL file database/campus-skill.sql in MySQL Workbench or the MySQL command line.
4. Check Tomcat is installed at C:\Tomcat. If not, edit TOMCAT_HOME in RunProject.bat.
5. Make sure Java and Maven are installed and available in PATH.
6. Double-click RunProject.bat to build, deploy and open the site.

FIRST TEST
- Open the website.
- Create an account with an email you have not used before.
- Then log in with the same email and password.
- You should see the skills page.

IF THE SITE DOES NOT OPEN
- Check Tomcat is at C:\Tomcat.
- Make sure port 8080 is not being used by another program.
- Check the terminal window for Maven errors.
- Check MySQL is running and the password in DBconn.java is correct.
- Do not open index.html directly from the folder. Use the localhost URL.

This first version includes account registration, login, logout and sample skills.
It is intentionally kept small so it is easier to understand and explain.
