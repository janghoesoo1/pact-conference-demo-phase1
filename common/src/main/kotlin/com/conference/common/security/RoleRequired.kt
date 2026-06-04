package com.conference.common.security

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RoleRequired(vararg val roles: String)
