package kr.caredoc.careinsurance.user

data class UserCreationCommand(
    val name: String,
    val emailAddressForLogin: String,
)
