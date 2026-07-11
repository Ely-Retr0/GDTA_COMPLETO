# GDTA ERP — Sistema de Gestión de Taller Automotriz y Refaccionaria

<div align="center">

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white)
![Swing](https://img.shields.io/badge/Java_Swing-007396?style=for-the-badge&logo=java&logoColor=white)
![BCrypt](https://img.shields.io/badge/BCrypt-Security-green?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-2.0-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-Academic-orange?style=for-the-badge)

**Sistema ERP de escritorio para la gestión integral de talleres mecánicos y refaccionarías de autopartes.**

*Proyecto académico final — Tecnólogo Profesional en Sistemas Informáticos (TPSI)*
*Escuela Politécnica de Guadalajara — Universidad de Guadalajara*

</div>

---

## Tabla de Contenidos

- [Descripción](#descripción)
- [Características](#características)
- [Tecnologías](#tecnologías)
- [Arquitectura](#arquitectura)
- [Requisitos](#requisitos)
- [Instalación](#instalación)
- [Configuración](#configuración)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Base de Datos](#base-de-datos)
- [Seguridad](#seguridad)
- [Asistente de IA](#asistente-de-ia)
- [Capturas de Pantalla](#capturas-de-pantalla)
- [Autores](#autores)

---

## Descripción

GDTA ERP es una aplicación de escritorio desarrollada en **Java con interfaz Swing** que centraliza la operación completa de un negocio que combina taller mecánico y refaccionaría de autopartes. Nació como respuesta a la problemática real de pequeños talleres en México que gestionan sus operaciones manualmente en papel, lo que genera errores de cálculo, pérdida de historial de clientes y falta de control sobre el inventario.

El sistema permite gestionar clientes, vehículos, órdenes de trabajo, inventario de refacciones, ventas al mostrador, pagos y usuarios — todo desde una sola plataforma con interfaz oscura moderna, control de acceso por roles y registro de auditoría completo.

---

## Características

### Módulos del sistema

| Módulo | Descripción |
|--------|-------------|
| 🔐 **Autenticación** | Login con BCrypt, bloqueo a 5 intentos fallidos, roles RBAC |
| 👥 **Clientes** | CRUD completo, vehículos asociados, búsqueda en tiempo real |
| 📦 **Inventario** | Gestión de productos con alertas de stock por código de colores |
| 🛒 **Ventas** | Carrito de compras, cobro, descuento automático de stock, historial |
| 🔧 **Órdenes de Trabajo** | Creación, seguimiento por estados con colores, edición y eliminación |
| 💳 **Pagos** | Registro de pagos contra órdenes con verificación de existencia |
| 👤 **Usuarios** | CRUD de usuarios, cambio de contraseña, activar/desactivar cuentas |
| 🤖 **Asistente IA** | Chat de diagnóstico mecánico online (Claude API) y modo offline |
| 📋 **Auditoría** | Log completo de todas las acciones con usuario, fecha, hora e IP |
| ⚙️ **Setup Inicial** | Configurador de primer arranque para conexión a MySQL sin tocar código |

### Funcionalidades destacadas

- **Dark mode** con paleta de colores consistente y esquinas redondeadas en Java Swing puro
- **RBAC** (Role-Based Access Control) con tres roles: Administrador, Mecánico y Cajero
- **Modo offline** en el asistente IA — responde con diagnósticos predefinidos si no hay internet o el firewall bloquea la API
- **Detección de red** en tiempo real con indicador de estado de conectividad
- Código de colores en inventario: 🟢 stock normal · 🟡 stock bajo · 🔴 sin stock
- Código de colores en órdenes: blanco (en proceso) · 🟡 esperando refacción · 🟢 listo · gris (entregado)
- **PreparedStatement** en todas las queries — prevención de inyección SQL
- Credenciales de BD en `config.properties` externo — nunca hardcodeadas en el código

---

## Tecnologías

| Tecnología | Versión | Uso |
|------------|---------|-----|
| Java | 17+ (compilado con 26) | Lenguaje principal |
| Java Swing | JDK built-in | Interfaz gráfica de escritorio |
| MySQL / MariaDB | 8.0 / 10.11 | Base de datos relacional |
| MySQL Connector/J | 9.x | Driver JDBC |
| jBCrypt | 0.4 | Hash seguro de contraseñas |
| Claude API (Anthropic) | claude-sonnet-4 | Asistente de diagnóstico IA |
| IntelliJ IDEA | Community | IDE de desarrollo |

---

## Arquitectura

El sistema implementa el patrón **DAO (Data Access Object)** con separación en tres capas:

```
┌─────────────────────────────────────────────────────┐
│                  CAPA DE PRESENTACIÓN                │
│         vista.panels  /  package_sistemaTR           │
│   JPanel + CardLayout + Swing Components             │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│                  CAPA DE DOMINIO                     │
│                    modelo/                           │
│  Cliente · Vehiculo · OrdenServicio · Pago           │
│  DetalleOrden · SistemaTaller                        │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│               CAPA DE ACCESO A DATOS                 │
│          ConexionMySQL + PreparedStatement            │
│              MySQL / MariaDB                         │
└─────────────────────────────────────────────────────┘

Capas transversales:
  seguridad/  →  Seguridad.java (RBAC, BCrypt, Auditoría)
  util/       →  SetupInicial.java · NetworkChecker.java
```

---

## Requisitos

### Para ejecutar

- **Java Runtime Environment** 17 o superior
  - Descarga: [https://adoptium.net](https://adoptium.net)
- **MySQL 8.0** o **MariaDB 10.11+**
  - En Debian/Ubuntu: `sudo apt install mariadb-server`
- Los archivos `mysql-connector-j-9.x.jar` y `jbcrypt-0.4.jar` en el classpath

### Para desarrollar

- **Java JDK** 17 o superior
- **IntelliJ IDEA** Community (recomendado) o Eclipse
- **DBeaver** o **MySQL Workbench** para gestión de BD

### Hardware mínimo

| Componente | Mínimo | Recomendado |
|------------|--------|-------------|
| CPU | 1 GHz | 2 GHz+ |
| RAM | 2 GB | 4 GB |
| Disco | 500 MB | 1 GB |
| Pantalla | 1280×768 | 1920×1080 |

---

## Instalación

### 1. Clonar el repositorio

```bash
git clone https://github.com/TU_USUARIO/GDTA-ERP.git
cd GDTA-ERP
```

### 2. Instalar la base de datos

Abre MySQL Workbench o DBeaver, conecta con tu usuario root y ejecuta el script completo:

```bash
mysql -u root -p < sql/bdgdta_completa.sql
```

O abre el archivo `sql/bdgdta_completa.sql` en tu cliente gráfico y ejecuta todo.

### 3. Agregar librerías en IntelliJ

1. **File → Project Structure → Libraries**
2. Click **+** → **Java**
3. Agrega `mysql-connector-j-9.x.jar`
4. Agrega `jbcrypt-0.4.jar`
5. **Apply → OK**

### 4. Ejecutar

Ejecuta la clase principal:

```
package_sistemaTR.MenuPrincipal
```

Al primer arranque, el sistema detecta que no hay `config.properties` y abre automáticamente el **Setup Inicial** para configurar la conexión y crear el primer usuario administrador.

---

## Configuración

El archivo `config.properties` se genera automáticamente en el Setup Inicial. Si necesitas editarlo manualmente:

```properties
# GDTA ERP — Configuración de base de datos
db.url=jdbc:mysql://localhost:3306/bdgdta?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Mexico_City
db.user=root
db.password=TU_PASSWORD
```

> ⚠️ **Importante:** Nunca subas `config.properties` a GitHub. Ya está incluido en el `.gitignore`.

### Roles de usuario

| Rol | Módulos accesibles |
|-----|-------------------|
| **ADMIN** | Todos los módulos sin restricción |
| **MECANICO** | Dashboard, Clientes, Órdenes (edición), Asistente IA |
| **CAJERO** | Dashboard, Clientes, Inventario (solo lectura), Ventas, Órdenes (solo lectura), Asistente IA |

---

## Estructura del Proyecto

```
GDTA_COMPLETO/
│
├── src/
│   ├── modelo/                     # Clases de dominio (entidades)
│   │   ├── Cliente.java
│   │   ├── Vehiculo.java
│   │   ├── OrdenServicio.java
│   │   ├── Pago.java
│   │   ├── DetalleOrden.java
│   │   ├── SistemaTaller.java      # Estado global en sesión
│   │   └── ConexionMySQL.java      # Acceso a datos (lee config.properties)
│   │
│   ├── vista/
│   │   └── panels/                 # Paneles de la interfaz gráfica
│   │       ├── Dashboard.java
│   │       ├── PanelClientes.java
│   │       ├── PanelInventario.java
│   │       ├── PanelVenta.java
│   │       ├── PanelOrdenes.java
│   │       ├── PanelUsuarios.java
│   │       ├── PanelAsistenteIA.java
│   │       ├── VentanaOrdenTrabajo.java
│   │       └── VentanaPagos.java
│   │
│   ├── package_sistemaTR/          # Punto de entrada y ventana principal
│   │   ├── MenuPrincipal.java      # Main — detecta primer arranque
│   │   ├── MenuRefaccionaria.java  # Ventana principal + navbar RBAC
│   │   └── LoginFrame.java         # Pantalla de login
│   │
│   ├── seguridad/
│   │   └── Seguridad.java          # RBAC, BCrypt, log de auditoría
│   │
│   └── util/
│       ├── SetupInicial.java       # Wizard de configuración inicial
│       └── NetworkChecker.java     # Detector de conectividad/firewall
│
├── sql/
│   └── bdgdta_completa.sql         # Script completo de la base de datos
│
├── docs/
│   ├── Manual_Tecnico_GDTA.docx
│   └── Manual_Usuario_GDTA.docx
│
├── config.properties               # Credenciales BD (NO subir a GitHub)
├── api_key.txt                     # API key de Anthropic (NO subir a GitHub)
└── README.md
```

---

## Base de Datos

El sistema utiliza **9 tablas** con relaciones mediante claves foráneas y `ENGINE=InnoDB`:

```
usuarios          → Cuentas de acceso con hash BCrypt y rol
clientes          → Datos de clientes del taller/refaccionaría
vehiculos         → Vehículos asociados a clientes (FK → clientes)
ordenes           → Órdenes de trabajo (FK → clientes, vehiculos)
detalles_orden    → Servicios/refacciones por orden (FK → ordenes)
pagos             → Cobros registrados por orden (FK → ordenes)
ventas            → Ventas al mostrador de la refaccionaría
inventario        → Catálogo de productos con control de stock
auditoria         → Log inmutable de todas las acciones del sistema
```

### Diagrama de relaciones

```
CLIENTES ──(1:N)──► VEHICULOS
CLIENTES ──(1:N)──► ORDENES
VEHICULOS ──(1:N)──► ORDENES
ORDENES ──(1:N)──► DETALLES_ORDEN
ORDENES ──(1:N)──► PAGOS
VENTAS ──(lógica)──► INVENTARIO (descuento de stock al vender)
USUARIOS ──(lógica)──► AUDITORIA (registro de acciones por usuario)
```

---

## Seguridad

El sistema implementa múltiples capas de seguridad:

- **BCrypt** con factor de costo 12 para hash de contraseñas — nunca se almacenan en texto plano
- **RBAC** — cada usuario solo puede acceder a los módulos y funciones de su rol
- **Bloqueo de cuenta** tras 5 intentos fallidos de login consecutivos
- **PreparedStatement** en el 100% de las consultas SQL — previene inyección SQL
- **Log de auditoría** inmutable con usuario, acción, detalle, marca de tiempo e IP
- **config.properties** externo — las credenciales de BD nunca están en el código fuente
- **api_key.txt** separado — la clave de la API de IA no se incluye en el repositorio

---

## Asistente de IA

El módulo de diagnóstico mecánico funciona en dos modos:

### Modo Online (Claude API)
Requiere cuenta en [console.anthropic.com](https://console.anthropic.com) y cargar la API key en el sistema. Proporciona diagnósticos personalizados basados en los síntomas descritos.

### Modo Offline (sin internet)
Funciona automáticamente cuando no hay conexión o el firewall bloquea la API. Incluye diagnósticos predefinidos para los problemas más comunes:

- Problemas de frenos
- Fugas de aceite
- Fallas de arranque
- Vibraciones
- Recalentamiento del motor
- Pérdida de potencia

El indicador de estado muestra en tiempo real si el sistema está **online** 🟢, con **API bloqueada** 🟡 (firewall) o **sin internet** 🔴.

---

## Capturas de Pantalla

> Las capturas se encuentran en la carpeta `docs/capturas/` del repositorio.

| Pantalla | Descripción |
|----------|-------------|
| Login | Autenticación con BCrypt y bloqueo por intentos |
| Dashboard | Panel principal con accesos rápidos y gráfica de actividad |
| Inventario | Tabla de productos con código de colores de stock |
| Ventas | Carrito de compras con búsqueda de productos |
| Órdenes | Lista con estados diferenciados por color |
| Asistente IA | Chat de diagnóstico con indicador de conectividad |
| Usuarios | Gestión de cuentas y log de auditoría |

---

## Autores

| Nombre | Rol en el proyecto |
|--------|-------------------|
| **Díaz Gutiérrez, Elias** | Desarrollo backend, seguridad, BD |
| **Gutiérrez Iñiguez, Ashly Sujey** | Documentación, diseño UI |
| **Nieto Ramírez, Ángel Daniel** | Módulo de órdenes y clientes |
| **Ruiz Zúñiga, Eduardo Ángel** | Módulo de ventas e inventario |
| **Yerenas Loza, Nicolás** | Módulo de pagos y configuración |

**Institución:** Escuela Politécnica de Guadalajara — Universidad de Guadalajara (SEMS)
**Programa:** Tecnólogo Profesional en Sistemas Informáticos (TPSI)
**Materias:** Análisis y Diseño de Software · Sistemas de Gestión de Bases de Datos
**Año:** 2025

---

## .gitignore recomendado

```gitignore
# Credenciales — NUNCA subir
config.properties
api_key.txt

# Compilados
out/
*.class
*.jar

# IntelliJ
.idea/
*.iml

# OS
.DS_Store
Thumbs.db
```

---

<div align="center">

*GDTA ERP v2.0 — Desarrollado con Java Swing + MySQL*
*Escuela Politécnica de Guadalajara · 2025*

</div>
