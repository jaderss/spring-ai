create table customer (
    id        varchar(16)  primary key,
    full_name varchar(200) not null
);

create table account (
    number      varchar(16)   primary key,
    customer_id varchar(16)   not null references customer (id),
    type        varchar(16)   not null check (type in ('CHECKING', 'SAVINGS')),
    currency    char(3)       not null,
    balance     numeric(19,4) not null,
    opened_at   timestamptz   not null
);

create table account_transaction (
    id             uuid          primary key,
    account_number varchar(16)   not null references account (number),
    posted_at      timestamptz   not null,
    description    varchar(200)  not null,
    memo           varchar(500),
    amount         numeric(19,4) not null,
    currency       char(3)       not null
);

create index account_transaction_by_account_and_time on account_transaction (account_number, posted_at desc);

create table transfer (
    id           uuid          primary key,
    from_account varchar(16)   not null references account (number),
    to_account   varchar(16)   not null references account (number),
    amount       numeric(19,4) not null check (amount > 0),
    currency     char(3)       not null,
    created_at   timestamptz   not null,
    check (from_account <> to_account)
);
