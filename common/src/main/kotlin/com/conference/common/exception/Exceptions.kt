package com.conference.common.exception

class ResourceNotFoundException(message: String) : RuntimeException(message)
class UnauthorizedException(message: String) : RuntimeException(message)
class ForbiddenException(message: String) : RuntimeException(message)
