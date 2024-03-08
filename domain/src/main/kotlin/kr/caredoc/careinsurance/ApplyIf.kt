package kr.caredoc.careinsurance

fun <T> T.applyIf(condition: Boolean, block: T.() -> T): T {
    if (condition) {
        block(this)
    }

    return this
}
