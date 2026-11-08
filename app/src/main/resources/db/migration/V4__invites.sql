create table invite (
    id          uuid primary key,
    token       text not null unique,
    created_by  uuid not null references member (id),
    created_at  timestamp with time zone not null,
    redeemed_by uuid references member (id),
    redeemed_at timestamp with time zone
);
