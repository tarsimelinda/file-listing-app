package com.codecool.backend.service;

import org.springframework.stereotype.Component;

@Component
public class RunningUserProvider {

    public String getRunUser() {
        String userFromEnv = System.getenv("APP_RUN_USER");

        if (userFromEnv != null && !userFromEnv.isBlank()) {
            return userFromEnv;
        }

        return System.getProperty("user.name", "unknown");
    }

    public String getRunUid() {
        return System.getenv().getOrDefault("APP_RUN_UID", "unknown");
    }

    public String getRunGid() {
        return System.getenv().getOrDefault("APP_RUN_GID", "unknown");
    }
}
