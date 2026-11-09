fun main(args: Array<String> = emptyArray()) {
    if (args.contentEquals(arrayOf("--demo"))) {
        mostrarDemostracion()
        return
    }
    if (args.isNotEmpty()) {
        println("Uso: Main.kt [--demo]")
        return
    }
    val pizzeria = crearPizzeria()
    println("=== Pizzería Kotlin | Semana 3 ===")
    println("El inventario y los pedidos se conservan durante esta ejecución.")
    println("Precios sin impuesto. Impuesto configurado: 19%; descuento de 10% por más de 3 pizzas.")
    println("Reparto por zonas: Norte, Centro y Sur. Se prioriza al repartidor disponible de la misma zona.")
    println("Flujo del pedido: En preparación → Listo → En camino → Entregado. La opción 3 realiza un paso cada vez.")

    mostrarPromocionables(pizzeria)

    try {
        while (true) {
            println("\n1. Registrar cliente\n2. Crear pedido\n3. Gestionar preparación, reparto o entrega")
            println("4. Ver inventario\n5. Reabastecer o registrar ingrediente\n6. Reporte del día\n7. Ver repartidores")
            println("8. Combinar dos pedidos antes de confirmar\n0. Salir")
            try {
                when (leerEntero("Opción: ", 0, 8)) {
                    1 -> registrarCliente(pizzeria)
                    2 -> crearPedido(pizzeria)
                    3 -> avanzarPedido(pizzeria)
                    4 -> mostrarInventario(pizzeria)
                    5 -> reabastecer(pizzeria)
                    6 -> mostrarReporte(pizzeria.consultarPedidos())
                    7 -> {
                        println("\n--- Repartidores por zona ---")
                        println("Al despachar se busca primero en la zona del cliente y luego en las zonas más cercanas.")
                        println("Tras una entrega, el repartidor queda disponible en la zona del cliente.")
                        pizzeria.repartidores.forEach { println(it.describir()) }
                    }
                    8 -> crearPedido(pizzeria, combinar = true)
                    0 -> {
                        mostrarReporte(pizzeria.consultarPedidos())
                        println("Sesión finalizada.")
                        return
                    }
                }
            } catch (error: StockInsuficienteException) {
                println(error.message)
                mostrarFaltantes(error.faltantes)
            } catch (error: IllegalArgumentException) {
                println("No se pudo completar la operación: ${error.message}")
            } catch (error: ArithmeticException) {
                println("La cantidad es demasiado grande. Usa una cantidad menor.")
            }
        }
    } catch (error: FinDeEntradaException) {
        println("\nSe terminó la entrada. Las operaciones sin confirmar no se registraron.")
        mostrarReporte(pizzeria.consultarPedidos())
    }
}

private fun registrarCliente(pizzeria: Pizzeria) {
    println("\n--- Registro de cliente ---")
    val nombre = leerTextoObligatorio("Nombre: ")
    val telefono = leerDatoValidado("Teléfono (12 dígitos o +XX (XXX) XXX-XXXX): ", ::formatearTelefono)
    val correo = leerDatoValidado("Correo electrónico: ", ::validarCorreo)
    val direccion = leerTextoObligatorio("Dirección de entrega: ")
    println("\nSelecciona la zona de entrega:")
    val zonas = Zona.entries
    zonas.forEachIndexed { indice, zona -> println("${indice + 1}. ${zona.descripcion}") }
    val zona = zonas[leerEntero("Zona (1 a ${zonas.size}): ", 1, zonas.size) - 1]
    val cliente = Cliente(nombre, telefono, correo, direccion, zona)
    if (!pizzeria.registrarCliente(cliente)) {
        println("Cliente duplicado: ya existe un registro con ese nombre y correo.")
        return
    }
    mostrarClienteDesestructurado(cliente)
    notificarCliente(cliente, "Registro completado.")
}

