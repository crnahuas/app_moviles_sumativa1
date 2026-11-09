import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.math.BigDecimal
import org.junit.jupiter.api.Test

class Pruebas {
    @Test
    fun `Ingrediente nuevo se registra, repone y puede consumirse`() {
        val inventario = Inventario(emptyMap())
        inventario.reabastecer(" aceitunas ", 3)
        inventario.reabastecer("aceitunas", 2)
        check(inventario.consultar() == mapOf("aceitunas" to 5))
        inventario.prepararLote(listOf(Ingrediente("aceitunas")), 2)
        check(inventario.consultar() == mapOf("aceitunas" to 3))
    }

    @Test
    fun `Alta de ingrediente inválida no modifica el catálogo`() {
        val inventario = Inventario(mapOf("masa" to 20))
        val antes = inventario.consultar()
        for (nombre in listOf("", "   ")) {
            rechaza<IllegalArgumentException> { inventario.reabastecer(nombre, 1) }
        }
        for (cantidad in listOf(0, -1)) {
            rechaza<IllegalArgumentException> { inventario.reabastecer("aceitunas", cantidad) }
        }
        check(inventario.consultar() == antes)
    }

    @Test
    fun `Un pedido acepta el máximo acumulado entre variedades`() {
        val pizzeria = crearPizzeria()
        val cliente = cliente()
        pizzeria.registrarCliente(cliente)
        for (ingrediente in pizzeria.inventario.consultar().keys) {
            pizzeria.inventario.reabastecer(ingrediente, MAX_PRODUCTOS_POR_PEDIDO)
        }
        val productos = List(MAX_PRODUCTOS_POR_PEDIDO - 1) { pizzeria.menu[0] } + pizzeria.menu[1]
        val pedido = pizzeria.confirmarPedido(cliente, productos, null)
        check(pedido.productos.size == MAX_PRODUCTOS_POR_PEDIDO)
        check(pizzeria.inventario.consultar()["masa"] == 20)
        check(pizzeria.inventario.consultar()["pepperoni"] == 109)
    }

    @Test
    fun `Exceder el máximo se rechaza sin consumir stock ni identificador`() {
        val pizzeria = crearPizzeria()
        val cliente = cliente()
        pizzeria.registrarCliente(cliente)
        for (ingrediente in pizzeria.inventario.consultar().keys) {
            pizzeria.inventario.reabastecer(ingrediente, MAX_PRODUCTOS_POR_PEDIDO)
        }
        val antes = pizzeria.inventario.consultar()
        val productos = List(MAX_PRODUCTOS_POR_PEDIDO) { pizzeria.menu[0] } + pizzeria.menu[1]
        rechaza<IllegalArgumentException> { Pedido(1, cliente, productos) }
        val error = rechaza<IllegalArgumentException> {
            pizzeria.confirmarPedido(cliente, productos, null)
        }
        check("como máximo $MAX_PRODUCTOS_POR_PEDIDO productos" in error.message.orEmpty())
        check(pizzeria.inventario.consultar() == antes)
        check(pizzeria.consultarPedidos().isEmpty())
        check(pizzeria.confirmarPedido(cliente, listOf(pizzeria.menu[0]), null).id == 1)
    }

    @Test
    fun `Consola permite registrar ingredientes nuevos y reponerlos`() {
        val salida = conEntrada("5\n0\n\naceitunas\n0\n3\n5\n6\n2\n4\n0\n") {
            capturar { main() }
        }
        check("Este dato es obligatorio" in salida)
        check("Ingresa un número entre 1 y" in salida)
        check("aceitunas: 5" in salida)
        check("Sesión finalizada." in salida)
    }

