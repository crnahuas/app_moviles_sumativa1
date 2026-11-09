import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Promocionable(porcentaje = 25)
private class PizzaDePrueba : Producto("Prueba", BigDecimal("100.00"), listOf(Ingrediente("masa")))

class PruebasSemana3 {
    private val fecha = LocalDate.of(2026, 11, 9)

    @Test
    fun `Correo y teléfono inválidos solicitan reingreso sin abandonar el registro`() {
        val salida = consola("1\nAna\nabc\n+56912345678\n+561234567890\nana@\nana@duoc.cl\nCalle 123\n1\n0\n")
        check("Teléfono inválido" in salida && "Correo inválido" in salida)
        check("Registro completado" in salida && "Cliente desestructurado" in salida)
        check("+56 (123) 456-7890" in salida && "Sesión finalizada." in salida)
    }

    @Test
    fun `Fecha imposible o sin formato vuelve a pedirse`() {
        val salida = consola("1\nAna\n561234567890\nana@duoc.cl\nCalle 123\n1\n2\n1\n30/02/2026\n09-11-2026\n09/11/2026\n1\n1\n0\n\ns\n0\n")
        check(salida.split("Fecha inválida").size - 1 == 2)
        check("Fecha del pedido: 09/11/2026" in salida && "Pedido #1 confirmado" in salida)
    }

    @Test
    fun `Fechas usan día mes año y validación estricta de calendario`() {
        check(interpretarFecha("09/11/2026") == fecha)
        check(formatearFecha(fecha) == "09/11/2026")
        check(interpretarFecha("29/02/2028") == LocalDate.of(2028, 2, 29))
        for (entrada in listOf("29/02/2026", "31/04/2026", "2026-11-09", "9/11/2026", "09/13/2026")) {
            assertThrows<Exception> { interpretarFecha(entrada) }
        }
        conEntrada("\n") { check(leerFecha("Fecha: ") == fechaActual()) }
    }

    @Test
    fun `Cantidades 0 y 101 se rechazan y los extremos 1 y 100 se aceptan`() {
        conEntrada("0\n101\n1\n100\n") {
            capturar {
                check(leerEntero("Cantidad: ", 1, 100) == 1)
                check(leerEntero("Cantidad: ", 1, 100) == 100)
            }.also { check(it.split("Ingresa un número entre 1 y 100").size - 1 == 2) }
        }
    }

    @Test
    fun `Periodo incluye inicio y fin y excluye días adyacentes`() {
        val periodo = PROMOCION_SEMANA_3
        for (dia in listOf(periodo.inicio, periodo.fin)) {
            val total = calcularTotal(listOf(crearPizzeria().menu[0]), fecha = dia, periodo = periodo)
            dinero("425.00", total.descuentoPromocional)
            dinero("9609.25", total.total)
        }
        for (dia in listOf(periodo.inicio.minusDays(1), periodo.fin.plusDays(1))) {
            val total = calcularTotal(listOf(crearPizzeria().menu[0]), fecha = dia, periodo = periodo)
            dinero("0.00", total.descuentoPromocional)
            dinero("10115.00", total.total)
        }
        assertThrows<IllegalArgumentException> { PeriodoPromocion(periodo.fin, periodo.inicio) }
    }

    @Test
    fun `Anotaciones de dos tipos se detectan y un producto sin anotar no recibe promoción`() {
        val menu = crearPizzeria().menu
        check(porcentajePromocional(menu[0]) == 5)
        check(porcentajePromocional(menu[1]) == 0)
        check(porcentajePromocional(menu[2]) == 10)
        val pepperoni = calcularTotal(listOf(menu[1]), fecha = fecha, periodo = PROMOCION_SEMANA_3)
        dinero("0.00", pepperoni.descuentoPromocional)
        dinero("11305.00", pepperoni.total)
    }

    @Test
    fun `El porcentaje se descubre mediante reflection sin depender del nombre del producto`() {
        val total = calcularTotal(listOf(PizzaDePrueba()), fecha = fecha, periodo = PROMOCION_SEMANA_3)
        dinero("25.00", total.descuentoPromocional)
        dinero("89.25", total.total)
    }

    @Test
    fun `Reflection lista métodos y propiedades de Cliente Producto y Pedido`() {
        val cliente = inspeccionarClase(Cliente::class)
        val producto = inspeccionarClase(Producto::class)
        val pedido = inspeccionarClase(Pedido::class)
        check(cliente.propiedades.containsAll(listOf("nombre", "correo", "contacto")))
        check(cliente.metodos.containsAll(listOf("component1", "equals", "hashCode")))
        check(producto.propiedades.containsAll(listOf("nombre", "categoria", "precio")))
        check(producto.metodos.containsAll(listOf("detallarProducto", "equals", "hashCode")))
        check(pedido.propiedades.containsAll(listOf("cliente", "productos", "fecha")))
        check(pedido.metodos.containsAll(listOf("plus", "component3", "calcularTotal")))
    }

