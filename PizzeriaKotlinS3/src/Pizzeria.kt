import java.math.BigDecimal
import kotlin.math.abs

class Pizzeria(
    menu: List<Producto>,
    val inventario: Inventario,
    val repartidores: List<Repartidor>,
    val promocion: PeriodoPromocion = PROMOCION_SEMANA_3
) {
    private val clientes = linkedSetOf<Cliente>()
    private val catalogo = menu.toMutableSet()
    val menu: List<Producto> get() = catalogo.toList()

    fun registrarProducto(producto: Producto): Boolean = catalogo.add(producto)
    private var pedidosDelDia: Array<Pedido> = arrayOf()

    // Set evita repetir ingredientes compartidos por varias recetas.
    val ingredientesDelMenu: Set<String> get() = menu.flatMap { producto ->
        producto.ingredientes.map { it.nombre }
    }.toSet()

    fun registrarCliente(cliente: Cliente): Boolean = clientes.add(cliente)

    fun consultarClientes(): List<Cliente> = clientes.toList()

    fun consultarPedidos(): Array<Pedido> = pedidosDelDia.copyOf()

    fun crearPropuesta(
        cliente: Cliente,
        productos: List<Producto>,
        notas: String? = null,
        fecha: java.time.LocalDate = fechaActual()
    ): Pedido {
        val registrado = clientes.find { it == cliente }
            ?: throw IllegalArgumentException("Primero debes registrar al cliente.")
        // La igualdad impide duplicados; el precio y la receta siempre vienen del catálogo.
        val seleccion = productos.map { elegido ->
            catalogo.find { it == elegido }
                ?: throw IllegalArgumentException("El pedido contiene un producto que no está en el menú.")
        }
        return Pedido(0, registrado, seleccion, notas, fecha, promocion)
    }

    fun confirmarPropuesta(propuesta: Pedido): Pedido {
        require(propuesta.estado == EstadoPedido.BORRADOR) { "Este pedido ya fue confirmado." }
        require(clientes.any { it === propuesta.cliente }) { "Primero debes registrar al cliente." }
        require(propuesta.productos.all { producto -> catalogo.any { it === producto } }) {
            "Confirma productos del catálogo original para conservar precios y recetas."
        }
        require(propuesta.periodo == promocion) { "El periodo promocional debe coincidir con el de la pizzería." }
        propuesta.calcularTotal()
        inventario.prepararLote(propuesta.productos.flatMap { it.ingredientes }, 1)
        propuesta.confirmarConId(pedidosDelDia.size + 1)
        pedidosDelDia += propuesta
        return propuesta
    }

    fun confirmarPedido(
        cliente: Cliente,
        productos: List<Producto>,
        notas: String?,
        fecha: java.time.LocalDate = fechaActual()
    ): Pedido = confirmarPropuesta(crearPropuesta(cliente, productos, notas, fecha))

    fun buscarPedido(id: Int): Pedido? = pedidosDelDia.find { it.id == id }

    fun asignarRepartidor(pedido: Pedido): Repartidor? {
        require(pedidosDelDia.any { it === pedido }) { "El pedido no está registrado." }
        require(pedido.estado == EstadoPedido.LISTO) { "El pedido debe estar listo para asignar repartidor." }
        // Misma zona primero, luego la vecina y finalmente la más alejada.
        // Norte y Sur tienen Centro entre ambas. Un empate conserva el orden de la lista.
        val cercano = repartidores
            .sortedBy { abs(it.zona.ordinal - pedido.cliente.zona.ordinal) }
            .find { it.disponible }
        cercano?.asignarPedido(pedido)
        return cercano
    }
}

fun crearPizzeria(): Pizzeria {
    val base = listOf(Ingrediente("masa"), Ingrediente("salsa"), Ingrediente("queso"))
    val menu = listOf(
        PizzaPromocional("Margarita", BigDecimal("8500.00"), base),
        Producto("Pepperoni", BigDecimal("9500.00"), base + Ingrediente("pepperoni")),
        PizzaDeTemporada("Vegetariana", BigDecimal("9000.00"), base + Ingrediente("champiñones"))
    )
    val inventario = Inventario(mapOf(
        "masa" to 20, "salsa" to 20, "queso" to 20,
        "pepperoni" to 10, "champiñones" to 10
    ))
    val repartidores = listOf(
        Repartidor("Camila", "+561111111111", Zona.NORTE),
        Repartidor("Diego", "+562222222222", Zona.CENTRO),
        Repartidor("Sofía", "+563333333333", Zona.SUR)
    )
    return Pizzeria(menu, inventario, repartidores)
}
