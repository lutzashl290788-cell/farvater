package app.farvater.core.model

import kotlinx.serialization.Serializable

enum class NetMode { WHITE, BLACK }

@Serializable
enum class NetModeChoice { AUTO, WHITE, BLACK }

@Serializable
enum class SourceMode {
    ANY, WHITE, BLACK;

    fun fits(mode: NetMode?): Boolean = mode == null || this == ANY || (this == WHITE) == (mode == NetMode.WHITE)
}
