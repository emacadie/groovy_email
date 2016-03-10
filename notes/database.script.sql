CREATE TABLE email_user (
    userid serial primary key NOT NULL,
    username character varying( 64 ) not null unique,
    password_hash character varying( 150 ) not null,
    password_algo character varying( 32 ) not null,
    iterations bigint not null,
    base_64_hash character varying( 150 ) not null,
    first_name character varying( 30 ) not null,
    last_name character varying( 30 ) not null,
    logged_in boolean not null,
    version bigint NOT NULL
);

-- change this name later
create table mail_store (
    id UUID PRIMARY KEY  NOT NULL unique,
    username character varying( 64 ) not null,
    from_address character varying( 255 ) not null,
    to_address character varying( 255 ) not null,
    message bytea,
    text_body text not null,
    msg_timestamp TIMESTAMP WITH TIME ZONE default clock_timestamp() not null,
    FOREIGN KEY ( username ) REFERENCES email_user ( username ) on delete cascade
);

create table mail_spool_in (
    id UUID PRIMARY KEY  NOT NULL unique,
    from_address character varying( 255 ) not null,
    to_address_list text not null,
    message bytea,
    text_body text not null,
    from_user_logged_in boolean,
    status_string text,
    msg_timestamp TIMESTAMP WITH TIME ZONE default clock_timestamp() not null
);

create table mail_spool_out (
    id UUID PRIMARY KEY  NOT NULL unique,
    from_address character varying( 255 ) not null,
    to_address_list text not null,
    message bytea,
    text_body text not null,
    from_user_logged_in boolean,
    status_string text,
    msg_timestamp TIMESTAMP WITH TIME ZONE default clock_timestamp() not null
);


