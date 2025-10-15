package by.losik.activityservice.exception

class ServiceException(
    errorCode: String,
    message: String,
    cause: Throwable? = null
) : BaseException(errorCode, message, cause)