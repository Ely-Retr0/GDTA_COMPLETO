-- ============================================================
--  GDTA ERP — Base de datos completa v2.0
--  Ejecutar en MySQL Workbench como usuario root
-- ============================================================

CREATE DATABASE IF NOT EXISTS bdgdta
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE bdgdta;

-- ─────────────────────────────────────────
--  TABLA: usuarios  (login + RBAC)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS usuarios (
    id            INT          NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,          -- bcrypt
    nombre        VARCHAR(100) NOT NULL,
    rol           ENUM('ADMIN','MECANICO','CAJERO') NOT NULL DEFAULT 'MECANICO',
    activo        TINYINT(1)   NOT NULL DEFAULT 1,
    ultimo_acceso DATETIME     DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ─────────────────────────────────────────
--  TABLA: clientes
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS clientes (
    id       INT          NOT NULL AUTO_INCREMENT,
    nombre   VARCHAR(100) NOT NULL,
    telefono VARCHAR(15)  NOT NULL UNIQUE,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ─────────────────────────────────────────
--  TABLA: vehiculos
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS vehiculos (
    id         INT         NOT NULL AUTO_INCREMENT,
    placas     VARCHAR(20) NOT NULL UNIQUE,
    marca      VARCHAR(50) NOT NULL,
    modelo     VARCHAR(50) NOT NULL,
    anio       INT         NOT NULL,
    color      VARCHAR(30) NOT NULL DEFAULT 'N/A',
    id_cliente INT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_veh_cli FOREIGN KEY (id_cliente)
        REFERENCES clientes(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─────────────────────────────────────────
--  TABLA: inventario
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS inventario (
    id        INT            NOT NULL AUTO_INCREMENT,
    nombre    VARCHAR(100)   NOT NULL,
    marca     VARCHAR(50)    NOT NULL,
    precio    DECIMAL(10,2)  NOT NULL,
    cantidad  INT            NOT NULL DEFAULT 0,
    cant_min  INT            NOT NULL DEFAULT 0,
    categoria VARCHAR(60)    NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ─────────────────────────────────────────
--  TABLA: ordenes
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ordenes (
    id            INT          NOT NULL AUTO_INCREMENT,
    diagnostico   VARCHAR(255) NOT NULL,
    estado        ENUM('EN_PROCESO','ESPERANDO_REFACCION','LISTO','ENTREGADO') NOT NULL DEFAULT 'EN_PROCESO',
    observaciones TEXT         DEFAULT NULL,
    fecha_ingreso DATE         NOT NULL,
    fecha_entrega DATE         DEFAULT NULL,
    id_cliente    INT          NOT NULL,
    id_vehiculo   INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ord_cli FOREIGN KEY (id_cliente)
        REFERENCES clientes(id) ON DELETE CASCADE,
    CONSTRAINT fk_ord_veh FOREIGN KEY (id_vehiculo)
        REFERENCES vehiculos(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─────────────────────────────────────────
--  TABLA: detalles_orden
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS detalles_orden (
    id               INT           NOT NULL AUTO_INCREMENT,
    descripcion      VARCHAR(255)  NOT NULL,
    cantidad         INT           NOT NULL DEFAULT 1,
    precio_unitario  DECIMAL(10,2) NOT NULL,
    subtotal         DECIMAL(10,2) NOT NULL,
    id_orden         INT           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_det_ord FOREIGN KEY (id_orden)
        REFERENCES ordenes(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─────────────────────────────────────────
--  TABLA: pagos
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS pagos (
    id       INT           NOT NULL AUTO_INCREMENT,
    fecha    DATE          NOT NULL,
    monto    DECIMAL(10,2) NOT NULL,
    metodo   VARCHAR(30)   NOT NULL,
    id_orden INT           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pag_ord FOREIGN KEY (id_orden)
        REFERENCES ordenes(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─────────────────────────────────────────
--  TABLA: ventas  (refaccionaria)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ventas (
    id       INT           NOT NULL AUTO_INCREMENT,
    fecha    DATE          NOT NULL,
    nom_cli  VARCHAR(100)  NOT NULL,
    met_pag  VARCHAR(30)   DEFAULT NULL,
    cantidad INT           NOT NULL,
    total    DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ─────────────────────────────────────────
--  TABLA: auditoria  (log de seguridad)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS auditoria (
    id         INT          NOT NULL AUTO_INCREMENT,
    fecha_hora DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario    VARCHAR(50)  NOT NULL,
    accion     VARCHAR(100) NOT NULL,
    detalle    TEXT         DEFAULT NULL,
    ip         VARCHAR(45)  DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ─────────────────────────────────────────
--  DATOS INICIALES
--  Usuario admin: admin / Admin1234!
--  (hash bcrypt generado con cost=12)
-- ─────────────────────────────────────────
INSERT INTO usuarios (username, password_hash, nombre, rol) VALUES
('admin',    '$2a$12$PLACEHOLDER_HASH_ADMIN',    'Administrador',  'ADMIN'),
('mecanico', '$2a$12$PLACEHOLDER_HASH_MECA',     'Mecánico Demo',  'MECANICO'),
('cajero',   '$2a$12$PLACEHOLDER_HASH_CAJERO',   'Cajero Demo',    'CAJERO');

-- NOTA: Los hashes reales se generan al primer arranque del sistema
-- via SetupInicial.java. No uses estos valores directamente.

-- Inventario demo
INSERT INTO inventario (nombre, marca, precio, cantidad, cant_min, categoria) VALUES
('Aceite 5W30',       'Castrol', 250.00, 48, 14, 'Aceites y lubricantes'),
('Filtro de Aire',    'Bosch',   180.00, 32, 10, 'Filtros'),
('Bujía',             'NGK',      90.00, 60, 18, 'Sistema de encendido'),
('Pastillas de Freno','Brembo',  320.00,  5,  5, 'Sistema de frenos'),
('Filtro de Aceite',  'Mann',    120.00, 40, 12, 'Filtros'),
('Correa Distribución','Gates',  450.00,  3,  4, 'Bandas y correas'),
('Amortiguador',      'Monroe',  890.00,  8,  3, 'Suspensión y dirección'),
('Líquido de Frenos', 'Dot4',     85.00, 25,  8, 'Sistema de frenos');