private fun seleccionarProductos(pizzeria: Pizzeria, maximo: Int = MAX_PRODUCTOS_POR_PEDIDO): List<Producto> {
    val productos = mutableListOf<Producto>()
    while (productos.size < maximo) {
        println("\n--- Menú de productos ---")
        pizzeria.menu.forEachIndexed { indice, producto -> println("${indice + 1}. ${producto.detallarProducto()}") }
        println("0. Terminar selección")
        val opcion = leerEntero("Producto: ", 0, pizzeria.menu.size)
        if (opcion == 0) break
        val restantes = maximo - productos.size
        val cantidad = leerEntero("Cantidad (puedes agregar hasta $restantes productos más): ", 1, restantes)
        repeat(cantidad) { productos.add(pizzeria.menu[opcion - 1]) }
    }
    if (productos.size == maximo) println("Alcanzaste el máximo de $maximo productos de esta selección. Selección terminada.")
    return productos
}

private fun crearPedido(pizzeria: Pizzeria, combinar: Boolean = false) {
    val clientes = pizzeria.consultarClientes()
    if (clientes.isEmpty()) {
        println("Primero registra un cliente con la opción 1.")
        return
    }
    println("\n--- Clientes registrados ---")
    clientes.forEachIndexed { indice, cliente -> println("${indice + 1}. ${cliente.describir()}") }
    val cliente = clientes[leerEntero("Cliente: ", 1, clientes.size) - 1]
    val fecha = leerFecha("Fecha del pedido (DD/MM/AAAA; Enter para ${formatearFecha(fechaActual())}): ")
    if (combinar) println("Selecciona el primer pedido. Se reserva al menos un cupo para el segundo.")
    val productos = seleccionarProductos(pizzeria, if (combinar) MAX_PRODUCTOS_POR_PEDIDO - 1 else MAX_PRODUCTOS_POR_PEDIDO)
    if (productos.isEmpty()) {
        println("Pedido cancelado: no se agregaron productos.")
        return
    }
    val notas = leerEntrada("Notas opcionales (Enter para omitir): ")
    val primero = pizzeria.crearPropuesta(cliente, productos, notas, fecha)
    val propuesta = if (combinar) {
        println("Selecciona el segundo pedido para el mismo cliente y fecha.")
        val segundoGrupo = seleccionarProductos(pizzeria, MAX_PRODUCTOS_POR_PEDIDO - productos.size)
        if (segundoGrupo.isEmpty()) {
            println("Combinación cancelada: el segundo pedido necesita productos. El inventario se mantiene.")
            return
        }
        val otrasNotas = leerEntrada("Notas del segundo pedido (Enter para omitir): ")
        val segundo = pizzeria.crearPropuesta(cliente, segundoGrupo, otrasNotas, fecha)
        (primero + segundo).also {
            println("Pedidos combinados con +: ${primero.productos.size} + ${segundo.productos.size} = ${it.productos.size} productos; total recalculado.")
        }
    } else primero
    val necesarios = pizzeria.inventario.calcularIngredientesNecesarios(propuesta.productos.flatMap { it.ingredientes }, 1)
    val faltantes = pizzeria.inventario.verificarDisponibilidad(necesarios)
    if (faltantes.isNotEmpty()) {
        println("Pedido no registrado: faltan ingredientes. No se descontó inventario.")
        mostrarFaltantes(faltantes)
        return
    }
    println("\n${propuesta.productos.size} producto(s) para ${cliente.nombre}.")
    mostrarResumen(propuesta)
    mostrarPedidoDesestructurado(propuesta)
    if (!confirmar("¿Confirmar el pedido y descontar los ingredientes?")) {
        println("Pedido cancelado. El inventario se mantiene.")
        return
    }
    val pedido = pizzeria.confirmarPropuesta(propuesta)
    println("Pedido #${pedido.id} confirmado. Se descontaron los ingredientes y comienza en preparación.")
    mostrarResumen(pedido)
    notificarCliente(cliente, "El pedido #${pedido.id} está ${pedido.estado.descripcion.lowercase()}.")
}

