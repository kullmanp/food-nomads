package ch.kup.foodnomads.members;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

/** A person in the group with access to the app. Their id is the security principal name. */
@Entity
public class Member extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "display_name", nullable = false)
    public String displayName;

    @Column(name = "joined_at", nullable = false)
    public Instant joinedAt;

    public String displayName() {
        return displayName;
    }
}
