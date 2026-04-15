package com.jobplatform.authservice;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String username;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private boolean forcePasswordChange = true;

  private String role = "USER";

  public Long getId() { return id; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

  public boolean isForcePasswordChange() { return forcePasswordChange; }
  public void setForcePasswordChange(boolean forcePasswordChange) {
    this.forcePasswordChange = forcePasswordChange;
  }

  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
}
