package com.dashboard.model;

/**
 * Represents a system administrator in the UCC portal.
 * Has full access to all data and management functions.
 */
public class Admin extends User {

    private int adminId;

    public Admin() {}

    public Admin(int id, String name, String email) {
        super(id, name, email, "ADMIN");
    }

    public int  getAdminId()      { return adminId; }
    public void setAdminId(int id){ this.adminId = id; }

    @Override
    public String toString() {
        return "Admin: " + name;
    }
}