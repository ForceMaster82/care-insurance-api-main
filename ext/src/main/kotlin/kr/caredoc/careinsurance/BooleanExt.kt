package kr.caredoc.careinsurance

fun Boolean.thenThrows(throwableGenerator: () -> Throwable) = run {
    if (this) {
        throw throwableGenerator()
    }
}

fun Boolean.then(block: () -> Unit) = if (this) {
    block()
} else {
    Unit
}
