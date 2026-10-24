create table member (
    id           uuid primary key,
    display_name varchar(100) not null,
    joined_at    timestamp with time zone not null
);

-- One row per passkey. Columns follow WebAuthnCredentialRecord.RequiredPersistedData.
create table passkey (
    credential_id        varchar(255) primary key,
    member_id            uuid not null references member (id),
    aaguid               uuid,
    public_key           bytea not null,
    public_key_algorithm bigint not null,
    counter              bigint not null
);

create index passkey_member_id on passkey (member_id);
