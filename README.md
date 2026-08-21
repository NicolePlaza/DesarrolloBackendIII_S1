# Migración de Procesos Batch – Banco XYZ

Proyecto de la Experiencia 1 / Semana 1 de Desarrollo Backend III (PBY2203). Implementa con **Spring Batch** la migración de tres procesos batch legacy del "Banco XYZ": reporte de transacciones diarias, cálculo de intereses mensuales y generación de estados de cuenta anuales.

## Objetivo

Modernizar procesos batch legacy leyendo datos desde archivos CSV con errores típicos de sistemas antiguos (montos negativos o en cero, saldos vacíos, edades no válidas, registros duplicados), validarlos y transformarlos con `ItemProcessor`, y persistirlos en una base de datos relacional (MySQL).

Los datos de origen provienen de [bank_legacy_data](https://github.com/KariVillagran/bank_legacy_data).

## Tecnologías

- Java 17
- Spring Boot 4.1.0 + Spring Batch
- Spring Data JPA + Hibernate
- MySQL 8 (vía Docker)
- Maven

## Estructura del proyecto

src/main/java/com/banco/batch/
├── model/ -> Entidades JPA (Transaccion, CuentaInteres, MovimientoAnual, EstadoCuentaAnual)
├── processor/ -> ItemProcessor de cada job (validación y transformación)
├── reader/ -> Lector personalizado para el informe agregado de cuentas anuales
├── config/ -> Configuración de cada Job (reader, processor, writer, steps)
└── BatchApplication.java

src/main/resources/
├── application.properties
└── data/ -> CSVs de origen (transacciones, intereses, cuentas_anuales)


## Jobs implementados

### 1. `transaccionesJob` – Reporte de Transacciones Diarias
Lee `transacciones.csv`, descarta filas con monto nulo o en cero, normaliza el campo `tipo`, y persiste en la tabla `transacciones`.

### 2. `interesesJob` – Cálculo de Intereses Mensuales
Lee `intereses.csv`, descarta cuentas con saldo vacío/cero/negativo, edades no válidas (fuera de 18-100 años) y registros duplicados. Calcula el saldo final aplicando una tasa de interés mensual según el tipo de cuenta (supuesto de negocio: ahorro 2%, préstamo 5%, hipoteca 3%) y persiste en `cuentas_interes`.

### 3. `cuentasAnualesJob` – Estados de Cuenta Anuales
Tiene dos steps:
- `movimientoStep`: lee `cuentas_anuales.csv`, descarta movimientos con monto en cero o sin descripción/tipo de transacción, tolera fechas en formato `yyyy-MM-dd` o `yyyy/MM/dd`, y persiste cada movimiento en `movimientos_anuales`.
- `informeAnualStep`: agrupa los movimientos por cuenta y genera el informe compilado (total ingresos, total egresos, saldo neto, cantidad de movimientos) en la tabla `estados_cuenta_anual`.

## Manejo de errores

Todos los steps usan `.faultTolerant().skipLimit(50).skip(Exception.class)`, de forma que un registro con datos inválidos se descarta sin detener el job completo. Cada `ItemProcessor` además aplica reglas de negocio explícitas (montos, saldos, edades, duplicados) antes de dejar pasar un registro al writer.

## Cómo ejecutar

### Prerrequisitos
- Java 17
- Docker

### 1. Levantar la base de datos

docker run --name banco-mysql -e MYSQL_ROOT_PASSWORD=NuevaClave123 -e MYSQL_DATABASE=banco_xyz -p 3306:3306 -d mysql:8


### 2. Compilar

./mvnw clean install


### 3. Ejecutar cada Job

./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=transaccionesJob"
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=interesesJob"
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=cuentasAnualesJob"


Las tablas se crean automáticamente (`spring.jpa.hibernate.ddl-auto=update`), no requieren pasos manuales adicionales.

## Integrantes

- Nicole Plaza
- Camila González
