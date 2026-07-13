package com.codecool.backend.service;

import org.junit.jupiter.api.Test;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RunningUserProviderTest {

    private final RunningUserProvider runningUserProvider =
            new RunningUserProvider();

    @Test
    void getRunUserShouldReturnEnvironmentVariableWhenItIsPresent()
            throws Exception {

        new EnvironmentVariables(
                "APP_RUN_USER",
                "container-user"
        ).execute(() -> {
            String result = runningUserProvider.getRunUser();

            assertEquals("container-user", result);
        });
    }

    @Test
    void getRunUserShouldReturnSystemUserNameWhenEnvironmentVariableIsMissing()
            throws Exception {

        new EnvironmentVariables()
                .remove("APP_RUN_USER")
                .execute(() ->
                        new SystemProperties(
                                "user.name",
                                "local-user"
                        ).execute(() -> {
                            String result =
                                    runningUserProvider.getRunUser();

                            assertEquals("local-user", result);
                        })
                );
    }

    @Test
    void getRunUserShouldReturnSystemUserNameWhenEnvironmentVariableIsBlank()
            throws Exception {

        new EnvironmentVariables(
                "APP_RUN_USER",
                " "
        ).execute(() ->
                new SystemProperties(
                        "user.name",
                        "local-user"
                ).execute(() -> {
                    String result =
                            runningUserProvider.getRunUser();

                    assertEquals("local-user", result);
                })
        );
    }

    @Test
    void getRunUserShouldReturnUnknownWhenEnvironmentVariableAndUserNameAreMissing()
            throws Exception {

        new EnvironmentVariables()
                .remove("APP_RUN_USER")
                .execute(() ->
                        new SystemProperties()
                                .remove("user.name")
                                .execute(() -> {
                                    String result =
                                            runningUserProvider.getRunUser();

                                    assertEquals("unknown", result);
                                })
                );
    }

    @Test
    void getRunUidShouldReturnEnvironmentVariableWhenItIsPresent()
            throws Exception {

        new EnvironmentVariables(
                "APP_RUN_UID",
                "1000"
        ).execute(() -> {
            String result = runningUserProvider.getRunUid();

            assertEquals("1000", result);
        });
    }

    @Test
    void getRunUidShouldReturnUnknownWhenEnvironmentVariableIsMissing()
            throws Exception {

        new EnvironmentVariables()
                .remove("APP_RUN_UID")
                .execute(() -> {
                    String result = runningUserProvider.getRunUid();

                    assertEquals("unknown", result);
                });
    }

    @Test
    void getRunGidShouldReturnEnvironmentVariableWhenItIsPresent()
            throws Exception {

        new EnvironmentVariables(
                "APP_RUN_GID",
                "1001"
        ).execute(() -> {
            String result = runningUserProvider.getRunGid();

            assertEquals("1001", result);
        });
    }

    @Test
    void getRunGidShouldReturnUnknownWhenEnvironmentVariableIsMissing()
            throws Exception {

        new EnvironmentVariables()
                .remove("APP_RUN_GID")
                .execute(() -> {
                    String result = runningUserProvider.getRunGid();

                    assertEquals("unknown", result);
                });
    }
}