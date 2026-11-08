package ch.kup.foodnomads.members;

import java.util.LinkedHashMap;
import java.util.Locale;
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
    public boolean existsWithLowercaseDisplayName(String lowercaseName) {
        return members.values().stream()
                .anyMatch(member -> member.displayName.toLowerCase(Locale.ROOT).equals(lowercaseName));
    }

    @Override
    public long count() {
        return members.size();
    }
}