    @Test
    fun `Combinar 2 y 2 recalcula volumen promoción e IVA sin registrar los borradores`() {
        val pizzeria = pizzeriaConCliente()
        val cliente = pizzeria.consultarClientes().single()
        val antes = pizzeria.inventario.consultar()
        val a = pizzeria.crearPropuesta(cliente, List(2) { pizzeria.menu[0] }, "Conserjería", fecha)
        val b = pizzeria.crearPropuesta(cliente, listOf(pizzeria.menu[1], pizzeria.menu[2]), "Depto 302", fecha)
        val combinado = a + b
        val total = combinado.calcularTotal()
        dinero("35500.00", total.subtotal)
        dinero("3550.00", total.descuentoVolumen)
        dinero("1575.00", total.descuentoPromocional)
        dinero("5771.25", total.impuestos)
        dinero("36146.25", total.total)
        check(combinado.notas == "Conserjería; Depto 302")
        check(a.productos.size == 2 && b.productos.size == 2)
        check(a.id == 0 && b.id == 0 && combinado.id == 0)
        check(pizzeria.inventario.consultar() == antes && pizzeria.consultarPedidos().isEmpty())
        check(total.total < a.calcularTotal().total + b.calcularTotal().total)
    }

    @Test
    fun `Confirmar el combinado consume solo sus ingredientes y rechaza una segunda confirmación`() {
        val pizzeria = pizzeriaConCliente()
        val cliente = pizzeria.consultarClientes().single()
        val a = pizzeria.crearPropuesta(cliente, List(2) { pizzeria.menu[0] }, fecha = fecha)
        val b = pizzeria.crearPropuesta(cliente, List(2) { pizzeria.menu[1] }, fecha = fecha)
        val combinado = pizzeria.confirmarPropuesta(a + b)
        check(combinado.id == 1 && combinado.estado == EstadoPedido.EN_PREPARACION)
        val stock = pizzeria.inventario.consultar()
        check(stock["masa"] == 16 && stock["pepperoni"] == 8)
        assertThrows<IllegalArgumentException> { pizzeria.confirmarPropuesta(combinado) }
        check(pizzeria.inventario.consultar() == stock && pizzeria.consultarPedidos().size == 1)
    }

    @Test
    fun `Operador rechaza otro cliente fecha diferente mismo borrador exceso y pedidos confirmados`() {
        val pizzeria = pizzeriaConCliente()
        val cliente = pizzeria.consultarClientes().single()
        val otro = Cliente("Luis", "561234567891", "luis@duoc.cl", "Calle 234", Zona.SUR)
        pizzeria.registrarCliente(otro)
        val a = pizzeria.crearPropuesta(cliente, listOf(pizzeria.menu[0]), fecha = fecha)
        val b = pizzeria.crearPropuesta(otro, listOf(pizzeria.menu[0]), fecha = fecha)
        val diferenteFecha = pizzeria.crearPropuesta(cliente, listOf(pizzeria.menu[0]), fecha = fecha.plusDays(1))
        val cien = pizzeria.crearPropuesta(cliente, List(100) { pizzeria.menu[0] }, fecha = fecha)
        assertThrows<IllegalArgumentException> { a + b }
        assertThrows<IllegalArgumentException> { a + diferenteFecha }
        assertThrows<IllegalArgumentException> { a + a }
        assertThrows<IllegalArgumentException> { a + cien }
        assertThrows<IllegalArgumentException> { a.cambiarEstado(EstadoPedido.LISTO) }
        val confirmado = pizzeria.confirmarPedido(cliente, listOf(pizzeria.menu[0]), null, fecha)
        assertThrows<IllegalArgumentException> { a + confirmado }
        check(pizzeria.consultarPedidos().size == 1 && pizzeria.inventario.consultar()["masa"] == 19)
    }

    @Test
    fun `Desestructuración entrega los tres componentes de Cliente y Pedido en el orden requerido`() {
        val pizzeria = pizzeriaConCliente()
        val original = pizzeria.consultarClientes().single()
        val (nombre, correo, telefono) = original
        check(nombre == "Ana" && correo == "ana@duoc.cl" && telefono == "+56 (123) 456-7890")
        val propuesta = pizzeria.crearPropuesta(original, listOf(pizzeria.menu[0]), fecha = fecha)
        val (cliente, productos, total) = propuesta
        check(cliente === original && productos == propuesta.productos)
        dinero("9609.25", total)
    }

    @Test
    fun `Clientes iguales comparten hash y se conserva el primer registro`() {
        val pizzeria = pizzeriaConCliente()
        val original = pizzeria.consultarClientes().single()
        val duplicado = Cliente(" ANA ", "569876543210", "ANA@DUOC.CL", "Otra calle", Zona.SUR)
        check(original == duplicado && original !== duplicado)
        check(original.hashCode() == duplicado.hashCode())
        check(setOf(original, duplicado).size == 1)
        check(!pizzeria.registrarCliente(duplicado))
        check(pizzeria.consultarClientes().single() === original)
        val propuesta = pizzeria.crearPropuesta(duplicado, listOf(pizzeria.menu[0]), fecha = fecha)
        check(propuesta.cliente === original)
    }

