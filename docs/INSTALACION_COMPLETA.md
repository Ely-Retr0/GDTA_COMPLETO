# GDTA ERP v2.0 — Guía Completa de Instalación y Compilación

---

## PARTE 1 — Instalar MySQL y MySQL Workbench

### 1.1 Descargar MySQL

1. Ve a: https://dev.mysql.com/downloads/installer/
2. Descarga **MySQL Installer for Windows** (el que dice "Windows (x86, 32-bit), MSI Installer ~455MB")
3. Ejecuta el instalador
4. En "Setup Type" elige: **Developer Default** (incluye MySQL Server + Workbench)
5. Da Next en todo, cuando pida contraseña del root: **anótala bien** (ej. `Admin1234!`)
6. Finaliza la instalación

### 1.2 Abrir MySQL Workbench y crear la base de datos

1. Abre **MySQL Workbench** desde el menú de Windows
2. En la pantalla principal verás una conexión llamada **"Local instance MySQL80"**
3. Haz doble clic → pide tu contraseña de root → ingresa la que pusiste al instalar
4. Se abre el editor SQL. Ahora haz click en el ícono de carpeta (Open SQL Script) 
   → busca el archivo `sql/bdgdta_completa.sql` de este proyecto → ábrelo
5. Presiona el rayo ⚡ (Execute All) para ejecutar todo el script
6. En el panel izquierdo "Schemas" deberías ver **bdgdta** aparecer
7. ¡Listo! La base de datos está creada con todas las tablas

---

## PARTE 2 — Configurar el proyecto en Eclipse

### 2.1 Librerías necesarias (JAR files)

Descarga estos JARs y agrégalos al proyecto:

| Librería       | Para qué sirve                  | Dónde descargar                                             |
|----------------|---------------------------------|-------------------------------------------------------------|
| mysql-connector-j-9.x.jar | Conectar Java con MySQL | https://dev.mysql.com/downloads/connector/j/ |
| jbcrypt-0.4.jar           | Hashear contraseñas (bcrypt) | https://github.com/jeremyh/jBCrypt/releases |

### 2.2 Agregar JARs en Eclipse

1. Click derecho en el proyecto → **Build Path** → **Configure Build Path**
2. Tab **Libraries** → **Add External JARs**
3. Selecciona ambos JARs
4. **Apply and Close**

### 2.3 Estructura de paquetes en Eclipse

El proyecto debe tener estos paquetes:
```
src/
  modelo/          → ConexionMySQL.java, Cliente.java, Vehiculo.java, etc.
  vista/panels/    → PanelClientes.java, PanelInventario.java, PanelVenta.java,
                     PanelOrdenes.java, PanelAsistenteIA.java, PanelUsuarios.java,
                     SistemaTaller.java, Dashboard.java
  package_sistemaTR/ → MenuPrincipal.java, MenuRefaccionaria.java, LoginFrame.java
  seguridad/       → Seguridad.java
  util/            → SetupInicial.java
```

### 2.4 Copiar archivos

Copia los archivos de código fuente entregados al lugar correspondiente en Eclipse.
El archivo `config.properties` va en la **raíz del proyecto** (misma carpeta que `src/`).

### 2.5 Ejecutar por primera vez

1. Click derecho en `MenuPrincipal.java` → **Run As** → **Java Application**
2. Como no hay `config.properties` válido, aparecerá el **Setup Inicial**
3. Llena: host = `localhost`, puerto = `3306`, BD = `bdgdta`, usuario = `root`, password = la que pusiste
4. Click "Probar conexión" → debe decir ✅
5. Llena los datos del admin (nombre, usuario, contraseña)
6. Click "Finalizar configuración"
7. Aparece la pantalla de Login → inicia sesión con el admin que creaste

---

## PARTE 3 — Generar el .EXE para Windows

### Prerequisitos

- Java 17 o superior (JDK, no solo JRE) — https://adoptium.net/
- Launch4j — https://launch4j.sourceforge.net/ (gratis)

### Paso 1 — Exportar el JAR desde Eclipse

