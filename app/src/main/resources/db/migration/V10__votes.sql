-- The Members' Votes on Candidate Venues. A Member votes at most once per Candidate Venue; voting
-- again changes the direction. Lunches checks that under the Lunch's row lock; the constraint is
-- the backstop. Removing a Candidate Venue removes its Votes first.
create table vote (
    id                 uuid primary key,
    candidate_venue_id uuid not null references candidate_venue (id),
    member_id          uuid not null references member (id),
    direction          text not null,
    constraint vote_once unique (candidate_venue_id, member_id)
);

create index vote_member_id on vote (member_id);