private fun avanzarPedido(pizzeria: Pizzeria) {
    val pedidos = pizzeria.consultarPedidos()
    if (pedidos.isEmpty()) {
        println("No hay pedidos para actualizar.")
        return
    }
    println("\n--- Gestión de pedidos ---")
    println("Selecciona un pedido para realizar la acción indicada. Cada selección realiza un solo paso.")
    for (pedido in pedidos) {
        println("#${pedido.id} | ${pedido.cliente.nombre} | ${pedido.estado.descripcion} | Acción: ${siguienteAccion(pedido)}")
    }
    val id = leerEntero("Número del pedido a gestionar: ", 1)
    val pedido = pizzeria.buscarPedido(id)
    if (pedido == null) {
        println("No existe el pedido #$id.")
        return
    }
    when (pedido.estado) {
        EstadoPedido.BORRADOR -> println("Confirma el borrador antes de gestionar su preparación.")
        EstadoPedido.EN_PREPARACION -> {
            pedido.cambiarEstado(EstadoPedido.LISTO)
            println("Pedido #$id marcado como Listo. Aún no tiene repartidor.")
        }
        EstadoPedido.LISTO -> {
            val repartidor = pizzeria.asignarRepartidor(pedido)
            if (repartidor == null) {
                println("Todos los repartidores están ocupados. El pedido #$id sigue Listo y sin repartidor.")
                println("En la opción 3, confirma la entrega de un pedido En camino para liberar a su repartidor; luego vuelve a seleccionar #$id.")
                return
            }
            println("Repartidor asignado: ${repartidor.nombre} (zona ${repartidor.zona.descripcion}).")
            if (repartidor.zona == pedido.cliente.zona) {
                println("Se eligió un repartidor de la misma zona del cliente: ${pedido.cliente.zona.descripcion}.")
            } else {
                println("No había repartidores disponibles en ${pedido.cliente.zona.descripcion}; se eligió la zona más cercana con disponibilidad.")
            }
            println("Pedido #$id En camino. ${repartidor.nombre} tiene este pedido asignado hasta confirmar la entrega.")
        }
        EstadoPedido.EN_CAMINO -> {
            val repartidor = requireNotNull(pedido.repartidor) { "El pedido en camino necesita un repartidor asignado." }
            repartidor.entregarPedido(pedido)
            println("Entrega del pedido #$id registrada. ${repartidor.nombre} vuelve a estar disponible.")
        }
        EstadoPedido.ENTREGADO -> {
            println("Este pedido ya fue entregado.")
            return
        }
    }
    notificarCliente(pedido.cliente, "El pedido #$id ahora está ${pedido.estado.descripcion.lowercase()}.")
    mostrarResumen(pedido)
}

private fun mostrarInventario(pizzeria: Pizzeria) {
    println("\n--- Inventario (porciones de receta) ---")
    for ((ingrediente, cantidad) in pizzeria.inventario.consultar()) println("$ingrediente: $cantidad")
    println("Ingredientes distintos del menú: ${pizzeria.ingredientesDelMenu.joinToString()}")
}

private fun reabastecer(pizzeria: Pizzeria) {
    val ingredientes = pizzeria.inventario.consultar().keys.toList()
    ingredientes.forEachIndexed { indice, nombre -> println("${indice + 1}. $nombre") }
    println("0. Ingresar ingrediente nuevo")
    val opcion = leerEntero("Ingrediente a reponer (0 para ingresar uno nuevo): ", 0, ingredientes.size)
    val ingrediente = if (opcion == 0) {
        leerTextoObligatorio("Nombre del ingrediente: ")
    } else {
        ingredientes[opcion - 1]
    }
    val cantidad = leerEntero("Porciones a agregar (1 a 100): ", 1, 100)
    pizzeria.inventario.reabastecer(ingrediente, cantidad)
    println("Inventario actualizado: $ingrediente = ${pizzeria.inventario.consultar()[ingrediente]} porción/es.")
}
