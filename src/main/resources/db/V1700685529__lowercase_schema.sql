-- In Postgres, unquoted object names are automatically lower case. It is possible to use case sensitive object names
-- by using " quoting but it is not recommended. So, we take a step back and adopt snake_case naming consistently.

DROP TABLE IF EXISTS user_access_token;
DROP TABLE IF EXISTS user_user;

CREATE TABLE user_user(
    user_id uuid NOT NULL,
    name VARCHAR NOT NULL,
    PRIMARY KEY (user_id)
);

CREATE TABLE user_access_token(
    access_token CHAR(32) NOT NULL,
    user_id uuid NOT NULL,
    PRIMARY KEY (access_token),
    CONSTRAINT at_user_id FOREIGN KEY(user_id) REFERENCES user_user(user_id)
);
