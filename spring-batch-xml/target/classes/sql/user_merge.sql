INSERT INTO users (first_name, last_name) SELECT :firstName, :lastName
WHERE NOT EXISTS ( SELECT 1 FROM users WHERE first_name = :firstName AND last_name = :lastName );