data class Faltante(val ingrediente: String, val disponible: Int, val necesario: Int) {
    val cantidadAReponer: Int get() = necesario - disponible
}

class StockInsuficienteException(val faltantes: List<Faltante>) : IllegalArgumentException(
    "No se puede confirmar el pedido: faltan ingredientes."
)

class Inventario(cantidadesIniciales: Map<String, Int>) {
    private val cantidades: MutableMap<String, Int> = cantidadesIniciales.toMutableMap()

    init {
        require(cantidades.keys.all { it.isNotBlank() }) { "Los ingredientes necesitan un nombre." }
        require(cantidades.values.all { it >= 0 }) { "El inventario no admite cantidades negativas." }
    }

    fun consultar(): Map<String, Int> = cantidades.toMap()

    fun calcularIngredientesNecesarios(
        ingredientes: List<Ingrediente>,
        cantidadPizzas: Int
    ): Map<String, Int> {
        require(cantidadPizzas > 0) { "La cantidad de pizzas debe ser mayor que cero." }
        require(ingredientes.isNotEmpty()) { "Debes indicar los ingredientes del lote." }
        val necesarios = mutableMapOf<String, Int>()
        for (ingrediente in ingredientes) {
            val cantidad = Math.multiplyExact(ingrediente.cantidad, cantidadPizzas)
            necesarios[ingrediente.nombre] = Math.addExact(necesarios[ingrediente.nombre] ?: 0, cantidad)
        }
        return necesarios
    }

    // El when distingue un ingrediente inexistente, uno agotado y uno insuficiente.
    fun verificarDisponibilidad(necesarios: Map<String, Int>): List<Faltante> {
        val faltantes = mutableListOf<Faltante>()
        for ((ingrediente, cantidadNecesaria) in necesarios) {
            require(cantidadNecesaria > 0) { "La cantidad necesaria debe ser positiva." }
            val disponible = cantidades[ingrediente]
            when {
                disponible == null -> faltantes.add(Faltante(ingrediente, 0, cantidadNecesaria))
                disponible == 0 -> faltantes.add(Faltante(ingrediente, 0, cantidadNecesaria))
                disponible < cantidadNecesaria -> faltantes.add(Faltante(ingrediente, disponible, cantidadNecesaria))
                else -> Unit
            }
        }
        return faltantes
    }

    // Primero valida todo el lote. Si falta algo, no descuenta ningún ingrediente.
    fun prepararLote(ingredientes: List<Ingrediente>, cantidadPizzas: Int): Map<String, Int> {
        val necesarios = calcularIngredientesNecesarios(ingredientes, cantidadPizzas)
        val faltantes = verificarDisponibilidad(necesarios)
        if (faltantes.isNotEmpty()) throw StockInsuficienteException(faltantes)
        for ((ingrediente, cantidad) in necesarios) {
            cantidades[ingrediente] = cantidades.getValue(ingrediente) - cantidad
        }
        return necesarios
    }

    fun reabastecer(ingrediente: String, cantidad: Int) {
        val nombre = ingrediente.trim()
        require(nombre.isNotBlank()) { "El ingrediente necesita un nombre." }
        require(cantidad > 0) { "La cantidad a reponer debe ser mayor que cero." }
        cantidades[nombre] = Math.addExact(cantidades[nombre] ?: 0, cantidad)
    }
}
