package com.team6.backend.app.entities;
/*

Code from Alexander Obregon :
https://medium.com/@AlexanderObregon/building-a-login-system-with-spring-boot-and-spring-security-2ef6f110a9cb
and my goat:
https://medium.com/@alxkm/lombok-essentials-enhancing-java-coding-efficiency-and-maintainability-1e8904dbefe1
*/

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * Maps to the app_users table in the db
 * CREATE TABLE IF NOT EXISTS app_users (
 *     id BIGSERIAL PRIMARY KEY,
 *     username TEXT NOT NULL UNIQUE,
 *     password TEXT NOT NULL,
 *     role TEXT NOT NULL DEFAULT 'USER'
 * );
 */
@Entity
@Table(name="app_users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE) 
    private Long id;

    private String username;
    private String password;
    private String role;

    @OneToMany(mappedBy = "user")
    private Set<UserProfile> profiles = new HashSet<>();

}



// @Entity
// @Table(name = "app_users")
// public class AppUser {
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;
//
//     private String username;
//     private String password;
//     private String role;
//
//     public Long getId() { return id; }
//     public String getUsername(){ return username;}
//     public String getPassword(){ return password;}
//     public String getRole(){ return role;}
//
//
//
//     public void setUsername(String username){  this.username = username;}
//     public void setPassword(String password){  this.password = password;}
//     public void setRole(String role){  this.role = role;}
// }