    @Test
    fun `Productos se comparan por nombre y categoría y el catálogo conserva precios recetas y promociones`() {
        val pizzeria = crearPizzeria()
        val original = pizzeria.menu[0]
        val duplicado = Producto(" MARGARITA ", BigDecimal("1.00"), listOf(Ingrediente("masa")))
        val bebida = Producto("Margarita", BigDecimal("1500.00"), listOf(Ingrediente("botella")), Categoria.BEBIDA)
        check(original == duplicado && original.hashCode() == duplicado.hashCode())
        check(original != bebida && setOf(original, duplicado, bebida).size == 2)
        check(!pizzeria.registrarProducto(duplicado))
        check(pizzeria.registrarProducto(bebida))
        val cliente = Cliente("Ana", "561234567890", "ana@duoc.cl", "Calle 123", Zona.CENTRO)
        pizzeria.registrarCliente(cliente)
        val propuesta = pizzeria.crearPropuesta(cliente, listOf(duplicado), fecha = fecha)
        check(propuesta.productos.single() === original)
        dinero("8500.00", propuesta.calcularTotal().subtotal)
        dinero("425.00", propuesta.calcularTotal().descuentoPromocional)
    }

    @Test
    fun `Bebidas no activan descuento por volumen de pizzas`() {
        val bebida = Producto("Agua", BigDecimal("1000.00"), listOf(Ingrediente("botella")), Categoria.BEBIDA)
        val total = calcularTotal(List(4) { bebida })
        dinero("0.00", total.descuentoVolumen)
        dinero("4760.00", total.total)
    }

    @Test
    fun `Consola combina y confirma pedidos con fecha e inventario coherentes`() {
        val salida = consola("1\nAna\n561234567890\nana@duoc.cl\nCalle 123\n2\n8\n1\n09/11/2026\n1\n2\n0\nConserjería\n2\n1\n3\n1\n0\nDepto 302\ns\n4\n9\n10\n0\n")
        for (texto in listOf("Pedidos combinados con +: 2 + 2 = 4", "36.146,25 pesos", "Pedido #1 confirmado", "masa: 16", "Sesión finalizada.")) {
            check(texto in salida) { "Falta $texto" }
        }
        check(salida.split("Ingresa un número entre 0 y 8").size - 1 == 2)
        check("9. Inspeccionar clases" !in salida && "10. Demostración integrada" !in salida)
    }

    @Test
    fun `Cancelar un pedido combinado no descuenta inventario ni registra pedidos`() {
        val salida = consola("1\nAna\n561234567890\nana@duoc.cl\nCalle 123\n2\n8\n1\n09/11/2026\n1\n1\n0\n\n2\n1\n0\n\nn\n4\n0\n")
        check("Pedido cancelado. El inventario se mantiene." in salida)
        check("masa: 20" in salida && "Todavía no hay pedidos confirmados" in salida)
    }

    @Test
    fun `Main con demo integra los requisitos de la pauta y termina con un pedido entregado`() {
        val salida = capturar { main(arrayOf("--demo")) }
        for (texto in listOf("Correo inválido rechazado", "Teléfono formateado", "Cliente == duplicado: true", "Registro de producto duplicado aceptado: false", "Pedido A (2 productos) + Pedido B (2 productos) = 4", "Cliente desestructurado", "Pedido desestructurado", "@Promocionable", "Clase: Cliente", "Clase: Producto", "Clase: Pedido", "36.146,25 pesos", "Completados (1): #1", "Pendientes (0): Ninguno", "FIN DEL RESUMEN")) {
            check(texto in salida) { "Falta $texto" }
        }
    }

    private fun pizzeriaConCliente(): Pizzeria = crearPizzeria().also {
        it.registrarCliente(Cliente("Ana", "561234567890", "ana@duoc.cl", "Calle 123", Zona.CENTRO))
    }

    private fun dinero(esperado: String, actual: BigDecimal) {
        check(actual.compareTo(BigDecimal(esperado)) == 0) { "Esperado $esperado; obtenido $actual" }
    }

    private fun consola(entrada: String): String = conEntrada(entrada) { capturar { main() } }

    private fun <T> conEntrada(entrada: String, bloque: () -> T): T {
        val anterior = System.`in`
        try {
            System.setIn(ByteArrayInputStream(entrada.toByteArray(Charsets.UTF_8)))
            return bloque()
        } finally {
            System.setIn(anterior)
        }
    }

    private fun capturar(bloque: () -> Unit): String {
        val anterior = System.out
        val buffer = ByteArrayOutputStream()
        try {
            System.setOut(PrintStream(buffer, true, Charsets.UTF_8))
            bloque()
        } finally {
            System.setOut(anterior)
        }
        return buffer.toString(Charsets.UTF_8)
    }
}
