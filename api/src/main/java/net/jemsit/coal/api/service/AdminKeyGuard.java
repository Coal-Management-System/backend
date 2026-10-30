package net.jemsit.coal.api.service;

import net.jemsit.coal.shared.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Shared secret-word check for the destructive "delete" endpoints. Mirrors the
 * original party-delete rule: the server must have a delete key configured, and
 * the caller must present it in the {@code X-Admin-Key} header. Compared with a
 * constant-time digest equality so a wrong key never leaks timing.
 */
@Component
public class AdminKeyGuard {

    private final String deleteKey;

    public AdminKeyGuard(@Value("${coal.admin.delete-key:}") String deleteKey) {
        this.deleteKey = deleteKey;
    }

    public void check(String adminKey) {
        if (deleteKey.isBlank()) {
            throw ApiException.forbidden("Удаление отключено на сервере");
        }
        if (adminKey == null || !MessageDigest.isEqual(
                deleteKey.getBytes(StandardCharsets.UTF_8),
                adminKey.getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.forbidden("Неверный ключ администратора");
        }
    }
}
