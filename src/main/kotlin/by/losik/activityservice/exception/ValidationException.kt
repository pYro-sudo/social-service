package by.losik.activityservice.exception

class ValidationException(
    message: String,
    cause: Throwable? = null
) : BaseException("VALIDATION_ERROR", message, cause)