    @Test
    fun `Consola limita la cantidad restante entre selecciones y confirma al llegar al máximo`() {
        val entrada = "5\n1\n100\n5\n2\n100\n5\n3\n100\n5\n4\n100\n" +
            "1\nAna\n+561234567890\nana@duoc.cl\nCalle 123\n1\n" +
            "2\n1\n01/12/2026\n1\n60\n2\n41\n40\n\ns\n4\n0\n"
        val salida = conEntrada(entrada) { capturar { main() } }
        check("puedes agregar hasta 40 productos más" in salida)
        check("Ingresa un número entre 1 y 40" in salida)
        check("Alcanzaste el máximo de 100 productos" in salida)
        check("100 producto(s) para Ana" in salida)
        check("Pedido #1 confirmado" in salida)
        check("masa: 20" in salida && "pepperoni: 70" in salida)
        check("Sesión finalizada." in salida)
    }

    @Test
    fun `Tres pizzas no reciben descuento`() {
        val totales = calcularTotal(List(3) { crearPizzeria().menu.first() })
        dinero("25500.00", totales.subtotal)
        dinero("0.00", totales.descuento)
        dinero("4845.00", totales.impuestos)
        dinero("30345.00", totales.total)
    }

    @Test
    fun `Cuatro pizzas reciben descuento antes del impuesto`() {
        val pizza = crearPizzeria().menu.first()
        val totales = calcularTotal(List(4) { pizza })
        dinero("34000.00", totales.subtotal)
        dinero("3400.00", totales.descuento)
        dinero("5814.00", totales.impuestos)
        dinero("36414.00", totales.total)
    }

    @Test
    fun `Pedido mixto y tasa configurable`() {
        val totales = calcularTotal(crearPizzeria().menu, BigDecimal.ZERO)
        dinero("27000.00", totales.total)
        dinero("0.00", totales.impuestos)
        rechaza<IllegalArgumentException> { calcularTotal(emptyList()) }
        rechaza<IllegalArgumentException> { calcularTotal(crearPizzeria().menu, BigDecimal("-0.1")) }
    }

    @Test
    fun `Cálculo decimal exacto y redondeo`() {
        val pizza = Producto("Prueba", BigDecimal("0.10"), listOf(Ingrediente("masa")))
        val totales = calcularTotal(List(4) { pizza })
        dinero("0.40", totales.subtotal)
        dinero("0.04", totales.descuento)
        dinero("0.07", totales.impuestos)
        dinero("0.43", totales.total)
        check(formatearMonto(BigDecimal("8500.5")) == "8.500,50 pesos")
    }

    @Test
    fun `Contactos válidos se formatean y los inválidos se rechazan`() {
        for (telefono in listOf("561234567890", "+561234567890", " +56 (123) 456-7890 ")) {
            check(validarTelefono(telefono) == "+56 (123) 456-7890")
        }
        check(validarTelefono("+121234567890") == "+12 (123) 456-7890")
        for (telefono in listOf("", "abc", "912345678", "+56912345678", "+56 9 1234 5678", "5612345678901", "+56 (123) 456-7890abc", "+56 (12) 456-7890", "+56 (123) 456-789")) {
            rechaza<IllegalArgumentException> { validarTelefono(telefono) }
        }
        check(validarCorreo(" ana.perez+curso@duoc.cl ") == "ana.perez+curso@duoc.cl")
        for (correo in listOf("", "ana@", "ana..perez@duoc.cl", "ana@-duoc.cl", "ana duoc.cl")) {
            rechaza<IllegalArgumentException> { validarCorreo(correo) }
        }
    }

    @Test
    fun `Producto valida el precio y Usuario aplica polimorfismo`() {
        rechaza<IllegalArgumentException> {
            Producto("Prueba", BigDecimal("-1"), listOf(Ingrediente("masa")))
        }
        val usuarios: List<Usuario> = listOf(cliente(), Repartidor("Camila", "+561111111111", Zona.NORTE))
        check(usuarios[0].describir().startsWith("Cliente:"))
        check(usuarios[1].describir().startsWith("Repartidor:"))
    }

