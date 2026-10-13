-- FHIR domain baseline schema. Identical in all variants (copy byte-for-byte).

CREATE SEQUENCE resource_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE patient (
    id            BIGINT       PRIMARY KEY,
    version       INT          NOT NULL,
    last_updated  TIMESTAMP WITH TIME ZONE NOT NULL,
    active        BOOLEAN,
    family        VARCHAR(255),
    gender        VARCHAR(16),
    birth_date    DATE
);

CREATE TABLE patient_given (
    patient_id    BIGINT       NOT NULL REFERENCES patient(id),
    position      INT          NOT NULL,
    given         VARCHAR(255) NOT NULL,
    PRIMARY KEY (patient_id, position)
);

CREATE TABLE patient_identifier (
    patient_id    BIGINT       NOT NULL REFERENCES patient(id),
    position      INT          NOT NULL,
    ident_system  VARCHAR(512),
    ident_value   VARCHAR(255),
    PRIMARY KEY (patient_id, position)
);

CREATE TABLE patient_telecom (
    patient_id    BIGINT       NOT NULL REFERENCES patient(id),
    position      INT          NOT NULL,
    telecom_system VARCHAR(16),
    telecom_value  VARCHAR(255),
    PRIMARY KEY (patient_id, position)
);

CREATE TABLE practitioner (
    id            BIGINT       PRIMARY KEY,
    version       INT          NOT NULL,
    last_updated  TIMESTAMP WITH TIME ZONE NOT NULL,
    active        BOOLEAN,
    family        VARCHAR(255)
);

CREATE TABLE practitioner_given (
    practitioner_id BIGINT     NOT NULL REFERENCES practitioner(id),
    position      INT          NOT NULL,
    given         VARCHAR(255) NOT NULL,
    PRIMARY KEY (practitioner_id, position)
);

CREATE TABLE practitioner_identifier (
    practitioner_id BIGINT     NOT NULL REFERENCES practitioner(id),
    position      INT          NOT NULL,
    ident_system  VARCHAR(512),
    ident_value   VARCHAR(255),
    PRIMARY KEY (practitioner_id, position)
);

CREATE TABLE encounter (
    id              BIGINT     PRIMARY KEY,
    version         INT        NOT NULL,
    last_updated    TIMESTAMP WITH TIME ZONE NOT NULL,
    status          VARCHAR(32) NOT NULL,
    class_system    VARCHAR(512),
    class_code      VARCHAR(64),
    subject_id      BIGINT     NOT NULL REFERENCES patient(id),
    practitioner_id BIGINT     REFERENCES practitioner(id),
    period_start    TIMESTAMP WITH TIME ZONE,
    period_end      TIMESTAMP WITH TIME ZONE
);

CREATE TABLE observation (
    id              BIGINT     PRIMARY KEY,
    version         INT        NOT NULL,
    last_updated    TIMESTAMP WITH TIME ZONE NOT NULL,
    status          VARCHAR(32) NOT NULL,
    category_system VARCHAR(512),
    category_code   VARCHAR(64),
    code_system     VARCHAR(512),
    code_code       VARCHAR(64) NOT NULL,
    code_display    VARCHAR(255),
    subject_id      BIGINT     NOT NULL REFERENCES patient(id),
    encounter_id    BIGINT     REFERENCES encounter(id),
    effective       TIMESTAMP WITH TIME ZONE,
    value_value     DECIMAL(19, 6),
    value_unit      VARCHAR(64),
    value_system    VARCHAR(512),
    value_code      VARCHAR(64)
);

CREATE TABLE appointment (
    id              BIGINT     PRIMARY KEY,
    version         INT        NOT NULL,
    last_updated    TIMESTAMP WITH TIME ZONE NOT NULL,
    status          VARCHAR(32) NOT NULL,
    description     VARCHAR(1024),
    start_time      TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE appointment_participant (
    appointment_id  BIGINT     NOT NULL REFERENCES appointment(id),
    position        INT        NOT NULL,
    actor_type      VARCHAR(32) NOT NULL,
    actor_id        BIGINT     NOT NULL,
    status          VARCHAR(32),
    PRIMARY KEY (appointment_id, position)
);
