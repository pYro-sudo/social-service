package by.losik.activityservice.exception

class NotFoundException(
    message: String,
    cause: Throwable? = null
) : BaseException("NOT_FOUND", message, cause)
