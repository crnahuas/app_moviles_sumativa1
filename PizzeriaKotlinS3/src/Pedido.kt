import java.time.LocalDate

const val MAX_PRODUCTOS_POR_PEDIDO = 100

enum class EstadoPedido(val descripcion: String) {
    BORRADOR("Borrador sin confirmar"),
    EN_PREPARACION("En preparación"),
    LISTO("Listo"),
    EN_CAMINO("En camino"),
    ENTREGADO("Entregado")
}

class Pedido(
    id: Int,
    val cliente: Cliente,
    productos: List<Producto>,
    notas: String? = null,
    val fecha: LocalDate = fechaActual(),
    val periodo: PeriodoPromocion? = null
) {
    var id: Int = id
        private set

    // Cada elemento representa una unidad del producto; se agrupan para mostrar las cantidades.
    val productos: List<Producto> = productos.toList()
    val notas: String = notas?.trim()?.takeIf { it.isNotEmpty() } ?: "Sin notas"
    var estado: EstadoPedido = if (id == 0) EstadoPedido.BORRADOR else EstadoPedido.EN_PREPARACION
        private set
    var repartidor: Repartidor? = null
        private set

    init {
        require(id >= 0) { "El identificador no puede ser negativo; 0 identifica un borrador." }
        require(productos.isNotEmpty()) { "El pedido necesita al menos un producto." }
        require(productos.size <= MAX_PRODUCTOS_POR_PEDIDO) {
            "El pedido admite como máximo $MAX_PRODUCTOS_POR_PEDIDO productos en total."
        }
    }

    fun calcularTotal(): Totales = calcularTotal(productos, fecha = fecha, periodo = periodo)

    operator fun component1(): Cliente = cliente
    operator fun component2(): List<Producto> = productos.toList()
    operator fun component3(): java.math.BigDecimal = calcularTotal().total

    // Se combinan borradores antes de reservar stock y asignar un identificador de venta.
    operator fun plus(otro: Pedido): Pedido {
        require(this !== otro) { "Selecciona dos pedidos distintos para combinarlos." }
        require(estado == EstadoPedido.BORRADOR && otro.estado == EstadoPedido.BORRADOR) {
            "Solo se combinan pedidos sin confirmar; los confirmados ya consumieron inventario."
        }
        require(cliente == otro.cliente && cliente.direccion == otro.cliente.direccion && cliente.zona == otro.cliente.zona) {
            "Los pedidos deben pertenecer al mismo cliente y destino de entrega."
        }
        require(fecha == otro.fecha && periodo == otro.periodo) {
            "Los pedidos deben tener la misma fecha y periodo promocional."
        }
        require(productos.size + otro.productos.size <= MAX_PRODUCTOS_POR_PEDIDO) {
            "La combinación admite como máximo $MAX_PRODUCTOS_POR_PEDIDO productos."
        }
        val notasCombinadas = listOf(notas, otro.notas).filter { it != "Sin notas" }.joinToString("; ")
        return Pedido(0, cliente, productos + otro.productos, notasCombinadas, fecha, periodo)
    }

    internal fun confirmarConId(nuevoId: Int) {
        require(estado == EstadoPedido.BORRADOR && nuevoId > 0) { "El borrador ya fue confirmado o el identificador es inválido." }
        id = nuevoId
        estado = EstadoPedido.EN_PREPARACION
    }

    fun cambiarEstado(nuevoEstado: EstadoPedido) {
        require(nuevoEstado != EstadoPedido.ENTREGADO) {
            "Confirma la entrega mediante el repartidor para liberar su disponibilidad."
        }
        val permitido = when (estado) {
            EstadoPedido.BORRADOR -> false
            EstadoPedido.EN_PREPARACION -> nuevoEstado == EstadoPedido.LISTO
            EstadoPedido.LISTO -> nuevoEstado == EstadoPedido.EN_CAMINO && repartidor != null
            EstadoPedido.EN_CAMINO -> false
            EstadoPedido.ENTREGADO -> false
        }
        require(permitido) { "No se puede pasar de ${estado.descripcion} a ${nuevoEstado.descripcion}." }
        estado = nuevoEstado
    }

    internal fun finalizarEntrega(repartidor: Repartidor) {
        require(estado == EstadoPedido.EN_CAMINO && this.repartidor === repartidor) {
            "Solo el repartidor asignado puede finalizar un pedido en camino."
        }
        estado = EstadoPedido.ENTREGADO
    }

    internal fun iniciarReparto(repartidor: Repartidor) {
        require(estado == EstadoPedido.LISTO) { "El pedido debe estar listo antes de salir a reparto." }
        require(this.repartidor == null) { "El pedido ya tiene repartidor." }
        this.repartidor = repartidor
        cambiarEstado(EstadoPedido.EN_CAMINO)
    }
}
