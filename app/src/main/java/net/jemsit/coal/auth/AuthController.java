package net.jemsit.coal.auth;

import jakarta.validation.Valid;
import net.jemsit.coal.api.service.ActivityLog;
import net.jemsit.coal.shared.domain.ActivityKind;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;
    private final ActivityLog activity;

    public AuthController(AuthService service, ActivityLog activity) {
        this.service = service;
        this.activity = activity;
    }

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        AuthDtos.LoginResponse res = service.login(req);
        activity.recordFor(res.user().username(), ActivityKind.LOGIN, null, null, "Вошел в систему");
        return res;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String header) {
        // Before the session is gone; the token filter already set the principal.
        activity.record(ActivityKind.LOGOUT, null, null, "Вышел из системы");
        service.logout(AuthTokens.extract(header));
    }

    @GetMapping("/me")
    public AuthDtos.UserDto me(@RequestHeader(value = "Authorization", required = false) String header) {
        return service.me(AuthTokens.extract(header));
    }
}
