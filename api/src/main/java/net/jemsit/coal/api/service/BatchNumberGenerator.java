package net.jemsit.coal.api.service;

import jakarta.persistence.EntityManager;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.repo.CoalBatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Assigns party numbers of the form {@code П-yyMMdd-NNN} — the Cyrillic letter П, today's
 * date, and a counter that restarts at 001 every day.
 *
 * <p>The counter is derived from the numbers already in the table rather than stored
 * separately, so no extra table or migration is needed. Concurrency is handled by a
 * PostgreSQL transaction-scoped advisory lock keyed on the day: a second operator creating
 * a party on the same day waits here until the first transaction commits, then reads the
 * committed maximum. Without it, two simultaneous creates would both compute the same
 * number and one would die on the unique {@code batch_number} constraint.
 */
@Service
public class BatchNumberGenerator {

    /** Party numbers start with the Russian letter П, as they did when operators typed them. */
    static final String PREFIX = "П-";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyMMdd");
    private static final int SEQUENCE_WIDTH = 3;
    /** Namespace for the advisory-lock key, so it cannot collide with another lock in the DB. */
    private static final String LOCK_KEY = "coal:batch-number:";

    private final CoalBatchRepository batches;
    private final EntityManager em;
    private final ZoneId zone;

    public BatchNumberGenerator(CoalBatchRepository batches,
                                EntityManager em,
                                @Value("${coal.numbering.zone:Asia/Tashkent}") String zone) {
        this.batches = batches;
        this.em = em;
        this.zone = ZoneId.of(zone);
    }

    /**
     * Next free number for today. Must run inside the caller's transaction — that is what
     * makes the advisory lock cover the insert, and what makes the number part of the same
     * all-or-nothing unit as the party being created.
     */
    @Transactional
    public String next() {
        String prefix = PREFIX + LocalDate.now(zone).format(DAY) + "-";
        lockDay(prefix);
        return candidate(prefix);
    }

    /**
     * The number a party created right now would most likely get, for showing in a form
     * before anything is saved. Deliberately takes no lock: a preview must never hold up a
     * real create, and the answer is only an estimate — if another operator saves first, the
     * party gets the next number instead.
     */
    @Transactional(readOnly = true)
    public String preview() {
        return candidate(PREFIX + LocalDate.now(zone).format(DAY) + "-");
    }

    private String candidate(String prefix) {
        return prefix + String.format("%0" + SEQUENCE_WIDTH + "d", lastSequence(prefix) + 1);
    }

    /**
     * The number to store on a new party: the one an operator explicitly asked for, after a
     * duplicate check, or the next from today's counter when none was given. Shared by party
     * creation and pile splits so both paths behave identically.
     */
    @Transactional
    public String resolve(String requested) {
        String number = requested == null ? "" : requested.trim();
        if (number.isEmpty()) {
            return next();
        }
        if (batches.existsByBatchNumber(number)) {
            throw ApiException.conflict("Партия с номером " + number + " уже существует");
        }
        return number;
    }

    /** Serialises every creator working on the same day, until each of them commits. */
    private void lockDay(String prefix) {
        em.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:key, 0))")
                .setParameter("key", LOCK_KEY + prefix)
                .getSingleResult();
    }

    /**
     * Highest counter already issued for this day. Numbers that do not fit the pattern —
     * hand-typed legacy ones such as {@code П-001} cannot match the prefix anyway — are
     * ignored rather than breaking the whole allocation.
     */
    private int lastSequence(String prefix) {
        List<String> issued = batches.findNumbersStartingWith(prefix);
        int max = 0;
        for (String number : issued) {
            Integer sequence = sequenceOf(number, prefix);
            if (sequence != null && sequence > max) {
                max = sequence;
            }
        }
        return max;
    }

    private static Integer sequenceOf(String number, String prefix) {
        String tail = number.substring(prefix.length());
        // Deliberately narrow: a long digit run would overflow int and is not a number we issued.
        if (tail.isEmpty() || tail.length() > SEQUENCE_WIDTH + 2) {
            return null;
        }
        int value = 0;
        for (int i = 0; i < tail.length(); i++) {
            char c = tail.charAt(i);
            if (c < '0' || c > '9') {
                return null;
            }
            value = value * 10 + (c - '0');
        }
        return value;
    }
}
