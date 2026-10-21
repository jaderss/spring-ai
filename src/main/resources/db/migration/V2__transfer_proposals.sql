create table transfer_proposal (
    id              uuid          primary key,
    customer_id     varchar(16)   not null references customer (id),
    from_account    varchar(16)   not null references account (number),
    to_account      varchar(16)   not null references account (number),
    amount          numeric(19,4) not null check (amount > 0),
    currency        char(3)       not null,
    expires_at      timestamptz   not null,
    transfer_id     uuid          unique references transfer (id),
    idempotency_key varchar(64)   unique,
    check ((transfer_id is null) = (idempotency_key is null))
);

create index transfer_proposal_pending_by_customer on transfer_proposal (customer_id) where transfer_id is null;
