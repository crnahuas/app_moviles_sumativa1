import java.math.BigDecimal
import java.time.LocalDate

fun mostrarDemostracion() {
    println("=== RESUMEN INTEGRADO | SEMANA 3 | FORMA B ===")
    println("Demostración reproducible con datos de ejemplo. Fecha fija: 09/11/2026.")
    val pizzeria = crearPizzeria()
    val cliente = Cliente("Carolina Muñoz", "+561234567890", "carolina.munoz@example.com",
        "Av. Los Aromos 1240, departamento 302", Zona.CENTRO)

    println("\n1. Regex y rangos")
    for ((etiqueta, operacion) in listOf<Pair<String, () -> Any>>(
        "Correo inválido" to { validarCorreo("carolina@") },
        "Teléfono incompleto" to { formatearTelefono("91234567") }
    )) {
        try {
            operacion()
        } catch (error: IllegalArgumentException) {
            println("$etiqueta rechazado: ${error.message}")
        }
    }
    println("Correo validado: ${validarCorreo(cliente.correo)}")
    println("Teléfono formateado: ${formatearTelefono("561234567890")}")
    println("Cantidades permitidas: 1..100; 0 pertenece: ${0 in 1..100}; 100 pertenece: ${100 in 1..100}; 101 pertenece: ${101 in 1..100}.")
    for (fecha in listOf(LocalDate.of(2026, 10, 31), pizzeria.promocion.inicio, pizzeria.promocion.fin, LocalDate.of(2026, 12, 1))) {
        println("Fecha ${formatearFecha(fecha)} dentro del periodo: ${fecha in pizzeria.promocion}")
    }

    println("\n2. Igualdad y prevención de duplicados")
    println("Primer cliente registrado: ${pizzeria.registrarCliente(cliente)}")
    val duplicado = Cliente("CAROLINA MUÑOZ", "+569876543210", "CAROLINA.MUNOZ@example.com",
        cliente.direccion, cliente.zona)
    println("Cliente == duplicado: ${cliente == duplicado}; misma referencia: ${cliente === duplicado}")
    println("Hash de clientes coincidente: ${cliente.hashCode() == duplicado.hashCode()}")
    println("Registro de cliente duplicado aceptado: ${pizzeria.registrarCliente(duplicado)}")
    val margarita = pizzeria.menu[0]
    val mismoProducto = Producto("MARGARITA", BigDecimal("1.00"), listOf(Ingrediente("masa")))
    val otraCategoria = Producto("Margarita", BigDecimal("1.00"), listOf(Ingrediente("botella")), Categoria.BEBIDA)
    println("Producto == mismo nombre y categoría: ${margarita == mismoProducto}")
    println("Producto == mismo nombre y otra categoría: ${margarita == otraCategoria}")
    println("Hash de productos coincidente: ${margarita.hashCode() == mismoProducto.hashCode()}")
    println("Registro de producto duplicado aceptado: ${pizzeria.registrarProducto(mismoProducto)}")
    println("Registros únicos: ${pizzeria.consultarClientes().size} cliente; ${pizzeria.menu.size} productos.")

    println("\n3. Anotaciones detectadas durante el cálculo")
    mostrarPromocionables(pizzeria)
    val fecha = LocalDate.of(2026, 11, 9)
    val primero = pizzeria.crearPropuesta(cliente, listOf(margarita, margarita), "Entregar en conserjería", fecha)
    val segundo = pizzeria.crearPropuesta(cliente, listOf(pizzeria.menu[1], pizzeria.menu[2]), "Departamento 302", fecha)
    val sinPromocion = pizzeria.crearPropuesta(cliente, primero.productos, fecha = LocalDate.of(2026, 12, 1))
    println("2 Margarita dentro de promoción: ${formatearMonto(primero.calcularTotal().total)}")
    println("2 Margarita fuera de promoción: ${formatearMonto(sinPromocion.calcularTotal().total)}")

    println("\n4. Sobrecarga de + y desestructuración de dos entidades")
    val antes = pizzeria.inventario.consultar()
    val combinado = primero + segundo
    println("Pedido A (2 productos) + Pedido B (2 productos) = ${combinado.productos.size} productos.")
    println("El total se recalcula con descuento por volumen y promoción antes del IVA.")
    println("Combinar conserva el inventario: ${pizzeria.inventario.consultar() == antes}")
    mostrarClienteDesestructurado(cliente)
    mostrarPedidoDesestructurado(combinado)
    mostrarResumen(combinado)
    val confirmado = pizzeria.confirmarPropuesta(combinado)
    println("Pedido combinado confirmado una vez con identificador #${confirmado.id}.")
    println("Stock tras confirmar cuatro pizzas: ${pizzeria.inventario.consultar()}")
    try {
        pizzeria.confirmarPropuesta(combinado)
    } catch (error: IllegalArgumentException) {
        println("Segunda confirmación rechazada: ${error.message}")
    }

    println("\n5. Reflection de clases del dominio")
    listOf(Cliente::class, Producto::class, Pedido::class, PizzaPromocional::class, PizzaDeTemporada::class)
        .forEach { mostrarInspeccion(it) }

    println("\n6. Seguimiento de la entrega")
    confirmado.cambiarEstado(EstadoPedido.LISTO)
    val repartidor = requireNotNull(pizzeria.asignarRepartidor(confirmado))
    println("Pedido #${confirmado.id}: ${confirmado.estado.descripcion} con ${repartidor.nombre}.")
    repartidor.entregarPedido(confirmado)
    println("Entrega registrada; ${repartidor.nombre} disponible: ${repartidor.disponible}.")
    mostrarReporte(pizzeria.consultarPedidos())
    println("\n=== FIN DEL RESUMEN DE SEMANA 3 ===")
}