    @Test
    fun `Notas nulas o vacías usan el valor predeterminado`() {
        val pizza = crearPizzeria().menu.first()
        check(Pedido(1, cliente(), listOf(pizza)).notas == "Sin notas")
        check(Pedido(2, cliente(), listOf(pizza), "  ").notas == "Sin notas")
        check(Pedido(3, cliente(), listOf(pizza), " Sin aceitunas ").notas == "Sin aceitunas")
        check(capturar { notificarCliente(null, "Prueba") }.isEmpty())
        check(capturar { notificarCliente(cliente(), "Prueba") }.contains("Aviso para Ana"))
    }

    @Test
    fun `Lote suma ingredientes repetidos y multiplica cantidades`() {
        val inventario = Inventario(mapOf("queso" to 10))
        val receta = listOf(Ingrediente("queso", 1), Ingrediente("queso", 2))
        check(inventario.prepararLote(receta, 3) == mapOf("queso" to 9))
        check(inventario.consultar()["queso"] == 1)
    }

    @Test
    fun `Stock insuficiente no descuenta parcialmente`() {
        val inventario = Inventario(mapOf("masa" to 4, "queso" to 0))
        val antes = inventario.consultar()
        val error = rechaza<StockInsuficienteException> {
            inventario.prepararLote(listOf(Ingrediente("masa"), Ingrediente("queso")), 2)
        }
        check(inventario.consultar() == antes)
        check(error.faltantes.single().cantidadAReponer == 2)
        check(capturar { mostrarFaltantes(error.faltantes) }.contains("Reabastecer ingrediente"))
    }

    @Test
    fun `Ingrediente inexistente se informa como faltante`() {
        val inventario = Inventario(mapOf("masa" to 1))
        val faltantes = inventario.verificarDisponibilidad(mapOf("tomate" to 2))
        check(faltantes.single() == Faltante("tomate", 0, 2))
    }

    @Test
    fun `Inventario rechaza negativos y desbordamientos sin modificarse`() {
        rechaza<IllegalArgumentException> { Inventario(mapOf("masa" to -1)) }
        val inventario = Inventario(mapOf("masa" to Int.MAX_VALUE))
        rechaza<IllegalArgumentException> { inventario.reabastecer("masa", -1) }
        rechaza<ArithmeticException> { inventario.reabastecer("masa", 1) }
        rechaza<ArithmeticException> { inventario.prepararLote(listOf(Ingrediente("masa", 2)), Int.MAX_VALUE) }
        check(inventario.consultar()["masa"] == Int.MAX_VALUE)
    }

    @Test
    fun `Reabastecer permite aceptar un pedido antes rechazado`() {
        val inventario = Inventario(mapOf("masa" to 0))
        rechaza<StockInsuficienteException> { inventario.prepararLote(listOf(Ingrediente("masa")), 1) }
        inventario.reabastecer("masa", 2)
        inventario.prepararLote(listOf(Ingrediente("masa")), 1)
        check(inventario.consultar()["masa"] == 1)
    }

    @Test
    fun `Pedidos confirmados conservan identificadores y stock`() {
        val pizzeria = crearPizzeria()
        val cliente = cliente()
        pizzeria.registrarCliente(cliente)
        val primero = pizzeria.confirmarPedido(cliente, List(4) { pizzeria.menu.first() }, null)
        val segundo = pizzeria.confirmarPedido(cliente, listOf(pizzeria.menu[1]), "Tocar timbre")
        check(primero.id == 1 && segundo.id == 2)
        check(pizzeria.consultarPedidos().size == 2)
        check(pizzeria.inventario.consultar()["masa"] == 15)
        check(pizzeria.inventario.consultar()["pepperoni"] == 9)
        check(pizzeria.ingredientesDelMenu.size == 5)
    }

    @Test
    fun `Pedido rechazado no se registra ni consume identificador`() {
        val pizzeria = crearPizzeria()
        val cliente = cliente()
        pizzeria.registrarCliente(cliente)
        val antes = pizzeria.inventario.consultar()
        rechaza<StockInsuficienteException> {
            pizzeria.confirmarPedido(cliente, List(21) { pizzeria.menu.first() }, null)
        }
        check(pizzeria.consultarPedidos().isEmpty())
        check(pizzeria.inventario.consultar() == antes)
        check(pizzeria.confirmarPedido(cliente, listOf(pizzeria.menu.first()), null).id == 1)
    }

