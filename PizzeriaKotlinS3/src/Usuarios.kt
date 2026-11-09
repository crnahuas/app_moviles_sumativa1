import java.util.Locale

private val PATRON_CORREO = Regex("[A-Za-z0-9_+%-]+(?:\\.[A-Za-z0-9_+%-]+)*@[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*(?:\\.[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)*\\.[A-Za-z]{2,}")
private val TELEFONO_DIGITOS = Regex("\\+?([0-9]{2})([0-9]{3})([0-9]{3})([0-9]{4})")
private val TELEFONO_FORMATEADO = Regex("\\+([0-9]{2}) \\(([0-9]{3})\\) ([0-9]{3})-([0-9]{4})")

fun validarCorreo(entrada: String): String {
    val correo = entrada.trim()
    require(correo.length <= 254 && PATRON_CORREO.matches(correo)) {
        "Correo inválido. Usa el formato nombre@dominio.com."
    }
    return correo
}

fun formatearTelefono(entrada: String): String {
    val telefono = entrada.trim()
    val grupos = TELEFONO_DIGITOS.matchEntire(telefono)?.groupValues
        ?: TELEFONO_FORMATEADO.matchEntire(telefono)?.groupValues
    require(grupos != null) {
        "Teléfono inválido. Ingresa 12 dígitos (2 de país y 10 nacionales) o +XX (XXX) XXX-XXXX."
    }
    return "+${grupos[1]} (${grupos[2]}) ${grupos[3]}-${grupos[4]}"
}

fun validarTelefono(entrada: String): String = formatearTelefono(entrada)

// Orden de las zonas para representar cercanía de forma sencilla en el ejercicio.
enum class Zona(val descripcion: String) {
    NORTE("Norte"),
    CENTRO("Centro"),
    SUR("Sur")
}

open class Usuario(val nombre: String, val contacto: String) {
    init {
        require(nombre.isNotBlank()) { "El nombre no puede estar vacío." }
    }

    open fun describir(): String = "$nombre | Contacto: $contacto"
}

class Cliente(
    nombre: String,
    telefono: String,
    correo: String,
    val direccion: String,
    val zona: Zona
) : Usuario(nombre.trim(), validarTelefono(telefono)) {
    val correo: String = validarCorreo(correo)

    init {
        require(direccion.isNotBlank()) { "La dirección no puede estar vacía." }
    }

    operator fun component1(): String = nombre
    operator fun component2(): String = correo
    operator fun component3(): String = contacto

    override fun equals(other: Any?): Boolean = other is Cliente &&
        claveNombre(nombre) == claveNombre(other.nombre) &&
        correo.lowercase(Locale.ROOT) == other.correo.lowercase(Locale.ROOT)

    override fun hashCode(): Int = 31 * claveNombre(nombre).hashCode() + correo.lowercase(Locale.ROOT).hashCode()

    override fun describir(): String =
        "Cliente: $nombre | Teléfono: $contacto | Correo: $correo"
}

class Repartidor(nombre: String, contacto: String, zona: Zona) : Usuario(nombre, formatearTelefono(contacto)) {
    var zona: Zona = zona
        private set
    var disponible: Boolean = true
        private set
    private var pedidoAsignado: Pedido? = null

    fun asignarPedido(pedido: Pedido) {
        require(disponible) { "$nombre ya tiene un pedido asignado." }
        pedido.iniciarReparto(this)
        pedidoAsignado = pedido
        disponible = false
    }

    fun entregarPedido(pedido: Pedido) {
        require(pedidoAsignado === pedido) { "Este pedido no está asignado a $nombre." }
        pedido.finalizarEntrega(this)
        zona = pedido.cliente.zona
        pedidoAsignado = null
        disponible = true
    }

    override fun describir(): String =
        "Repartidor: $nombre | Zona: ${zona.descripcion} | " +
            if (disponible) "Disponible" else "Ocupado"
}
