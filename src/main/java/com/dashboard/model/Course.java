package com.dashboard.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Course {

    private String courseCode;
    private String courseName;
    private int    credits;
    private String instructor;
    private String schedule;
    private String description;
    private double progressPercent;
    private String semester;
    private List<Assignment> assignments;
    private List<String>     materials;

    public Course() {
        this.assignments = new ArrayList<>();
        this.materials   = new ArrayList<>();
    }

    public Course(String courseCode, String courseName, int credits,
                  String instructor, String schedule, String semester) {
        this.courseCode      = courseCode.trim().toUpperCase();
        this.courseName      = courseName.trim();
        this.credits         = credits;
        this.instructor      = instructor;
        this.schedule        = schedule;
        this.semester        = semester;
        this.progressPercent = 0.0;
        this.assignments     = new ArrayList<>();
        this.materials       = new ArrayList<>();
    }

    // Getters & Setters
    public String getCourseCode()  { return courseCode; }
    public void   setCourseCode(String c) { this.courseCode = c; }

    public String getCourseName()  { return courseName; }
    public void   setCourseName(String n) { this.courseName = n; }

    public int  getCredits()       { return credits; }
    public void setCredits(int c)  { this.credits = c; }

    public String getInstructor()  { return instructor; }
    public void   setInstructor(String i) { this.instructor = i; }

    public String getSchedule()    { return schedule; }
    public void   setSchedule(String s)   { this.schedule = s; }

    public String getDescription() { return description; }
    public void   setDescription(String d){ this.description = d; }

    public double getProgressPercent() { return progressPercent; }
    public void   setProgressPercent(double p) { this.progressPercent = p; }

    public String getSemester()    { return semester; }
    public void   setSemester(String s)   { this.semester = s; }

    public List<Assignment> getAssignments() {
        return Collections.unmodifiableList(assignments);
    }

    public List<String> getMaterials() {
        return Collections.unmodifiableList(materials);
    }

    public void addAssignment(Assignment a) {
        if (a != null && !assignments.contains(a)) assignments.add(a);
    }

    public void addMaterial(String m) {
        if (m != null && !m.isBlank()) materials.add(m.trim());
    }

    // Business Logic
    public long getPendingAssignmentsCount() {
        return assignments.stream().filter(a -> !a.isSubmitted()).count();
    }

    public long getSubmittedAssignmentsCount() {
        return assignments.stream().filter(Assignment::isSubmitted).count();
    }

    public void recalculateProgress() {
        if (assignments.isEmpty()) { this.progressPercent = 0.0; return; }
        double ratio = (double) getSubmittedAssignmentsCount() / assignments.size();
        this.progressPercent = Math.round(ratio * 100.0 * 100.0) / 100.0;
    }

    public String getDisplayLabel() {
        return courseCode + " • " + credits
                + (credits == 1 ? " Credit" : " Credits");
    }

    @Override
    public String toString() {
        return courseCode + " - " + courseName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Course other)) return false;
        return this.courseCode.equalsIgnoreCase(other.courseCode);
    }

    @Override
    public int hashCode() { return courseCode.toLowerCase().hashCode(); }
}