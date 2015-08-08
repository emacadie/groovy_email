CREATE TABLE email_user (
    userid serial primary key NOT NULL,
    username character varying(64) not null unique,
    password_hash character varying(150) not null,
    password_algo character varying(32) not null,
    iterations bigint not null,
    first_name character varying(30) not null,
    last_name character varying(30) not null,
    version bigint NOT NULL
);

