package com.dashboard.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Assignment {

    private int       id;
    private int       studentId;
    private String    courseCode;
    private String    title;
    private String    description;
    private LocalDate dueDate;
    private boolean   submitted;
    private double    score;
    private String    priority;

    public Assignment() { this.score = -1; }

    public Assignment(int id, int studentId, String courseCode,
                      String title, String description,
                      LocalDate dueDate, String priority) {
        this.id          = id;
        this.studentId   = studentId;
        this.courseCode  = courseCode;
        this.title       = title;
        this.description = description;
        this.dueDate     = dueDate;
        this.submitted   = false;
        this.score       = -1;
        this.priority    = priority;
    }

    public int       getId()          { return id; }
    public int       getStudentId()   { return studentId; }
    public String    getCourseCode()  { return courseCode; }
    public String    getTitle()       { return title; }
    public String    getDescription() { return description; }
    public LocalDate getDueDate()     { return dueDate; }
    public boolean   isSubmitted()    { return submitted; }
    public double    getScore()       { return score; }
    public String    getPriority()    { return priority; }

    public void setSubmitted(boolean s) { this.submitted = s; }
    public void setScore(double s)      { this.score = s; }
    public void setPriority(String p)   { this.priority = p; }
    public void setDueDate(LocalDate d) { this.dueDate = d; }
    public void setTitle(String t)      { this.title = t; }
    public void setDescription(String d){ this.description = d; }

    public boolean isOverdue() {
        return !submitted && dueDate != null
                && dueDate.isBefore(LocalDate.now());
    }

    public long getDaysUntilDue() {
        if (dueDate == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    public String getDueDateLabel() {
        if (dueDate == null) return "No due date";
        long days = getDaysUntilDue();
        if (submitted)  return " Submitted";
        if (days < 0)   return "️ Overdue by " + Math.abs(days) + "d";
        if (days == 0)  return " Due today!";
        if (days == 1)  return "Due tomorrow";
        return "Due in " + days + " days";
    }

    public String getUrgencyStyle() {
        if (submitted)              return "submitted";
        if (isOverdue())            return "overdue";
        if (getDaysUntilDue() <= 2) return "urgent";
        return "normal";
    }

    @Override
    public String toString() {
        return String.format("[%s] %s — due %s", courseCode, title, dueDate);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Assignment other)) return false;
        return this.id == other.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}