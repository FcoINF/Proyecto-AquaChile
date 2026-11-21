CREATE TABLE IF NOT EXISTS aperturas (
    id UUID PRIMARY KEY,
    supervisor VARCHAR(80) NOT NULL,
    datos TEXT NOT NULL,
    actualizada_en TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS aperturas_supervisor ON aperturas(supervisor);

CREATE TABLE IF NOT EXISTS buzos (
    id VARCHAR(80) PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    activo BOOLEAN NOT NULL
);

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

ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS buzo_id VARCHAR(80) REFERENCES buzos(id);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS iniciada_en TIMESTAMP WITH TIME ZONE;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS iniciada_por VARCHAR(80);

CREATE TABLE IF NOT EXISTS evidencias_faena (
    faena UUID NOT NULL REFERENCES expedientes(id) ON DELETE CASCADE,
    equipo VARCHAR(30) NOT NULL,
    tipo VARCHAR(12) NOT NULL,
    formato VARCHAR(16) NOT NULL,
    tomada_en TIMESTAMP WITH TIME ZONE NOT NULL,
    responsable VARCHAR(80) NOT NULL,
    imagen BLOB NOT NULL,
    PRIMARY KEY (faena, equipo, tipo)
);

CREATE TABLE IF NOT EXISTS bitacoras (
    faena UUID PRIMARY KEY REFERENCES expedientes(id),
    duracion INT NOT NULL,
    observaciones VARCHAR(1000) NOT NULL,
    responsable VARCHAR(80) NOT NULL,
    salud_salida TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS alertas_inmersion (
    faena UUID NOT NULL REFERENCES bitacoras(faena),
    tipo VARCHAR(12) NOT NULL,
    revisada_por VARCHAR(80),
    revisada_en TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (faena, tipo)
);
