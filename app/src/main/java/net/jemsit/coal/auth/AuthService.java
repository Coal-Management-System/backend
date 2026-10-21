package net.jemsit.coal.auth;

import net.jemsit.coal.auth.AuthDtos.LoginRequest;
import net.jemsit.coal.auth.AuthDtos.LoginResponse;
import net.jemsit.coal.auth.AuthDtos.UserDto;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.domain.AppUser;
import net.jemsit.coal.shared.domain.AuthSession;
import net.jemsit.coal.shared.repo.AppUserRepository;
import net.jemsit.coal.shared.repo.AuthSessionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

    private static final Duration SESSION_TTL = Duration.ofHours(12);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository users;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository users,
                       AuthSessionRepository sessions,
                       PasswordEncoder passwordEncoder) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        sessions.deleteByExpiresAtBefore(Instant.now());
        AppUser user = users.findByUsername(req.username().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Неверный логин или пароль"));
        AuthSession session = new AuthSession();
        session.setToken(newToken());
        session.setUser(user);
        session.setExpiresAt(Instant.now().plus(SESSION_TTL));
        sessions.save(session);
        return new LoginResponse(session.getToken(), toDto(user));
    }

    @Transactional
    public void logout(String token) {
        if (token == null) {
            return;
        }
        sessions.findWithUserByToken(token).ifPresent(sessions::delete);
    }

    @Transactional(readOnly = true)
    public UserDto me(String token) {
        return toDto(resolveUser(token)
                .orElseThrow(() -> ApiException.unauthorized("Требуется вход в систему")));
    }

    /** The user behind a bearer token, empty when the token is unknown or expired. */
    @Transactional(readOnly = true)
    public Optional<AppUser> resolveUser(String token) {
        if (token == null) {
            return Optional.empty();
        }
        return sessions.findWithUserByToken(token)
                .filter(s -> s.getExpiresAt().isAfter(Instant.now()))
                .map(AuthSession::getUser);
    }

    public static UserDto toDto(AppUser user) {
        return new UserDto(user.getId(), user.getUsername(), user.getFullName(),
                user.getRole(), user.getShift());
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
