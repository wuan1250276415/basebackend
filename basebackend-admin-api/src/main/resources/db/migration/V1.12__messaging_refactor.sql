-- Messaging module refactor: remove RabbitMQ routing_key columns
ALTER TABLE sys_message_log DROP COLUMN routing_key;
ALTER TABLE sys_dead_letter DROP COLUMN routing_key;
