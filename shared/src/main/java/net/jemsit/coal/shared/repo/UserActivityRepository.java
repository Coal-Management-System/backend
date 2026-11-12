package net.jemsit.coal.shared.repo;

import jakarta.persistence.criteria.Predicate;
import net.jemsit.coal.shared.domain.ActivityKind;
import net.jemsit.coal.shared.domain.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserActivityRepository extends JpaRepository<UserActivity, UUID>,
        JpaSpecificationExecutor<UserActivity> {

    /**
     * Newest first; every filter is optional (null = any). Built as a Specification so only the
     * filters actually set reach SQL: "? is null" on an untyped null fails on PostgreSQL.
     */
    default Page<UserActivity> search(String username, ActivityKind kind, Instant from, Instant to,
                                      Pageable pageable) {
        Specification<UserActivity> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (username != null) where.add(cb.equal(root.get("username"), username));
            if (kind != null) where.add(cb.equal(root.get("kind"), kind));
            if (from != null) where.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), from));
            if (to != null) where.add(cb.lessThan(root.<Instant>get("createdAt"), to));
            return cb.and(where.toArray(Predicate[]::new));
        };
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return findAll(spec, sorted);
    }

    /** The user's latest entry, used to skip repeated visits of the same page. */
    Optional<UserActivity> findFirstByUsernameOrderByCreatedAtDesc(String username);

    @Query("select distinct a.username from UserActivity a order by a.username")
    List<String> usernames();
}
