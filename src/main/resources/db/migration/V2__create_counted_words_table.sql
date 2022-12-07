CREATE TABLE IF NOT EXISTS counted_words(
    id serial primary key,
    actor_name varchar(255) not null unique,
    c_words INTEGER not null
)