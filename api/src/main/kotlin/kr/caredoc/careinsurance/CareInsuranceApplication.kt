package kr.caredoc.careinsurance

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CareInsuranceApplication

fun main(args: Array<String>) {
    runApplication<CareInsuranceApplication>(*args)
}
