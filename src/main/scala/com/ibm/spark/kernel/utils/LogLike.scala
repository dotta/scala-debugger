package com.ibm.spark.kernel.utils

import org.slf4j.{Logger, LoggerFactory}

/**
 * A trait for mixing in logging. This trait
 * exposes a {@link org.slf4j.Logger}
 * through a protected field called logger
 */
trait LogLike {
  val loggerName = this.getClass.getName
  protected val logger = LoggerFactory.getLogger(loggerName)

  implicit class LoggerExtras(private val logger: Logger) {
    def throwable(throwable: Throwable): Unit = {
      require(throwable != null, "Throwable cannot be null!")

      val message = {
        val localizedMessage = throwable.getLocalizedMessage

        if (localizedMessage != null) localizedMessage
        else throwable.getClass.getName
      }

      logger.error(message)
    }
  }
}
