CREATE TABLE operators (
    id   BIGINT      NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_operators_code UNIQUE (code),
    CONSTRAINT uk_operators_name UNIQUE (name)
);

CREATE TABLE number_ranges (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    operator_id BIGINT   NOT NULL,
    range_start CHAR(11) NOT NULL,
    range_end   CHAR(11) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_number_ranges_operator FOREIGN KEY (operator_id) REFERENCES operators (id),
    CONSTRAINT ck_number_ranges_order CHECK (range_start <= range_end)
);

CREATE TABLE porting_requests (
    id                    BIGINT      NOT NULL AUTO_INCREMENT,
    phone_number          CHAR(11)    NOT NULL,
    donor_operator_id     BIGINT      NOT NULL,
    recipient_operator_id BIGINT      NOT NULL,
    status                VARCHAR(20) NOT NULL,
    created_at            DATETIME(6) NOT NULL,
    updated_at            DATETIME(6) NOT NULL,
    version               BIGINT      NOT NULL DEFAULT 0,

    pending_phone_number  CHAR(11) GENERATED ALWAYS AS
        (CASE WHEN status = 'PENDING' THEN phone_number END) STORED,

    PRIMARY KEY (id),
    CONSTRAINT fk_porting_requests_donor     FOREIGN KEY (donor_operator_id)     REFERENCES operators (id),
    CONSTRAINT fk_porting_requests_recipient FOREIGN KEY (recipient_operator_id) REFERENCES operators (id),
    CONSTRAINT ck_porting_requests_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELED')),
    CONSTRAINT ck_porting_requests_parties CHECK (donor_operator_id <> recipient_operator_id),
    CONSTRAINT uk_porting_requests_one_pending UNIQUE (pending_phone_number)
);

CREATE INDEX idx_porting_requests_phone_number ON porting_requests (phone_number, status);
CREATE INDEX idx_porting_requests_status_created ON porting_requests (status, created_at);