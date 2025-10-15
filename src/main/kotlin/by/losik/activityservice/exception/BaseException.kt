package by.losik.activityservice.exception

open class BaseException(
    val errorCode: String,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)