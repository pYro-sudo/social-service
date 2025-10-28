package by.losik.activityservice.utils

import by.losik.activityservice.exception.NotFoundException
import by.losik.activityservice.exception.ValidationException

object ExceptionUtils {

    fun <T> requireNotEmpty(list: List<T>, message: String): List<T> {
        if (list.isEmpty()) {
            throw NotFoundException(message)
        }
        return list
    }

    fun requireNotNull(value: Any?, message: String) {
        if (value == null) {
            throw NotFoundException(message)
        }
    }

    fun requireValidId(id: String) {
        if (id.isBlank()) {
            throw ValidationException("ID cannot be blank")
        }
    }

    fun requireValidUserId(userId: Long) {
        if (userId <= 0) {
            throw ValidationException("User ID must be positive")
        }
    }

    fun requireValidImageId(imageId: Long) {
        if (imageId <= 0) {
            throw ValidationException("Image ID must be positive")
        }
    }
}