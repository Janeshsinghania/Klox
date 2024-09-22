import kotlin.RuntimeException

class RunTimeError(val token: Token, override val message: String): RuntimeException()