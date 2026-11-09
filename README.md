# Pizzería Kotlin

Aplicación de consola para registrar clientes, crear pedidos de pizzas, controlar ingredientes y coordinar entregas por zona. Permite combinar pedidos antes de confirmarlos, aplicar descuentos y consultar pedidos completados y pendientes.

El proyecto se encuentra en [PizzeriaKotlinS3](PizzeriaKotlinS3/). Los datos se mantienen en memoria durante cada ejecución y se reinician al cerrar la aplicación.

## Ejecutar la aplicación

Necesitas un **JDK 17 o superior**. Gradle Wrapper está incluido; la primera ejecución requiere internet para descargar las dependencias.

Desde la raíz de este repositorio, en macOS o Linux:

```bash
./PizzeriaKotlinS3/ejecutar.sh
```

En PowerShell:

```powershell
.\PizzeriaKotlinS3\gradlew.bat -p PizzeriaKotlinS3 --console=plain run
```

En IntelliJ IDEA, abre la carpeta `PizzeriaKotlinS3` como proyecto Gradle, selecciona un JDK 17 o superior y sincroniza las dependencias. Ejecuta `src/Main.kt` o la tarea Gradle `run`.

## Operaciones disponibles

| Opción | Acción |
| --- | --- |
| 1 | Registrar un cliente. |
| 2 | Crear y confirmar un pedido. |
| 3 | Gestionar preparación, reparto y entrega. |
| 4 | Consultar el inventario. |
| 5 | Reabastecer o ingresar ingredientes nuevos. |
| 6 | Consultar el reporte del día. |
| 7 | Consultar los repartidores disponibles. |
| 8 | Combinar dos pedidos antes de confirmarlos. |
| 0 | Mostrar el reporte final y salir. |

Los teléfonos se guardan como `+XX (XXX) XXX-XXXX` y las fechas se ingresan como `DD/MM/AAAA`. Cada pedido admite hasta 100 unidades en total. Los ingredientes se descuentan al confirmar y el repartidor queda disponible al registrar la entrega.

Los pedidos de cuatro pizzas o más reciben un descuento por volumen del 10%. Margarita y Vegetariana tienen promociones configuradas del 01/11/2026 al 30/11/2026. Los descuentos se aplican antes del IVA del 19%.

## Ejemplo y pruebas

Para ejecutar una demostración automática desde la raíz del repositorio:

```bash
./PizzeriaKotlinS3/ejecutar.sh --demo
```

Para verificar el funcionamiento:

```bash
./PizzeriaKotlinS3/verificar.sh
```

En PowerShell puedes ejecutar las tareas `demostracion` y `test` con `gradlew.bat -p PizzeriaKotlinS3`.

- [Manual completo de la aplicación](PizzeriaKotlinS3/README.md).
- [Resumen de ejemplo](PizzeriaKotlinS3/RESUMEN_EJEMPLO.txt).
- [Código fuente](PizzeriaKotlinS3/src/).
- [Pruebas](PizzeriaKotlinS3/tests/).
