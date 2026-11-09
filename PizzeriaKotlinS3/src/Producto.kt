import java.math.BigDecimal
import java.util.Locale

internal fun claveNombre(nombre: String): String = nombre.trim().lowercase(Locale.ROOT)

enum class Categoria(val descripcion: String) {
    PIZZA("Pizza"), BEBIDA("Bebida"), INGREDIENTE("Ingrediente")
}

data class Ingrediente(val nombre: String, val cantidad: Int = 1) {
    init {
        require(nombre.isNotBlank()) { "El ingrediente necesita un nombre." }
        require(cantidad > 0) { "La cantidad de un ingrediente debe ser positiva." }
    }
}

open class Producto(
    nombre: String,
    val precio: BigDecimal,
    ingredientes: List<Ingrediente>,
    val categoria: Categoria = Categoria.PIZZA
) {
    val nombre: String = nombre.trim()
    val ingredientes: List<Ingrediente> = ingredientes.toList()

    init {
        require(this.nombre.isNotBlank()) { "El producto necesita un nombre." }
        require(precio > BigDecimal.ZERO) { "El precio debe ser mayor que cero." }
        require(precio.stripTrailingZeros().scale() <= 2) { "El precio admite hasta dos decimales." }
        require(ingredientes.isNotEmpty()) { "El producto necesita una receta de inventario." }
    }

    fun detallarProducto(): String {
        val receta = ingredientes.joinToString { "${it.nombre} (${it.cantidad} porción/es)" }
        val promocion = porcentajePromocional(this).takeIf { it > 0 }
            ?.let { " | Promocionable: $it% durante el periodo" } ?: ""
        return "$nombre | ${categoria.descripcion} | ${formatearMonto(precio)} | Ingredientes: $receta$promocion"
    }

    // == delega en equals. Precio, receta y subtipo no cambian la identidad comercial.
    final override fun equals(other: Any?): Boolean = other is Producto &&
        claveNombre(nombre) == claveNombre(other.nombre) && categoria == other.categoria

    final override fun hashCode(): Int = 31 * claveNombre(nombre).hashCode() + categoria.hashCode()
}

@Promocionable(porcentaje = 5)
class PizzaPromocional(nombre: String, precio: BigDecimal, ingredientes: List<Ingrediente>) :
    Producto(nombre, precio, ingredientes)

@Promocionable(porcentaje = 10)
class PizzaDeTemporada(nombre: String, precio: BigDecimal, ingredientes: List<Ingrediente>) :
    Producto(nombre, precio, ingredientes)
