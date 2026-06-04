package com.conference.common.exception

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException, request: HttpServletRequest): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found").apply {
            title = "Resource Not Found"
            type = URI.create("https://conference.example.com/errors/not-found")
            instance = URI.create(request.requestURI)
            setProperty("error", ex.message ?: "Resource not found")
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: HttpServletRequest): ProblemDetail {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid") }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed").apply {
            title = "Validation Error"
            type = URI.create("https://conference.example.com/errors/validation")
            instance = URI.create(request.requestURI)
            setProperty("errors", errors)
        }
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException, request: HttpServletRequest): ProblemDetail {
        val errors = ex.constraintViolations.associate {
            it.propertyPath.toString() to it.message
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Constraint violation").apply {
            title = "Validation Error"
            type = URI.create("https://conference.example.com/errors/validation")
            instance = URI.create(request.requestURI)
            setProperty("errors", errors)
        }
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: HttpServletRequest): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Bad request").apply {
            title = "Bad Request"
            type = URI.create("https://conference.example.com/errors/bad-request")
            instance = URI.create(request.requestURI)
        }
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException, request: HttpServletRequest): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.message ?: "Unauthorized").apply {
            title = "Unauthorized"
            type = URI.create("https://conference.example.com/errors/unauthorized")
            instance = URI.create(request.requestURI)
        }
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(ex: ForbiddenException, request: HttpServletRequest): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.message ?: "Forbidden").apply {
            title = "Forbidden"
            type = URI.create("https://conference.example.com/errors/forbidden")
            instance = URI.create(request.requestURI)
        }
    }
}
