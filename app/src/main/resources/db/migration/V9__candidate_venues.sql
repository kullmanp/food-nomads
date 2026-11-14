-- The Venues Members put up for a Lunch. A Venue is a Candidate Venue of a Lunch at most once.
-- Lunches checks that too, under the Lunch's row lock; the constraint is the backstop.
create table candidate_venue (
    id       uuid primary key,
    lunch_id uuid not null references lunch (id),
    venue_id uuid not null references venue (id),
    constraint candidate_venue_once unique (lunch_id, venue_id)
);

create index candidate_venue_venue_id on candidate_venue (venue_id);
