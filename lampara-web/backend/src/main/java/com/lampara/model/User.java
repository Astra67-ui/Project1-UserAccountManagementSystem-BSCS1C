package com.lampara.model;
// User Class collecting the data from users and sending it to the database
public class User {
    public int    user_id;
    public String firstName;
    public String lastName;
    public String email;
    public String username;
    public String password; 
    public boolean isVerified;
    public String role;

    public User() {}

    public User(int user_id, String firstName, String lastName,
                String email, String username, String password, boolean isVerified, String role) {
        this.user_id    = user_id;
        this.firstName  = firstName;
        this.lastName   = lastName;
        this.email      = email;
        this.username   = username;
        this.password   = password;
        this.isVerified = isVerified;
        this.role = role;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