1. Click derecho en el proyecto → **Export**
2. Java → **Runnable JAR file**
3. Launch configuration: **MenuPrincipal**
4. Export destination: `C:\GDTA\GDTA.jar`
5. Library handling: **Extract required libraries into generated JAR**
6. Finish

### Paso 2 — Crear el .EXE con Launch4j

1. Abre Launch4j
2. Llena los campos:
   - **Output file**: `C:\GDTA\GDTA_ERP.exe`
   - **Jar**: `C:\GDTA\GDTA.jar`
   - Tab **JRE** → Min JRE version: `17.0.0`
   - Tab **Header** → Header type: `GUI` (para que no abra consola negra)
   - (Opcional) Tab **Version info** → agregar nombre y versión
3. Click el engrane (Build wrapper) → genera `GDTA_ERP.exe`

### Paso 3 — Carpeta final de distribución

Para que funcione en otra computadora, arma esta carpeta:
```
GDTA_ERP/
  GDTA_ERP.exe          ← el ejecutable
  GDTA.jar              ← el JAR (Launch4j lo necesita junto)
  config.properties     ← configuración de BD (el usuario la llena en Setup Inicial)
```

**¡IMPORTANTE!** La otra computadora también necesita tener MySQL instalado
(o conectarse a un MySQL en red local). La primera vez que ejecuten el .exe
aparecerá el Setup Inicial para configurar la conexión.

---

## PARTE 4 — Instalador para Windows (.msi)

Para crear un instalador profesional con ícono y todo:

### Usando jpackage (viene con Java 17+)

Abre CMD y ejecuta:
```cmd
jpackage ^
  --input C:\GDTA ^
  --name "GDTA ERP" ^
  --main-jar GDTA.jar ^
  --main-class package_sistemaTR.MenuPrincipal ^
  --type msi ^
  --app-version 2.0 ^
  --vendor "Taller GDTA" ^
  --icon C:\GDTA\logo.ico ^
  --dest C:\GDTA\instalador
```

Esto genera un `.msi` que al hacer doble clic instala el programa completo en Windows.

**Nota para Linux** — cambia `--type msi` por `--type deb` (Ubuntu/Debian) o `--type rpm` (Fedora):
```bash
jpackage \
  --input ~/GDTA \
  --name "GDTA ERP" \
  --main-jar GDTA.jar \
  --main-class package_sistemaTR.MenuPrincipal \
  --type deb \
  --app-version 2.0 \
  --dest ~/GDTA/instalador
```

---

## PARTE 5 — Asistente de IA (API Key)

1. Ve a https://console.anthropic.com y crea una cuenta
2. En el dashboard → API Keys → Create Key
3. Copia la key (empieza con `sk-ant-...`)
4. En el sistema, ve al módulo **Asistente IA**
5. Pega tu key en el campo "API Key de Anthropic" → Guardar key
6. ¡Ya puedes consultar diagnósticos!

**Costo aproximado**: con el modelo `claude-sonnet-4` son ~$0.003 USD por consulta.
Anthropic da créditos gratis al registrarte.

---

## PARTE 6 — Credenciales por defecto del sistema

Al hacer el Setup Inicial se crea el admin que tú configures.
Después el admin puede crear usuarios desde el panel Usuarios:

| Rol      | Puede hacer                                          |
|----------|------------------------------------------------------|
| ADMIN    | Todo: usuarios, inventario, ventas, órdenes, reportes|
| MECANICO | Ver/crear/editar órdenes, ver clientes               |
| CAJERO   | Ventas, ver inventario, ver órdenes                  |

---

## Solución de problemas comunes

**"Access denied for user root"** → Verifica la contraseña en config.properties o corre el Setup Inicial nuevamente.

**"Communications link failure"** → MySQL no está corriendo. Ve a Servicios de Windows → MySQL80 → Iniciar.

**El .exe no abre** → Verifica que Java 17+ esté instalado: `java -version` en CMD.

**"Table doesn't exist"** → Vuelve a ejecutar el script SQL en MySQL Workbench.
