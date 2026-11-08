package ch.kup.foodnomads.members;

import java.util.Optional;
import java.util.UUID;

/** Where Members are kept (ADR-0005). */
public interface MemberRepository {

    /**
     * Stores a new Member. Two people picking the same name at once are only stopped by the
     * storage itself, which fails with {@link DisplayNameTakenException}.
     */
    void add(Member member);

    Optional<Member> find(UUID id);

    /** Whether a Member's display name, lowercased, equals this already lowercased name. */
    boolean existsWithLowercaseDisplayName(String lowercaseName);

    long count();
}
