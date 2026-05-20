package io.trading.domain.user

@JvmInline
value class Email(val value: String) {
    init {
        require(REGEX.matches(value)) { "Invalid email format" }
    }

    val masked: String
        get() {
            val at = value.indexOf('@')
            if (at <= 0) return "***"
            val local = value.substring(0, at)
            val domain = value.substring(at)
            val visible = local.take(2)
            return "$visible${"*".repeat((local.length - 2).coerceAtLeast(1))}$domain"
        }

    companion object {
        private val REGEX = Regex("""^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$""")
    }
}
