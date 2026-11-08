package com.hextech.vango

enum class PortType {
    ANALOG_PERCENT,
    ANALOG_TEMP,
    DIM,
}

data class Port (
    private val type: PortType,
    private val id: Int,
    private val name: String,
    private var value: Int
) {
    fun getValue(): Int { return value }
    fun setValue(value: Int) { this.value = value }
    fun getName(): Int { return value }
    fun setName(value: Int) { this.value = value }

    fun getID(): Int { return id }
    fun getType(): PortType { return type }
}

