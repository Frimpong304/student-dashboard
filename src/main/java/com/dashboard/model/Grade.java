package com.dashboard.model;

public class Grade {

    private String courseCode;
    private String courseName;
    private double score;
    private String letterGrade;
    private String semester;

    public Grade() {}

    public Grade(Course course, double score, String semester) {
        this.courseCode  = course.getCourseCode();
        this.courseName  = course.getCourseName();
        this.score       = score;
        this.semester    = semester;
        this.letterGrade = deriveLetterGrade(score);
    }

    public String getCourseCode()  { return courseCode; }
    public String getCourseName()  { return courseName; }
    public String getSemester()    { return semester; }
    public void   setSemester(String s) { this.semester = s; }

    public double getScore() { return score; }
    public void   setScore(double score) {
        this.score       = score;
        this.letterGrade = deriveLetterGrade(score);
    }

    public String getLetterGrade() { return letterGrade; }

    public double getGradePoints() {
        if (score >= 90) return 4.0;
        if (score >= 80) return 3.0;
        if (score >= 70) return 2.0;
        if (score >= 60) return 1.0;
        return 0.0;
    }

    public Course getCourse() {
        Course stub = new Course();
        stub.setCourseCode(courseCode);
        stub.setCourseName(courseName);
        return stub;
    }

    private String deriveLetterGrade(double score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    @Override
    public String toString() {
        return String.format("%s — %.1f (%s)", courseCode, score, letterGrade);
    }
}