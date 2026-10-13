ALTER TABLE repair_tickets ADD COLUMN imei_number VARCHAR(40) NULL;
UPDATE repair_tickets SET imei_number = (SELECT imei_number FROM warranty_cards WHERE warranty_cards.id = repair_tickets.warranty_id) WHERE imei_number IS NULL;

CREATE TABLE warranty_exchanges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    warranty_id BIGINT NOT NULL,
    old_imei VARCHAR(40) NOT NULL,
    new_imei VARCHAR(40) NOT NULL,
    actor_id BIGINT NOT NULL,
    reason VARCHAR(500) NULL,
    occurred_at DATETIME(6) NOT NULL,
    FOREIGN KEY(warranty_id) REFERENCES warranty_cards(id) ON DELETE CASCADE
);

CREATE INDEX ix_warranty_exchanges_warranty ON warranty_exchanges(warranty_id);
