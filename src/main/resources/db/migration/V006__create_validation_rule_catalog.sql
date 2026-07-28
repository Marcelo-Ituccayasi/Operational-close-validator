CREATE TABLE ocv.validation_rule (
    rule_code VARCHAR(10) NOT NULL,
    rule_version INTEGER NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    scope VARCHAR(10) NOT NULL,
    severity VARCHAR(10) NOT NULL,
    failure_effect VARCHAR(40) NOT NULL,
    is_current BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_validation_rule
        PRIMARY KEY (
            rule_code,
            rule_version
        ),

    CONSTRAINT ck_validation_rule_rule_code
        CHECK (
            btrim(rule_code) <> ''
        ),

    CONSTRAINT ck_validation_rule_rule_version
        CHECK (
            rule_version >= 1
        ),

    CONSTRAINT ck_validation_rule_name
        CHECK (
            btrim(name) <> ''
        ),

    CONSTRAINT ck_validation_rule_description
        CHECK (
            btrim(description) <> ''
        ),

    CONSTRAINT ck_validation_rule_scope
        CHECK (
            scope IN (
                'EVENT',
                'CLOSE'
            )
        ),

    CONSTRAINT ck_validation_rule_severity
        CHECK (
            severity IN (
                'CRITICAL',
                'HIGH'
            )
        ),

    CONSTRAINT ck_validation_rule_failure_effect
        CHECK (
            btrim(failure_effect) <> ''
        )
);

CREATE UNIQUE INDEX uq_validation_rule_current
    ON ocv.validation_rule (
        rule_code
    )
    WHERE is_current = TRUE;

CREATE INDEX idx_validation_rule_scope_current
    ON ocv.validation_rule (
        scope,
        is_current
    );

INSERT INTO ocv.validation_rule (
    rule_code,
    rule_version,
    name,
    description,
    scope,
    severity,
    failure_effect,
    is_current,
    created_at
)
VALUES
    (
        'VR-001',
        1,
        'Registro trazable de movimientos autorizados',
        'Todo egreso o descuento autorizado por gerencia debe estar representado por un Evento Operativo registrado y trazable antes de validar el cierre.',
        'EVENT',
        'CRITICAL',
        'BLOCKS_CLOSE',
        TRUE,
        CURRENT_TIMESTAMP
    ),
    (
        'VR-002',
        1,
        'Coincidencia de monto de ingreso',
        'Todo ingreso registrado debe coincidir con el monto de su comprobante físico o digital asociado.',
        'EVENT',
        'CRITICAL',
        'BLOCKS_EVENT_AND_CLOSE',
        TRUE,
        CURRENT_TIMESTAMP
    ),
    (
        'VR-003',
        1,
        'Evidencia legible para egresos menores',
        'Todo egreso menor que requiera soporte debe tener un comprobante presente y legible antes de considerarse válido.',
        'EVENT',
        'HIGH',
        'BLOCKS_EVENT_AND_CLOSE',
        TRUE,
        CURRENT_TIMESTAMP
    ),
    (
        'VR-006',
        1,
        'Autorización formal de descuento o anulación',
        'Todo descuento o anulación debe tener una autorización formal, registrada y vinculada antes de considerarse válido.',
        'EVENT',
        'CRITICAL',
        'BLOCKS_EVENT_AND_CLOSE',
        TRUE,
        CURRENT_TIMESTAMP
    ),
    (
        'VR-008',
        1,
        'Control final antes del envío',
        'Un cierre solo puede enviarse a contabilidad cuando todos sus eventos están Validados, no existen Alertas bloqueantes activas, todos los resultados aplicables están vigentes y satisfechos, y la consolidación está completa.',
        'CLOSE',
        'CRITICAL',
        'REJECTS_SUBMISSION_AND_BLOCKS_CLOSE',
        TRUE,
        CURRENT_TIMESTAMP
    );