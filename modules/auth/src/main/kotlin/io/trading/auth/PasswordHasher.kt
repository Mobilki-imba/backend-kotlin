package io.trading.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PasswordHasher(
    private val cost: Int = 12,
    private val dispatcher: CoroutineDispatcher = defaultDispatcher(),
) {
    private val hasher = BCrypt.withDefaults()
    private val verifier = BCrypt.verifyer()

    suspend fun hash(password: String): String = withContext(dispatcher) {
        hasher.hashToString(cost, password.toCharArray())
    }

    suspend fun verify(password: String, hash: String): Boolean = withContext(dispatcher) {
        verifier.verify(password.toCharArray(), hash).verified
    }

    companion object {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        fun defaultDispatcher(): CoroutineDispatcher {
            val cores = Runtime.getRuntime().availableProcessors()
            val parallelism = maxOf(2, cores - 2)
            return Dispatchers.Default.limitedParallelism(parallelism)
        }
    }
}

object PasswordPolicy {
    private val REGEX = Regex("""^(?=.*[A-Za-z])(?=.*\d).{8,}$""")
    fun isValid(password: String): Boolean = REGEX.matches(password)
}
