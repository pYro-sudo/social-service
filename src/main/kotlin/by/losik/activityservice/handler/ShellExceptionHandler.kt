package by.losik.activityservice.handler

import by.losik.activityservice.exception.BaseException
import by.losik.activityservice.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.shell.command.CommandExceptionResolver
import org.springframework.shell.command.CommandHandlingResult
import org.springframework.stereotype.Component

@Component
class ShellExceptionHandler : CommandExceptionResolver {

    companion object {
        private val log = LoggerFactory.getLogger(ShellExceptionHandler::class.java)
    }

    override fun resolve(ex: Exception): CommandHandlingResult? {
        return when (ex) {
            is NotFoundException -> {
                log.warn("Resource not found: {}", ex.message)
                CommandHandlingResult.of("Resource not found: ${ex.message}\n")
            }
            is BaseException -> {
                log.warn("Business error: {}", ex.message)
                CommandHandlingResult.of("Business error: ${ex.message}\n")
            }
            is DuplicateKeyException -> {
                log.warn("Duplicate key: {}", ex.message)
                CommandHandlingResult.of("Resource already exists\n")
            }
            is IllegalArgumentException -> {
                log.warn("Invalid argument: {}", ex.message)
                CommandHandlingResult.of("Invalid input: ${ex.message}\n")
            }
            is java.time.format.DateTimeParseException -> {
                log.warn("Date parsing error: {}", ex.message)
                CommandHandlingResult.of("Invalid date format. Please use yyyy-MM-dd format\n")
            }
            else -> {
                log.error("Unexpected error", ex)
                CommandHandlingResult.of("Unexpected error: ${ex.message}\n")
            }
        }
    }
}