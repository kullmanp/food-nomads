package ch.kup.foodnomads.members;

import java.util.Optional;
import java.util.UUID;

/** Where Members are kept (ADR-0005). */
public interface MemberRepository {

    void add(Member member);

    Optional<Member> find(UUID id);

    long count();
}
