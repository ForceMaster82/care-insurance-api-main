package kr.caredoc.careinsurance.security.authentication

import org.springframework.security.access.AccessDeniedException

class CredentialExpiredException(msg: String) : AccessDeniedException(msg)
