package com.greencodes.greensky.core.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greencodes.greensky.integration.auth.AuthProvider;
import org.junit.jupiter.api.Test;

class AuthSettingsTest {

    private static final boolean OFFLINE = false;
    private static final boolean ONLINE = true;

    @Test
    void noneWithOfflineModeRefusesToStartWithoutDevMode() {
        AuthSettings unsafe = new AuthSettings(AuthProvider.NONE, false);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> unsafe.validateFor(OFFLINE));
        assertTrue(e.getMessage().contains("dev-mode"), e.getMessage());
    }

    @Test
    void noneWithOfflineModeStartsOnlyWithExplicitDevModeAndIsFlaggedInsecure() {
        AuthSettings dev = new AuthSettings(AuthProvider.NONE, true);
        assertDoesNotThrow(() -> dev.validateFor(OFFLINE));
        assertTrue(dev.insecureDevMode(OFFLINE));
    }

    @Test
    void noneWithOnlineModeIsSafeBecauseMojangAuthenticates() {
        AuthSettings none = new AuthSettings(AuthProvider.NONE, false);
        assertDoesNotThrow(() -> none.validateFor(ONLINE));
        assertFalse(none.insecureDevMode(ONLINE));
        assertFalse(new AuthSettings(AuthProvider.NONE, true).insecureDevMode(ONLINE));
    }

    @Test
    void nloginIsAlwaysAcceptedHere() {
        // A presença do plugin nLogin é conferida no boot do servidor, não aqui.
        assertDoesNotThrow(() -> new AuthSettings(AuthProvider.NLOGIN, false).validateFor(OFFLINE));
        assertFalse(new AuthSettings(AuthProvider.NLOGIN, true).insecureDevMode(OFFLINE));
    }
}
