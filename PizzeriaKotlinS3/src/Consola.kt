import java.time.LocalDate
import java.time.format.DateTimeParseException

class FinDeEntradaException : RuntimeException()

fun leerEntrada(mensaje: String): String {
    print(mensaje)
    return readlnOrNull()?.trim() ?: throw FinDeEntradaException()
}

fun leerTextoObligatorio(mensaje: String): String {
    while (true) {
        val texto = leerEntrada(mensaje)
        if (texto.isNotBlank()) return texto
        println("Este dato es obligatorio. Intenta nuevamente.")
    }
}

fun leerEntero(mensaje: String, minimo: Int, maximo: Int = Int.MAX_VALUE): Int {
    while (true) {
        try {
            val numero = leerEntrada(mensaje).toInt()
            require(numero in minimo..maximo) { "Ingresa un número entre $minimo y $maximo." }
            return numero
        } catch (error: NumberFormatException) {
            println("Entrada inválida. Escribe un número entero dentro del rango permitido.")
        } catch (error: IllegalArgumentException) {
            println(error.message)
        }
    }
}

fun confirmar(mensaje: String): Boolean {
    while (true) {
        when (leerEntrada("$mensaje (s/n): ").lowercase()) {
            "s", "si", "sí" -> return true
            "n", "no" -> return false
            else -> println("Responde s para sí o n para no.")
        }
    }
}

fun mostrarFaltantes(faltantes: List<Faltante>) {
    println("\nIngredientes insuficientes:")
    for (faltante in faltantes) {
        println("- ${faltante.ingrediente}: ${faltante.disponible} disponible(s), " +
            "${faltante.necesario} necesaria(s); reponer al menos ${faltante.cantidadAReponer} porción/es.")
    }
    println("Para reabastecer: 1) Vuelve al menú. 2) Elige 5, Reabastecer ingrediente.")
    println("3) Selecciona cada ingrediente faltante y agrega las porciones indicadas. 4) Crea nuevamente el pedido.")
}

fun leerDatoValidado(mensaje: String, validar: (String) -> String): String {
    while (true) {
        try {
            return validar(leerEntrada(mensaje))
        } catch (error: IllegalArgumentException) {
            println(error.message)
        }
    }
}

fun leerFecha(mensaje: String): LocalDate {
    while (true) {
        val entrada = leerEntrada(mensaje)
        if (entrada.isEmpty()) return fechaActual()
        try {
            return interpretarFecha(entrada)
        } catch (error: DateTimeParseException) {
            println("Fecha inválida. Usa una fecha existente con formato DD/MM/AAAA.")
        } catch (error: IllegalArgumentException) {
            println("Fecha inválida. Usa una fecha existente con formato DD/MM/AAAA.")
        }
    }
}

fun mostrarClienteDesestructurado(cliente: Cliente) {
    val (nombre, correo, telefono) = cliente
    println("Cliente desestructurado: $nombre | Correo: $correo | Teléfono: $telefono")
}

fun mostrarPedidoDesestructurado(pedido: Pedido) {
    val (cliente, productos, total) = pedido
    println("Pedido desestructurado: ${cliente.nombre} | ${productos.size} productos | ${formatearMonto(total)}")
}

fun notificarCliente(cliente: Cliente?, mensaje: String) {
    cliente?.let { println("Aviso para ${it.nombre}: $mensaje") }
}

fun siguienteAccion(pedido: Pedido): String = when (pedido.estado) {
    EstadoPedido.BORRADOR -> "Confirmar antes de preparar"
    EstadoPedido.EN_PREPARACION -> "Marcar como listo"
    EstadoPedido.LISTO -> "Asignar repartidor y salir a reparto"
    EstadoPedido.EN_CAMINO -> "Confirmar entrega al cliente"
    EstadoPedido.ENTREGADO -> "Sin acciones pendientes"
}

fun mostrarResumen(pedido: Pedido) {
    val totales = pedido.calcularTotal()
    println("\n========================================")
    println(if (pedido.id == 0) "RESUMEN DEL BORRADOR SIN CONFIRMAR" else "RESUMEN DEL PEDIDO #${pedido.id}")
    println("========================================")
    println(pedido.cliente.describir())
    println("Dirección: ${pedido.cliente.direccion}")
    println("Zona de entrega: ${pedido.cliente.zona.descripcion}")
    println("Fecha del pedido: ${formatearFecha(pedido.fecha)}")
    val activa = pedido.periodo?.let { pedido.fecha in it } ?: false
    println("Fecha en periodo promocional: ${if (activa) "Sí" else "No"}")
    println("\nProductos (precios sin impuesto):")
    for ((producto, cantidad) in pedido.productos.groupingBy { it }.eachCount()) {
        println("- ${producto.nombre} | ${formatearMonto(producto.precio)} c/u | Cantidad: $cantidad")
    }
    println("\nSubtotal: ${formatearMonto(totales.subtotal)}")
    println("Descuento por volumen (10% desde 4 pizzas): ${formatearMonto(totales.descuentoVolumen)}")
    println("Descuento promocional por fecha y anotación: ${formatearMonto(totales.descuentoPromocional)}")
    println("Descuento total: ${formatearMonto(totales.descuento)}")
    println("Impuestos (${TASA_IMPUESTO.movePointRight(2).stripTrailingZeros().toPlainString()}% sobre el subtotal descontado): ${formatearMonto(totales.impuestos)}")
    println("TOTAL FINAL: ${formatearMonto(totales.total)}")
    println("\nEstado: ${pedido.estado.descripcion}")
    val reparto = pedido.repartidor?.nombre ?: when (pedido.estado) {
        EstadoPedido.BORRADOR -> "Sin asignar; el pedido no está confirmado."
        EstadoPedido.EN_PREPARACION -> "Sin asignar; se asigna al despachar el pedido, después de marcarlo como Listo."
        else -> "Sin asignar; usa la opción 3 para buscar un repartidor disponible y salir a reparto."
    }
    println("Repartidor: $reparto")
    if (pedido.estado != EstadoPedido.ENTREGADO && pedido.estado != EstadoPedido.BORRADOR) {
        println("Siguiente paso: opción 3, pedido #${pedido.id} → ${siguienteAccion(pedido)}.")
    }
    println("Notas: ${pedido.notas}")
    println("========================================")
}

fun mostrarReporte(pedidosDelDia: Array<Pedido>) {
    println("\n=== Reporte de pedidos del día ===")
    if (pedidosDelDia.isEmpty()) {
        println("Todavía no hay pedidos confirmados.")
        return
    }
    // El reporte recorre explícitamente el arreglo mediante for.
    for (pedido in pedidosDelDia) {
        val pizzas = pedido.productos.groupingBy { it.nombre }.eachCount()
            .entries.joinToString { "${it.value} x ${it.key}" }
        println("#${pedido.id} | ${pedido.cliente.nombre} | $pizzas | " +
            "${formatearMonto(pedido.calcularTotal().total)} | ${pedido.estado.descripcion}")
    }
    val completados = pedidosDelDia.filter { it.estado == EstadoPedido.ENTREGADO }
    val pendientes = pedidosDelDia.filter { it.estado != EstadoPedido.ENTREGADO }
    println("\nCompletados (${completados.size}): ${completados.joinToString { "#${it.id}" }.ifEmpty { "Ninguno" }}")
    println("Pendientes (${pendientes.size}): ${pendientes.joinToString { "#${it.id}" }.ifEmpty { "Ninguno" }}")
    for (pedido in pedidosDelDia) mostrarResumen(pedido)
}
