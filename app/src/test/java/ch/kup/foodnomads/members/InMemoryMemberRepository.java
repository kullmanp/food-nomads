package ch.kup.foodnomads.members;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** A {@link MemberRepository} for unit tests. */
class InMemoryMemberRepository implements MemberRepository {

    private final Map<UUID, Member> members = new LinkedHashMap<>();

    @Override
    public void add(Member member) {
        members.put(member.id, member);
    }

    @Override
    public Optional<Member> find(UUID id) {
        return Optional.ofNullable(members.get(id));
    }

    @Override
    public long count() {
        return members.size();
    }
}
