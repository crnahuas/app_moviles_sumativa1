# Pizzería Kotlin

Aplicación de consola para registrar clientes, crear pedidos de pizzas, controlar ingredientes y coordinar entregas por zona. Permite combinar pedidos antes de confirmarlos, aplicar descuentos y consultar el reporte de ventas.

Los datos se mantienen en memoria durante cada ejecución. Al cerrar la aplicación se pierden los clientes registrados, pedidos y cambios de inventario; la siguiente sesión comienza con la configuración inicial.

## Ejecutar la aplicación

Necesitas un JDK 17 o superior. Gradle Wrapper está incluido y descarga las dependencias en la primera ejecución, por lo que inicialmente se requiere conexión a internet.

En macOS o Linux, abre una terminal en esta carpeta y ejecuta:

```bash
./ejecutar.sh
```

En PowerShell:

```powershell
.\gradlew.bat --console=plain run
```

Si Java no se encuentra, configura `JAVA_HOME` con la ubicación de tu JDK.

En IntelliJ IDEA, abre esta carpeta como proyecto Gradle, selecciona un JDK 17 o superior y sincroniza las dependencias. Ejecuta `src/Main.kt` o la tarea Gradle `run`.

## Menú principal

| Opción | Acción |
| --- | --- |
| 1 | Registrar un cliente con contacto, dirección y zona. |
| 2 | Crear y confirmar un pedido. |
| 3 | Gestionar la preparación, el reparto o la entrega de un pedido. |
| 4 | Consultar el inventario. |
| 5 | Reabastecer un ingrediente o ingresar uno nuevo. |
| 6 | Consultar el reporte del día y los resúmenes de pedidos. |
| 7 | Consultar las zonas y disponibilidad de los repartidores. |
| 8 | Combinar dos pedidos antes de confirmarlos. |
| 0 | Mostrar el reporte final y salir. |

## Registrar un cliente

La opción 1 solicita nombre, teléfono, correo, dirección y zona de entrega. El nombre y la dirección son obligatorios. Si el teléfono o correo son inválidos, se muestra el error y se solicita nuevamente el dato.

El teléfono se guarda con el formato `+XX (XXX) XXX-XXXX`. Puedes ingresar los 12 dígitos completos, con `+` opcional, o el número ya formateado. Por ejemplo, `561234567890` y `+561234567890` se guardan como `+56 (123) 456-7890`. Se requieren 2 dígitos para el país y 10 para el número nacional. Los números incompletos, con dígitos adicionales o con otro formato se rechazan y vuelven a solicitarse.

El correo debe incluir usuario, dominio y extensión, como `ana@ejemplo.cl`. Un cliente con el mismo nombre y correo que otro registrado se considera duplicado, ignorando mayúsculas y minúsculas; se conserva el registro original.

## Crear un pedido

Primero registra al cliente. En la opción 2, selecciona su nombre e ingresa la fecha como `DD/MM/AAAA`, por ejemplo `09/11/2026`. Presiona Enter para usar la fecha actual de Chile. Las fechas imposibles o con otro formato se vuelven a solicitar.

Elige un producto y su cantidad. Puedes repetir la selección para agregar otras variedades y escribir `0` para terminar. Se admiten de 1 a 100 unidades por pedido, sumando todas las selecciones. La consola indica cuántas unidades puedes agregar y termina la selección al alcanzar el límite.

Luego puedes escribir notas de entrega. Las notas no modifican la receta, el precio ni los ingredientes consumidos.

Antes de confirmar se muestran los productos, descuentos, IVA y total. Responde `s` para confirmar o `n` para cancelar. Los ingredientes se descuentan una sola vez, al confirmar. Si falta algún ingrediente, el pedido no se registra y se informa cuánto reponer. Cancelar tampoco modifica el inventario.

### Productos y descuentos

| Pizza | Precio sin IVA | Descuento promocional |
| --- | ---: | ---: |
| Margarita | 8.500,00 pesos | 5% |
| Pepperoni | 9.500,00 pesos | Sin descuento promocional |
| Vegetariana | 9.000,00 pesos | 10% |

Las promociones están configuradas del **01/11/2026 al 30/11/2026**, incluyendo ambas fechas. Se utiliza la fecha ingresada para el pedido.

