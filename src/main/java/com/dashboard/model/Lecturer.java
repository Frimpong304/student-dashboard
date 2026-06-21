package com.dashboard.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a lecturer in the UCC portal.
 * Extends User with department, title and course list.
 */
public class Lecturer extends User {

    private int    lecturerId;
    private String department;
    private String title;        // Mr. / Mrs. / Dr. / Prof.
    private List<Course> courses;

    public Lecturer() {
        this.courses = new ArrayList<>();
    }

    public Lecturer(int id, int lecturerId, String name,
                    String email, String title,
                    String department) {
        super(id, name, email, "LECTURER");
        this.lecturerId = lecturerId;
        this.title      = title;
        this.department = department;
        this.courses    = new ArrayList<>();
    }

    // Getters & Setters
    public int    getLecturerId()  { return lecturerId; }
    public void   setLecturerId(int id) { this.lecturerId = id; }

    public String getDepartment()  { return department; }
    public void   setDepartment(String d) { this.department = d; }

    public String getTitle()       { return title; }
    public void   setTitle(String t) { this.title = t; }

    public List<Course> getCourses() {
        return Collections.unmodifiableList(courses);
    }

    public void addCourse(Course course) {
        if (course != null && !courses.contains(course))
            courses.add(course);
    }

    public void removeCourse(Course course) {
        courses.remove(course);
    }

    /**
     * Returns full name with title e.g. "Dr. Emmanuel Tetteh"
     */
    public String getFullTitle() {
        return (title != null ? title + " " : "") + name;
    }

    /**
     * Returns initials for avatar display e.g. "ET"
     */
    public String getInitials() {
        if (name == null || name.isBlank()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1)
                + parts[parts.length - 1].substring(0, 1))
                .toUpperCase();
    }

    @Override
    public String toString() {
        return getFullTitle() + " — " + department;
    }
}