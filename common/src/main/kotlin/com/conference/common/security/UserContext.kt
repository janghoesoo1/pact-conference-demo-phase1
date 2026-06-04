package com.conference.common.security

data class UserContext(
    val userId: Int,
    val roles: List<String>
) {
    fun hasRole(role: String): Boolean = roles.contains(role)
    fun isOrganizer(): Boolean = hasRole("ORGANIZER")
    fun isAttendee(): Boolean = hasRole("ATTENDEE")
}

object UserContextHolder {
    private val context = ThreadLocal<UserContext?>()

    fun set(userContext: UserContext) = context.set(userContext)
    fun get(): UserContext? = context.get()
    fun clear() = context.remove()
}
