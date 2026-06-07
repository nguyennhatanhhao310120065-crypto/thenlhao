package com.akatsuki.model;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private int role; // 0=Admin, 1=Lecturer, 2=Student
    private String createdAt;

    public User() {}

    public User(int id, String username, int role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public int getRole() { return role; }
    public void setRole(int role) { this.role = role; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getRoleLabel() {
        return switch (role) {
            case 0 -> "Admin";
            case 1 -> "Lecturer";
            default -> "Student";
        };
    }
}
