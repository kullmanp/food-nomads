-- The Members' answers, in or out, for a Lunch. A Member answers at most once per Lunch; answering
-- again changes the answer. Lunches checks that under the Lunch's row lock; the constraint is the
-- backstop. Moving the Lunch deletes its RSVPs.
create table rsvp (
    id        uuid primary key,
    lunch_id  uuid not null references lunch (id),
    member_id uuid not null references member (id),
    answer    text not null,
    constraint rsvp_once unique (lunch_id, member_id)
);

create index rsvp_member_id on rsvp (member_id);
