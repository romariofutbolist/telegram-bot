-- liquibase formatted sql

-- chandeset artemev:1

create TABLE notification_task
(
  id           BIGSERIAL primary key,
  chat_id      BIGINT,
  text         VARCHAR,
  task_date    TIMESTAMP
)