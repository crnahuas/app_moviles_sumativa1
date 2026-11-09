import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

data class InspeccionClase(val clase: String, val propiedades: List<String>, val metodos: List<String>)

fun inspeccionarClase(clase: KClass<*>): InspeccionClase = InspeccionClase(
    clase.simpleName ?: "Clase anónima",
    clase.memberProperties.map { it.name }.sorted(),
    clase.memberFunctions.map { it.name }.distinct().sorted()
)

fun mostrarInspeccion(clase: KClass<*>) {
    val inspeccion = inspeccionarClase(clase)
    println("\nClase: ${inspeccion.clase}")
    println("Propiedades: ${inspeccion.propiedades.joinToString()}")
    println("Métodos: ${inspeccion.metodos.joinToString()}")
}

fun mostrarPromocionables(pizzeria: Pizzeria) {
    println("\n--- Productos identificados con @Promocionable mediante reflection ---")
    val promocionables = pizzeria.menu.filter { porcentajePromocional(it) > 0 }
    if (promocionables.isEmpty()) println("No hay productos anotados.")
    for (producto in promocionables) {
        println("${producto.nombre} | ${producto::class.simpleName} | ${porcentajePromocional(producto)}%")
    }
    println("Periodo inclusivo: ${formatearFecha(pizzeria.promocion.inicio)} a ${formatearFecha(pizzeria.promocion.fin)}.")
}
