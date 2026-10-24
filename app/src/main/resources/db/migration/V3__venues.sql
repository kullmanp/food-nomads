create table venue (
    id          uuid primary key,
    name        text not null,
    address     text,
    proposed_by uuid not null references member (id),
    proposed_at timestamp with time zone not null
);

create index venue_proposed_at on venue (proposed_at);
