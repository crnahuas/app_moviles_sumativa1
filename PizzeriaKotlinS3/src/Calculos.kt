import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

val TASA_IMPUESTO = BigDecimal("0.19")
val TASA_DESCUENTO = BigDecimal("0.10")

data class Totales(
    val subtotal: BigDecimal,
    val descuento: BigDecimal,
    val impuestos: BigDecimal,
    val total: BigDecimal,
    val descuentoVolumen: BigDecimal = descuento,
    val descuentoPromocional: BigDecimal = BigDecimal("0.00")
)

fun calcularTotal(
    productos: List<Producto>,
    tasaImpuesto: BigDecimal = TASA_IMPUESTO,
    fecha: LocalDate? = null,
    periodo: PeriodoPromocion? = null
): Totales {
    require(productos.isNotEmpty()) { "Agrega al menos un producto al pedido." }
    require(tasaImpuesto >= BigDecimal.ZERO && tasaImpuesto <= BigDecimal.ONE) {
        "La tasa de impuestos debe estar entre 0 y 1."
    }
    val subtotal = productos.fold(BigDecimal.ZERO) { suma, producto -> suma + producto.precio }.moneda()
    val volumen = productos.count { it.categoria == Categoria.PIZZA } > 3
    // El descuento por volumen afecta solo a pizzas. Bebidas y extras no activan el umbral.
    val basePizzas = productos.filter { it.categoria == Categoria.PIZZA }
        .fold(BigDecimal.ZERO) { suma, producto -> suma + producto.precio }
    val descuentoVolumen = (if (volumen) basePizzas * TASA_DESCUENTO else BigDecimal.ZERO).moneda()
    val promocionActiva = fecha != null && periodo != null && fecha in periodo
    val descuentoPromocional = (if (promocionActiva) {
        productos.fold(BigDecimal.ZERO) { suma, producto ->
            val base = if (volumen && producto.categoria == Categoria.PIZZA) {
                producto.precio * (BigDecimal.ONE - TASA_DESCUENTO)
            } else producto.precio
            suma + base * BigDecimal(porcentajePromocional(producto)).movePointLeft(2)
        }
    } else BigDecimal.ZERO).moneda().min(subtotal - descuentoVolumen)
    val descuento = descuentoVolumen + descuentoPromocional
    val baseImponible = subtotal - descuento
    val impuestos = (baseImponible * tasaImpuesto).moneda()
    return Totales(subtotal, descuento, impuestos, baseImponible + impuestos, descuentoVolumen, descuentoPromocional)
}

private fun BigDecimal.moneda(): BigDecimal = setScale(2, RoundingMode.HALF_UP)

fun formatearMonto(monto: BigDecimal): String {
    val formato = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CL"))
    formato.minimumFractionDigits = 2
    formato.maximumFractionDigits = 2
    return "${formato.format(monto)} pesos"
}
