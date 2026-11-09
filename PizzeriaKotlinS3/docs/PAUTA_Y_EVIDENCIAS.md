# Pauta y evidencias de Semana 3

Esta matriz sigue la pauta sumativa y las instrucciones específicas de la forma B. Describe evidencia revisable del proyecto; no asigna una nota ni reemplaza la evaluación del docente.

| Criterio | Puntos | Implementación | Verificación y demostración |
| --- | ---: | --- | --- |
| Regex y rangos | 20 | `Usuarios.kt`: correo y teléfono. `Consola.kt`: reingreso y calendario. `Promociones.kt`: intervalo inclusivo de fechas. `Main.kt` y `Pedido.kt`: límites de cantidad y total. | Pruebas de contactos, reingreso, fechas imposibles, inicio/fin y días exteriores, cantidades 0/1/100/101. Sección 1 del resumen y archivo de entradas inválidas. |
| Anotaciones y reflection | 20 | `@Promocionable` tiene Target CLASS y Retention RUNTIME. Dos clases anotadas: PizzaPromocional y PizzaDeTemporada. `Calculos.kt` obtiene porcentajes dinámicos. `Inspeccion.kt` lista métodos y propiedades de Cliente, Producto y Pedido. | Prueba de un tercer tipo anotado con 25% para demostrar detección sin nombres fijos; pruebas de productos no anotados e inspección de tres entidades. Secciones 3 y 5 del resumen ejecutado mediante `Main.kt --demo`. |
| Sobrecarga de operadores | 15 | `Pedido.plus` combina productos, notas y recalcula el total. `Producto.equals` da comportamiento a `==` por nombre y categoría. | Combinación 2 + 2 activa volumen, conserva stock y calcula total de 36.146,25; rechaza clientes/fechas incompatibles, exceso, misma instancia y pedidos confirmados. Sección 4, opción 8. |
| Desestructuración | 10 | Cliente: component1 nombre, component2 correo y component3 teléfono. Pedido: component1 cliente, component2 productos y component3 total. | Prueba de orden y valores; las funciones `mostrarClienteDesestructurado` y `mostrarPedidoDesestructurado` usan `val (...)`. Sección 4 y creación normal de pedidos. |
| Igualdad de objetos | 15 | `equals` y `hashCode` explícitos en Cliente y Producto. Conjuntos impiden duplicados. Registro de productos y clientes devuelve resultado; las propuestas utilizan las entidades originales del registro. | Pruebas de igualdad con distinta referencia, hashes coincidentes, colecciones sin duplicados, categorías distintas y conservación de precio, receta y promoción. Sección 2 del resumen. |
| Resumen integrado | 10 | `Main.kt --demo` llama a `Demostracion.kt`; se guarda su salida real en `RESUMEN_EJEMPLO.txt`. | Prueba que ejecuta Main y verifica las funciones requeridas y el cierre con un pedido entregado. Archivo de entradas reproduce el flujo interactivo, incluidos errores y recuperación. |
| Presentación y entrega | 10 | Proyecto separado, responsabilidades por archivo, Gradle Wrapper, JDK 17+, dependencia kotlin-reflect, JUnit, README y ZIP. | `./verificar.sh`, `./ejecutar.sh --demo`, `./ejecutar.sh < scripts/ejemplo-entrada.txt` y `./preparar-entrega.sh`. |

## Decisiones donde el enunciado deja opciones abiertas

- El encargo no fija fechas ni porcentaje de la promoción: se usa noviembre de 2026, con 5% para Margarita y 10% para Vegetariana. La configuración está identificada como ejemplo y puede cambiarse.
- Se conservan el 10% por volumen y el IVA de Semana 2. Los descuentos temporales se calculan después del de volumen y antes del IVA; el resumen desglosa ambos importes.
- Se combina antes de confirmar para conservar coherencia de stock, cobro e identificadores. El resultado sigue el flujo de entrega ya existente.
- El teléfono se formatea como `+XX (XXX) XXX-XXXX`, respetando las instrucciones. Se aceptan 12 dígitos con + opcional o el número ya formateado, sin agregar dígitos a entradas incompletas. Las fechas se ingresan y muestran como `DD/MM/AAAA`, con validación estricta de calendario.
- La expresión «archivo de resumen en la consola» se cubre mostrando el resumen y entregando su salida en un archivo de texto reproducible.
- El caso TechCare de la guía se utiliza para aprender las técnicas. La solución evaluada conserva el caso de la pizzería indicado por la forma B.

## Resultado de la revisión local

La suite contiene 45 pruebas, 18 específicas de Semana 3 y 27 adaptadas de las funciones anteriores. Los reportes se generan en `build/` y quedan fuera de la entrega. Los documentos originales no se modifican ni se incluyen en el ZIP del proyecto. No se realiza el envío a AVA desde la aplicación.

## Ejecución de las funciones académicas

El menú contiene las operaciones 1 a 8 y 0 para salir. La funcionalidad de inspección con reflection del paso 2 se ejecuta desde `Main.kt --demo` y lista métodos y propiedades de Cliente, Producto, Pedido y las dos clases anotadas.

Los pasos 6 y 7 y el criterio del resumen se demuestran con la misma ejecución: `./ejecutar.sh --demo` o la configuración **Resumen Semana 3** de IntelliJ. Se conserva también su salida en `RESUMEN_EJEMPLO.txt`.
