CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       username VARCHAR(100) NOT NULL,
                       is_public BOOLEAN DEFAULT true,
                       created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE books (
                       id SERIAL PRIMARY KEY,
                       ol_id VARCHAR(50) UNIQUE NOT NULL,
                       title VARCHAR(255) NOT NULL,
                       author VARCHAR(255),
                       cover_url VARCHAR(500),
                       page_count INT,
                       description TEXT
);

CREATE TABLE reading_list (
                              id SERIAL PRIMARY KEY,
                              user_id INT REFERENCES users(id),
                              book_id INT REFERENCES books(id),
                              status VARCHAR(20) NOT NULL,
                              added_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE reading_progress (
                                  id SERIAL PRIMARY KEY,
                                  user_id INT REFERENCES users(id),
                                  book_id INT REFERENCES books(id),
                                  current_page INT DEFAULT 0,
                                  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE ratings (
                         id SERIAL PRIMARY KEY,
                         user_id INT REFERENCES users(id),
                         book_id INT REFERENCES books(id),
                         score SMALLINT CHECK (score BETWEEN 1 AND 5),
                         review TEXT,
                         created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE goals (
                       id SERIAL PRIMARY KEY,
                       user_id INT REFERENCES users(id),
                       year SMALLINT NOT NULL,
                       target_books INT NOT NULL
);