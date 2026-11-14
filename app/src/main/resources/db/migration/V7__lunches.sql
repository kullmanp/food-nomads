create table lunch (
    id         uuid primary key,
    date       date not null,
    state      text not null,
    created_at timestamp with time zone not null
);

-- At most one Planned Lunch. Two Members planning at once both pass the check in Lunches; this stops the second.
create unique index lunch_one_planned on lunch (state) where state = 'PLANNED';

create table lunch_comment (
    id         uuid primary key,
    lunch_id   uuid not null references lunch (id),
    author_id  uuid not null references member (id),
    text       text not null,
    written_at timestamp with time zone not null
);

create index lunch_comment_lunch_id on lunch_comment (lunch_id);
