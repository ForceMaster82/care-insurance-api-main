package kr.caredoc.careinsurance

fun ByteArray.toHex() = joinToString("") { "%02X".format(it) }
