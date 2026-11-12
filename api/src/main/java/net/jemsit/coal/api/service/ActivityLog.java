package net.jemsit.coal.api.service;

import net.jemsit.coal.shared.domain.ActivityKind;
import net.jemsit.coal.shared.domain.AppUser;
import net.jemsit.coal.shared.domain.UserActivity;
import net.jemsit.coal.shared.repo.AppUserRepository;
import net.jemsit.coal.shared.repo.CoalBatchRepository;
import net.jemsit.coal.shared.repo.CoalPileRepository;
import net.jemsit.coal.shared.repo.WarehouseRepository;
import net.jemsit.coal.shared.repo.UserActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Writes the admin activity journal. Called by controllers after the operation succeeded,
 * so failed requests leave no entry. A journal failure is only logged: it must never
 * break the user's actual work.
 */
@Component
public class ActivityLog {

    private static final Logger log = LoggerFactory.getLogger(ActivityLog.class);
    /** Re-opening the same page within this window is not written again. */
    private static final Duration VISIT_DEDUP = Duration.ofMinutes(10);

    private final UserActivityRepository activities;
    private final AppUserRepository users;
    private final CoalBatchRepository batches;
    private final CoalPileRepository piles;
    private final WarehouseRepository warehouses;

    public ActivityLog(UserActivityRepository activities, AppUserRepository users,
                       CoalBatchRepository batches, CoalPileRepository piles,
                       WarehouseRepository warehouses) {
        this.activities = activities;
        this.users = users;
        this.batches = batches;
        this.piles = piles;
        this.warehouses = warehouses;
    }

    // Number lookups for summaries; "?" if the record is gone, never an exception.

    public String batchNumber(UUID id) {
        return id == null ? "—" : batches.findById(id).map(b -> b.getBatchNumber()).orElse("?");
    }

    public String pileNumber(UUID id) {
        return id == null ? "—" : piles.findById(id).map(p -> p.getPileNumber()).orElse("?");
    }

    public String warehouseNumber(UUID id) {
        return id == null ? "—" : warehouses.findById(id).map(w -> w.getNumber()).orElse("?");
    }

    public static String tons(BigDecimal v) {
        return v == null ? "—" : v.stripTrailingZeros().toPlainString() + " т";
    }

    /** Entry for the user of the current request. */
    public void record(ActivityKind kind, String entityType, String entityLabel, String summary) {
        String username = currentUsername();
        if (username != null) {
            recordFor(username, kind, entityType, entityLabel, summary);
        }
    }

    /** Entry for a named user, for login, where the request has no principal yet. */
    public void recordFor(String username, ActivityKind kind, String entityType,
                          String entityLabel, String summary) {
        try {
            String fullName = users.findByUsername(username).map(AppUser::getFullName).orElse(null);
            activities.save(new UserActivity(username, fullName, kind, entityType,
                    cut(entityLabel, 200), cut(summary, 500)));
        } catch (RuntimeException e) {
            log.warn("Could not write activity entry for {}: {}", username, e.toString());
        }
    }

    /** A menu page was opened. Skipped when it is the same page as the user's last entry, a moment ago. */
    public void visit(String path, String title) {
        String username = currentUsername();
        if (username == null || path == null || path.isBlank()) {
            return;
        }
        boolean repeat = activities.findFirstByUsernameOrderByCreatedAtDesc(username)
                .filter(last -> last.getKind() == ActivityKind.VISIT
                        && path.equals(last.getEntityLabel())
                        && last.getCreatedAt().isAfter(Instant.now().minus(VISIT_DEDUP)))
                .isPresent();
        if (!repeat) {
            String name = title == null || title.isBlank() ? path : title;
            recordFor(username, ActivityKind.VISIT, "PAGE", path, "Открыл страницу «" + name + "»");
        }
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return auth.getName();
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
