CREATE TABLE IF NOT EXISTS aperturas (
    id UUID PRIMARY KEY,
    supervisor VARCHAR(80) NOT NULL,
    datos TEXT NOT NULL,
    actualizada_en TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS aperturas_supervisor ON aperturas(supervisor);

CREATE TABLE IF NOT EXISTS cierres_historicos (
    id UUID PRIMARY KEY REFERENCES aperturas(id),
    cerrado_en BIGINT NOT NULL,
    tiempo_fondo INT NOT NULL,
    profundidad DOUBLE PRECISION NOT NULL,
    descenso DOUBLE PRECISION NOT NULL,
    ascenso DOUBLE PRECISION NOT NULL,
    origen VARCHAR(12) NOT NULL
);

CREATE TABLE IF NOT EXISTS expedientes (
    id UUID PRIMARY KEY REFERENCES aperturas(id),
    version BIGINT NOT NULL,
    operacion UUID NOT NULL,
    contenido TEXT NOT NULL,
    decision TEXT
);

CREATE TABLE IF NOT EXISTS fotos_faena (
    faena UUID NOT NULL REFERENCES expedientes(id),
    equipo VARCHAR(10) NOT NULL,
    imagen BLOB NOT NULL,
    PRIMARY KEY (faena, equipo)
);