    @Test
    fun `Cliente no registrado y pedido vacío se rechazan`() {
        val pizzeria = crearPizzeria()
        val cliente = cliente()
        rechaza<IllegalArgumentException> { pizzeria.confirmarPedido(cliente, pizzeria.menu, null) }
        pizzeria.registrarCliente(cliente)
        rechaza<IllegalArgumentException> { pizzeria.confirmarPedido(cliente, emptyList(), null) }
        check(pizzeria.inventario.consultar()["masa"] == 20)
    }

    @Test
    fun `Repartidor de la misma zona, entrega y recuperación de disponibilidad`() {
        val pizzeria = crearPizzeria()
        val cliente = cliente(Zona.CENTRO)
        pizzeria.registrarCliente(cliente)
        val pedido = pizzeria.confirmarPedido(cliente, listOf(pizzeria.menu.first()), null)
        rechaza<IllegalArgumentException> { pizzeria.asignarRepartidor(pedido) }
        pedido.cambiarEstado(EstadoPedido.LISTO)
        val repartidor = pizzeria.asignarRepartidor(pedido) ?: error("Falta repartidor")
        check(repartidor.nombre == "Diego")
        check(!repartidor.disponible && pedido.estado == EstadoPedido.EN_CAMINO)
        rechaza<IllegalArgumentException> { pedido.cambiarEstado(EstadoPedido.ENTREGADO) }
        repartidor.entregarPedido(pedido)
        check(pedido.estado == EstadoPedido.ENTREGADO && repartidor.disponible)
        check(repartidor.zona == cliente.zona)
        rechaza<IllegalArgumentException> { pedido.cambiarEstado(EstadoPedido.LISTO) }
        rechaza<IllegalArgumentException> { repartidor.entregarPedido(pedido) }
    }

    @Test
    fun `Asignación prioriza la misma zona, la vecina y desempata por orden de registro`() {
        val casos = mapOf(
            Zona.NORTE to listOf("Camila", "Diego", "Sofía"),
            Zona.CENTRO to listOf("Diego", "Camila", "Sofía"),
            Zona.SUR to listOf("Sofía", "Diego", "Camila")
        )
        for ((zona, esperados) in casos) {
            val pizzeria = crearPizzeria()
            val cliente = cliente(zona)
            pizzeria.registrarCliente(cliente)
            val nombres = List(3) {
                val pedido = pizzeria.confirmarPedido(cliente, listOf(pizzeria.menu.first()), null)
                pedido.cambiarEstado(EstadoPedido.LISTO)
                pizzeria.asignarRepartidor(pedido)?.nombre
            }
            check(nombres == esperados) { "Orden incorrecto para la zona $zona: $nombres" }
        }
    }

    @Test
    fun `Una entrega actualiza la zona usada en la siguiente asignación`() {
        val base = crearPizzeria()
        val camila = Repartidor("Camila", "+561111111111", Zona.NORTE)
        val diego = Repartidor("Diego", "+562222222222", Zona.CENTRO)
        val pizzeria = Pizzeria(base.menu, base.inventario, listOf(camila, diego))
        val clienteSur = cliente(Zona.SUR)
        val clienteCentro = cliente(Zona.CENTRO)
        pizzeria.registrarCliente(clienteSur)
        pizzeria.registrarCliente(clienteCentro)
        val primero = pizzeria.confirmarPedido(clienteSur, listOf(pizzeria.menu.first()), null)
        primero.cambiarEstado(EstadoPedido.LISTO)
        check(pizzeria.asignarRepartidor(primero) === diego)
        diego.entregarPedido(primero)
        check(diego.zona == Zona.SUR && diego.disponible)
        val segundo = pizzeria.confirmarPedido(clienteCentro, listOf(pizzeria.menu.first()), null)
        segundo.cambiarEstado(EstadoPedido.LISTO)
        check(pizzeria.asignarRepartidor(segundo) === camila)
    }

