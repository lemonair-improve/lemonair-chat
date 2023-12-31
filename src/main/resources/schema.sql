CREATE TABLE chat
(
    id SERIAL PRIMARY KEY,
    sender VARCHAR(255) NOT NULL,
    message VARCHAR(255) NOT NULL,
    room_id VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);


TRUNCATE TABLE chat;


SELECT * FROM chat WHERE sender ='system';

SELECT
    id,
    sender,
    message,
    room_id,
    created_at,
    SUBSTRING_INDEX(SUBSTRING_INDEX(message, 'VU', -1), ' ', 1)-1 + 1  AS vu_number
FROM
    chat
WHERE
    sender ='system';
