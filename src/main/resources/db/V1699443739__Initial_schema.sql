CREATE TABLE user_user(
    userId uuid NOT NULL,
    name VARCHAR NOT NULL,
    PRIMARY KEY (userId)
);

CREATE TABLE user_access_token(
    accessToken CHAR(32) NOT NULL,
    userId uuid NOT NULL,
    PRIMARY KEY (accessToken),
    CONSTRAINT at_userId FOREIGN KEY(userId) REFERENCES user_user(userId)
);
