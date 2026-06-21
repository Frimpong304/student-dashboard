package com.dashboard.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a student in the UCC portal.
 * Extends User with student-specific fields.
 */
public class Student extends User {

    private int    studentId;      // students table id
    private String studentNumber;  // e.g. PS/CSC/23/0210
    private String major;
    private int    year;
    private String profileImagePath;
    private List<Course> courses;
    private List<Grade>  grades;

    public Student() {
        this.courses = new ArrayList<>();
        this.grades  = new ArrayList<>();
    }

    public Student(int id, String name, String email,
                   String password, String major, int year) {
        super(id, name, email, "STUDENT");
        this.password = password;
        this.major    = major;
        this.year     = year;
        this.courses  = new ArrayList<>();
        this.grades   = new ArrayList<>();
    }

    // Getters & Setters
    public int    getStudentId()    { return studentId; }
    public void   setStudentId(int id) { this.studentId = id; }

    // keep getId() working for backward compat — returns studentId
    // if set, otherwise user id
    @Override
    public int getId() {
        return studentId > 0 ? studentId : id;
    }

    public String getStudentNumber()  { return studentNumber; }
    public void   setStudentNumber(String n) { this.studentNumber = n; }

    public String getMajor()  { return major; }
    public void   setMajor(String m) { this.major = m; }

    public int  getYear()     { return year; }
    public void setYear(int y){ this.year = y; }

    public String getProfileImagePath() { return profileImagePath; }
    public void   setProfileImagePath(String p) {
        this.profileImagePath = p;
    }

    public List<Course> getCourses() {
        return Collections.unmodifiableList(courses);
    }

    public List<Grade> getGrades() {
        return Collections.unmodifiableList(grades);
    }

    public void addCourse(Course course) {
        if (course != null && !courses.contains(course))
            courses.add(course);
    }

    public void removeCourse(Course course) {
        courses.remove(course);
    }

    public void addGrade(Grade grade) {
        if (grade != null) grades.add(grade);
    }

    // Business Logic
    public double getGPA() {
        if (grades.isEmpty()) return 0.0;
        double total = grades.stream()
                .mapToDouble(Grade::getGradePoints).sum();
        return Math.round((total / grades.size()) * 100.0) / 100.0;
    }

    public String getYearLabel() {
        return switch (year) {
            case 1 -> "Freshman";
            case 2 -> "Sophomore";
            case 3 -> "Junior";
            case 4 -> "Senior";
            default -> "Year " + year;
        };
    }

    public boolean authenticate(String email, String password) {
        return this.email.equalsIgnoreCase(email.trim())
                && this.password.equals(password);
    }

    @Override
    public String toString() {
        return String.format("Student{id=%d, name='%s', gpa=%.2f}",
                getId(), name, getGPA());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Student other)) return false;
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}