package io.trading.application.orders

import kotlinx.coroutines.sync.Mutex

class StripedMutex(stripes: Int = 1024) {
    init {
        require(stripes > 0 && (stripes and (stripes - 1)) == 0) {
            "stripes must be a power of two, got $stripes"
        }
    }
    private val mask = stripes - 1
    private val mutexes = Array(stripes) { Mutex() }

    fun forKey(key: Any): Mutex = mutexes[key.hashCode() and mask]
}
