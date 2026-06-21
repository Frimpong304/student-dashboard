package com.dashboard.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Quiz {

    private int       id;
    private int       studentId;
    private String    courseCode;
    private String    title;
    private LocalDate dueDate;
    private boolean   completed;
    private double    score;
    private int       totalQuestions;
    private int       durationMinutes;

    public Quiz() { this.score = -1; }

    public Quiz(int id, int studentId, String courseCode, String title,
                LocalDate dueDate, int totalQuestions, int durationMinutes) {
        this.id              = id;
        this.studentId       = studentId;
        this.courseCode      = courseCode;
        this.title           = title;
        this.dueDate         = dueDate;
        this.completed       = false;
        this.score           = -1;
        this.totalQuestions  = totalQuestions;
        this.durationMinutes = durationMinutes;
    }

    public int       getId()              { return id; }
    public int       getStudentId()       { return studentId; }
    public String    getCourseCode()      { return courseCode; }
    public String    getTitle()           { return title; }
    public LocalDate getDueDate()         { return dueDate; }
    public boolean   isCompleted()        { return completed; }
    public double    getScore()           { return score; }
    public int       getTotalQuestions()  { return totalQuestions; }
    public int       getDurationMinutes() { return durationMinutes; }

    public void setCompleted(boolean c) { this.completed = c; }
    public void setScore(double s)      { this.score = s; }
    public void setDueDate(LocalDate d) { this.dueDate = d; }

    public boolean isOverdue() {
        return !completed && dueDate != null
                && dueDate.isBefore(LocalDate.now());
    }

    public long getDaysUntilDue() {
        if (dueDate == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    public String getScoreDisplay() {
        if (!completed || score < 0) return "Not taken";
        return String.format("%.1f%%", score);
    }

    public String getStatusLabel() {
        if (completed)   return "Completed — " + getScoreDisplay();
        if (isOverdue()) return " Overdue";
        long days = getDaysUntilDue();
        if (days == 0)   return " Due today!";
        if (days == 1)   return "Due tomorrow";
        return "Due in " + days + " days";
    }

    public String getDurationLabel() {
        return durationMinutes + " min • " + totalQuestions + " questions";
    }

    @Override
    public String toString() {
        return String.format("[%s] %s — %s", courseCode, title, getStatusLabel());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Quiz other)) return false;
        return this.id == other.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}