import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
    .withResolverStyle(ResolverStyle.STRICT)

fun formatearFecha(fecha: LocalDate): String = fecha.format(FORMATO_FECHA)

fun interpretarFecha(entrada: String): LocalDate {
    require(entrada.matches(Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}"))) {
        "Fecha inválida. Usa una fecha existente con formato DD/MM/AAAA."
    }
    return LocalDate.parse(entrada, FORMATO_FECHA)
}
