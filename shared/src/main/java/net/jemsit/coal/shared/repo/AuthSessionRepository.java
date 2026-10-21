package net.jemsit.coal.shared.repo;

import net.jemsit.coal.shared.domain.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    // join fetch keeps the user usable outside the loading transaction (open-in-view is off).
    @Query("select s from AuthSession s join fetch s.user where s.token = :token")
    Optional<AuthSession> findWithUserByToken(@Param("token") String token);

    void deleteByExpiresAtBefore(Instant moment);
}
