package com.dashboard.model;

/**
 * Base class for all users in the UCC portal.
 * Shared fields across Student, Lecturer and Admin.
 */
public class User {

    protected int    id;
    protected String name;
    protected String email;
    protected String password;
    protected String role;
    protected String createdAt;

    public User() {}

    public User(int id, String name, String email,
                String role) {
        this.id    = id;
        this.name  = name;
        this.email = email;
        this.role  = role;
    }

    // Getters & Setters
    public int    getId()        { return id; }
    public void   setId(int id)  { this.id = id; }

    public String getName()      { return name; }
    public void   setName(String name) { this.name = name.trim(); }

    public String getEmail()     { return email; }
    public void   setEmail(String email) {
        this.email = email.trim().toLowerCase();
    }

    public String getPassword()  { return password; }
    public void   setPassword(String password) {
        this.password = password;
    }

    public String getRole()      { return role; }
    public void   setRole(String role) { this.role = role; }

    public String getCreatedAt() { return createdAt; }
    public void   setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    // Helpers
    public String getFirstName() {
        if (name == null || name.isBlank()) return "";
        return name.trim().split("\\s+")[0];
    }

    public String getLastName() {
        if (name == null || name.isBlank()) return "";
        String[] parts = name.trim().split("\\s+");
        return parts.length > 1 ? parts[parts.length - 1] : "";
    }

    public boolean isStudent()  { return "STUDENT".equals(role); }
    public boolean isLecturer() { return "LECTURER".equals(role); }
    public boolean isAdmin()    { return "ADMIN".equals(role); }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, role);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User other)) return false;
        return this.id == other.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}