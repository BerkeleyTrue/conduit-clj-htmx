-- :name create-session-table
-- :command :execute
-- :result :raw
-- :doc Create session table
CREATE TABLE IF NOT EXISTS sessions (
  session_id VARCHAR NOT NULL PRIMARY KEY,
  session_data BLOB
)

-- :name get-session-by-id :? :1
-- :doc Get session by id
SELECT session_data FROM sessions
WHERE session_id = :id

-- :name insert-session :! :n
-- :doc Insert a new session
INSERT INTO sessions (session_id, session_data)
VALUES (:id, :data)

-- :name update-session :! :n
-- :doc Update a session 
UPDATE sessions
SET session_data = :data
WHERE id = :id

-- :name delete-session-by-id :! :n
-- :doc Delete session
DELETE FROM sessions 
WHERE session_id = :id
