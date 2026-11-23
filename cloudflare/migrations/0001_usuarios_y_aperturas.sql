CREATE TABLE usuarios (
    nombre TEXT PRIMARY KEY NOT NULL,
    rol TEXT NOT NULL CHECK (rol IN ('JEFE', 'SUPERVISOR')),
    sal TEXT NOT NULL,
    verificador TEXT NOT NULL,
    activo INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0, 1))
);

CREATE TABLE aperturas (
    id TEXT PRIMARY KEY NOT NULL,
    supervisor TEXT NOT NULL,
    datos TEXT NOT NULL CHECK (json_valid(datos)),
    actualizada_en TEXT NOT NULL
);

CREATE INDEX aperturas_supervisor ON aperturas(supervisor);
