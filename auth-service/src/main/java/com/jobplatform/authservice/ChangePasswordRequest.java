package com.jobplatform.authservice;

public record ChangePasswordRequest(String username, String currentPassword, String newPassword) {}
