-- Undirected friendship: a single durable row represents both players' lists.
CREATE TABLE IF NOT EXISTS gameplay_friendships (
    user_low INT NOT NULL,
    user_high INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_low, user_high),
    KEY friend_high (user_high),
    CHECK (user_low < user_high)
) ENGINE=InnoDB;
