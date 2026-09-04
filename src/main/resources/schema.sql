-- Drop the stale check constraint that Hibernate 7 auto-generated from the
-- OrderStatus enum at table-creation time.  `ddl-auto=update` never revisits
-- existing CHECK constraints, so when enum values were added later the constraint
-- became stale and started rejecting valid values (e.g. PAID).
-- Hibernate will recreate a correct constraint on next full schema creation.
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_order_status_check;