    @Test
    fun `Repartidores ocupados no se asignan a dos pedidos`() {
        val pizzeria = crearPizzeria()
        val cliente = cliente()
        pizzeria.registrarCliente(cliente)
        val pedidos = List(4) {
            pizzeria.confirmarPedido(cliente, listOf(pizzeria.menu.first()), null)
                .also { it.cambiarEstado(EstadoPedido.LISTO) }
        }
        val asignados = pedidos.take(3).map { pizzeria.asignarRepartidor(it) }
        check(asignados.toSet().size == 3)
        check(pizzeria.asignarRepartidor(pedidos[3]) == null)
        check(pedidos[3].estado == EstadoPedido.LISTO)
        rechaza<IllegalArgumentException> { asignados.first()?.entregarPedido(pedidos[1]) }
        asignados.first()?.entregarPedido(pedidos[0])
        check(pizzeria.asignarRepartidor(pedidos[3]) === asignados.first())
    }

    @Test
    fun `Reporte separa completados y pendientes e incluye resumen`() {
        val pizzeria = crearPizzeria()
        val cliente = cliente()
        pizzeria.registrarCliente(cliente)
        val primero = pizzeria.confirmarPedido(cliente, listOf(pizzeria.menu.first()), null)
        pizzeria.confirmarPedido(cliente, listOf(pizzeria.menu[1]), "Tocar timbre")
        primero.cambiarEstado(EstadoPedido.LISTO)
        pizzeria.asignarRepartidor(primero)?.entregarPedido(primero)
        val reporte = capturar { mostrarReporte(pizzeria.consultarPedidos()) }
        for (texto in listOf("Completados (1): #1", "Pendientes (1): #2", "1 x Margarita", "Sin notas", "Tocar timbre", "Impuestos (19%", "Teléfono:", "Correo:")) {
            check(texto in reporte) { "Falta $texto en el reporte" }
        }
    }

    @Test
    fun `La lectura con try-catch recupera texto, negativos y desbordamientos`() {
        val entradaAnterior = System.`in`
        try {
            System.setIn(ByteArrayInputStream("abc\n-1\n999999999999\n3\n".toByteArray()))
            val salida = capturar { check(leerEntero("Cantidad: ", 0, 10) == 3) }
            check("Entrada inválida" in salida && "entre 0 y 10" in salida)
        } finally {
            System.setIn(entradaAnterior)
        }
    }

    private fun cliente(zona: Zona = Zona.NORTE) =
        Cliente("Ana", "+561234567890", "ana.${zona.name.lowercase()}@duoc.cl", "Calle de prueba 123", zona)

    private fun <T> conEntrada(entrada: String, bloque: () -> T): T {
        val entradaAnterior = System.`in`
        try {
            System.setIn(ByteArrayInputStream(entrada.toByteArray(Charsets.UTF_8)))
            return bloque()
        } finally {
            System.setIn(entradaAnterior)
        }
    }

    private fun dinero(esperado: String, actual: BigDecimal) {
        check(actual.compareTo(BigDecimal(esperado)) == 0) { "Esperado $esperado, obtenido $actual" }
    }

    private inline fun <reified T : Throwable> rechaza(bloque: () -> Unit): T {
        try {
            bloque()
        } catch (error: Throwable) {
            check(error is T) { "Se esperaba ${T::class.simpleName}, ocurrió $error" }
            return error
        }
        error("Se esperaba ${T::class.simpleName} y no se lanzó ninguna excepción")
    }

    private fun capturar(bloque: () -> Unit): String {
        val salidaAnterior = System.out
        val buffer = ByteArrayOutputStream()
        try {
            System.setOut(PrintStream(buffer, true, Charsets.UTF_8))
            bloque()
        } finally {
            System.setOut(salidaAnterior)
        }
        return buffer.toString(Charsets.UTF_8)
    }
}