Al incluir cuatro pizzas o más, se aplica un descuento por volumen del 10% sobre las pizzas. Después, si la fecha corresponde, se aplica el descuento promocional de cada producto sobre su precio ya descontado por volumen. Bebidas e ingredientes no activan el descuento por cantidad de pizzas. Finalmente se calcula IVA del 19% sobre el monto descontado.

Los importes se redondean a dos decimales. El resumen muestra cada descuento por separado.

### Combinar pedidos

La opción 8 permite seleccionar dos grupos de productos para el mismo cliente y fecha. Ambos deben contener productos y, juntos, no pueden superar las 100 unidades. Se reserva al menos un cupo para el segundo grupo.

Se reúnen los productos y las notas de ambos grupos, y se recalculan los descuentos y el total. Los borradores no consumen inventario. Solo se registra y descuenta el pedido resultante cuando confirmas con `s`. Los pedidos ya confirmados no se pueden combinar.

## Preparación y entrega

Cada vez que usas la opción 3 y seleccionas un pedido se realiza un paso:

| Estado actual | Acción | Resultado |
| --- | --- | --- |
| En preparación | Marcar como listo | Queda Listo, sin repartidor. |
| Listo | Asignar un repartidor disponible | Queda En camino. |
| En camino | Registrar la entrega al cliente | Queda Entregado y se libera al repartidor. |
| Entregado | Sin acciones pendientes | El pedido ya está completado. |

Las zonas son Norte, Centro y Sur. Al despachar se prioriza un repartidor disponible de la misma zona; luego se buscan las zonas más cercanas. Cada repartidor atiende un pedido a la vez. Si todos están ocupados, el pedido permanece Listo hasta volver a intentar la asignación.

Al entregar, el repartidor queda disponible en la zona del cliente. La entrega no vuelve a descontar ingredientes.

## Inventario y reportes

La opción 4 muestra las porciones disponibles. Cada pizza consume una porción de masa, salsa y queso; Pepperoni agrega una de pepperoni y Vegetariana una de champiñones.

En la opción 5, selecciona un ingrediente existente para reponer entre 1 y 100 porciones. Para ingresar uno nuevo, elige `0`, escribe su nombre e indica la cantidad. Puedes repetir la operación. Los nombres distinguen mayúsculas y minúsculas; los espacios exteriores se eliminan al reabastecer. Agregar un ingrediente amplía el inventario, sin crear recetas automáticamente.

La opción 6 muestra cliente, productos, cantidades, total y estado de cada pedido. Los Entregados aparecen como completados y el resto como pendientes. Cada resumen incluye contacto, dirección, fecha, descuentos, IVA, repartidor y notas. La opción 7 permite consultar la disponibilidad de los repartidores.

## Ejemplo de ejecución

Para ejecutar una demostración automática con datos de ejemplo:

```bash
./ejecutar.sh --demo
```

También puedes usar la tarea Gradle `demostracion` en IntelliJ o `.\gradlew.bat --quiet demostracion` en PowerShell. La demostración utiliza una sesión independiente y una fecha fija: `09/11/2026`.

[RESUMEN_EJEMPLO.txt](RESUMEN_EJEMPLO.txt) contiene la salida de esa demostración. Para recorrer un ejemplo desde el menú, con validaciones, combinación y entrega:

```bash
./ejecutar.sh < scripts/ejemplo-entrada.txt
```

Dos Margarita, una Pepperoni y una Vegetariana con fecha `09/11/2026` producen:

| Concepto | Pesos |
| --- | ---: |
| Subtotal | 35.500,00 |
| Descuento por volumen | 3.550,00 |
| Descuento promocional | 1.575,00 |
| Base para IVA | 30.375,00 |
| IVA 19% | 5.771,25 |
| Total | **36.146,25** |

Fuera del periodo promocional, las mismas cuatro pizzas totalizan **38.020,50 pesos**.

## Verificar el funcionamiento

```bash
./verificar.sh
```

En PowerShell usa `.\gradlew.bat test`; en IntelliJ, la tarea Gradle `test`. Las pruebas comprueban validaciones, cálculos, combinación de pedidos, inventario y entregas. El reporte se genera en `build/reports/tests/test/index.html`.
