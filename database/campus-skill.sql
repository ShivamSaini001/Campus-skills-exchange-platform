CREATE DATABASE IF NOT EXISTS campus_skill_exchange;
USE campus_skill_exchange;

CREATE TABLE IF NOT EXISTS students (
    student_id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    course VARCHAR(80) NOT NULL,
    semester INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS skills (
    skill_id INT PRIMARY KEY AUTO_INCREMENT,
    skill_name VARCHAR(100) NOT NULL,
    category VARCHAR(60) NOT NULL,
    description VARCHAR(255) NOT NULL
);

INSERT INTO skills (skill_name, category, description)
SELECT 'Java Basics', 'Programming', 'Learn classes, objects and simple Java programs.'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE skill_name = 'Java Basics');

INSERT INTO skills (skill_name, category, description)
SELECT 'Web Design', 'Design', 'Practice HTML, CSS and making pages look good.'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE skill_name = 'Web Design');

INSERT INTO skills (skill_name, category, description)
SELECT 'SQL', 'Database', 'Understand tables, queries and joins.'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE skill_name = 'SQL');

INSERT INTO skills (skill_name, category, description)
SELECT 'Public Speaking', 'Communication', 'Get more comfortable speaking in front of people.'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE skill_name = 'Public Speaking');

INSERT INTO skills (skill_name, category, description)
SELECT 'Graphic Design', 'Design', 'Learn the basics of layout, colour and visual design.'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE skill_name = 'Graphic Design');

INSERT INTO skills (skill_name, category, description)
SELECT 'Excel Basics', 'Productivity', 'Learn useful spreadsheet features and formulas.'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE skill_name = 'Excel Basics');

CREATE TABLE IF NOT EXISTS student_skills (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    skill_id INT NOT NULL,
    skill_type ENUM('TEACH', 'LEARN') NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE CASCADE,
    UNIQUE KEY unique_student_skill (student_id, skill_id, skill_type)
);

CREATE TABLE IF NOT EXISTS exchange_requests (
    request_id INT PRIMARY KEY AUTO_INCREMENT,
    from_student INT NOT NULL,
    to_student INT NOT NULL,
    skill_id INT NOT NULL,
    request_message VARCHAR(255),
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (from_student) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (to_student) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE CASCADE
);
