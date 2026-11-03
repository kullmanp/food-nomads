package ch.kup.foodnomads.members;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

/** Keeps Members in PostgreSQL. Callers run it inside a transaction. */
@ApplicationScoped
class PanacheMemberRepository implements MemberRepository {

    @Override
    public void add(Member member) {
        member.persist();
    }

    @Override
    public Optional<Member> find(UUID id) {
        return Member.findByIdOptional(id);
    }

    @Override
    public long count() {
        return Member.count();
    }
}
