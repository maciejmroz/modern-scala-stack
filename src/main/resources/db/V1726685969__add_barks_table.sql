
CREATE TABLE bark_bark(
    bark_id uuid NOT NULL,
    author_id uuid NOT NULL,
    content varchar NOT NULL,
    rebark_from_id uuid DEFAULT NULL,
    created_at timestamp NOT NULL,
    likes int DEFAULT 0,
    rebarks int DEFAULT 0,
    PRIMARY KEY (bark_id),
    FOREIGN KEY (author_id) REFERENCES user_user(user_id),
    FOREIGN KEY (rebark_from_id) REFERENCES bark_bark(bark_id)
);
