package com.fahim.pilltime

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform