package net.jemsit.coal.auth;

public final class AuthTokens {

    private static final String PREFIX = "Bearer ";

    private AuthTokens() {
    }

    /** Returns the bearer token from an Authorization header, or null when absent/malformed. */
    public static String extract(String header) {
        if (header == null || !header.startsWith(PREFIX)) {
            return null;
        }
        String token = header.substring(PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
