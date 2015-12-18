package org.scaladebugger.api.profiles.pure.exceptions

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import com.sun.jdi.event.ExceptionEvent
import org.scaladebugger.api.lowlevel.JDIArgument
import org.scaladebugger.api.lowlevel.events.{EventManager, JDIEventArgument}
import org.scaladebugger.api.lowlevel.events.filters.UniqueIdPropertyFilter
import org.scaladebugger.api.lowlevel.exceptions.{ExceptionManager, StandardExceptionManager$}
import org.scaladebugger.api.lowlevel.requests.JDIRequestArgument
import org.scaladebugger.api.lowlevel.requests.properties.UniqueIdProperty
import org.scaladebugger.api.lowlevel.utils.JDIArgumentGroup
import org.scaladebugger.api.pipelines.Pipeline
import org.scaladebugger.api.pipelines.Pipeline.IdentityPipeline
import org.scaladebugger.api.profiles.traits.exceptions.ExceptionProfile
import org.scaladebugger.api.utils.Memoization
import org.scaladebugger.api.lowlevel.events.EventType._

import scala.collection.JavaConverters._
import scala.util.Try

/**
 * Represents a pure profile for exceptions that adds no extra logic on
 * top of the standard JDI.
 */
trait PureExceptionProfile extends ExceptionProfile {
  protected val exceptionManager: ExceptionManager
  protected val eventManager: EventManager

  /**
   * Contains mapping from input to a counter indicating how many pipelines
   * are currently active for the input.
   */
  private val pipelineCounter = new ConcurrentHashMap[
    (String, Boolean, Boolean, Seq[JDIEventArgument]),
    AtomicInteger
  ]().asScala

  /**
   * Constructs a stream of exception events for all exceptions.
   *
   * @param notifyCaught If true, exception events will be streamed when an
   *                     exception is caught in a try/catch block
   * @param notifyUncaught If true, exception events will be streamed when an
   *                       exception is not caught in a try/catch block
   * @param extraArguments The additional JDI arguments to provide
   *
   * @return The stream of exception events and any retrieved data based on
   *         requests from extra arguments
   */
  override def onAllExceptionsWithData(
    notifyCaught: Boolean,
    notifyUncaught: Boolean,
    extraArguments: JDIArgument*
  ): Try[IdentityPipeline[ExceptionEventAndData]] = Try {
    val JDIArgumentGroup(rArgs, eArgs, _) = JDIArgumentGroup(extraArguments: _*)
    val requestId = newCatchallExceptionRequest((
      notifyCaught,
      notifyUncaught,
      rArgs
    ))
    newExceptionPipeline(
      requestId,
      (null, notifyCaught, notifyUncaught, eArgs)
    )
  }

  /**
   * Constructs a stream of exception events for the specified exception.
   *
   * @param exceptionName The full class name of the exception
   * @param notifyCaught If true, exception events will be streamed when the
   *                     exception is caught in a try/catch block
   * @param notifyUncaught If true, exception events will be streamed when the
   *                       exception is not caught in a try/catch block
   * @param extraArguments The additional JDI arguments to provide
   *
   * @return The stream of exception events and any retrieved data based on
   *         requests from extra arguments
   */
  override def onExceptionWithData(
    exceptionName: String,
    notifyCaught: Boolean,
    notifyUncaught: Boolean,
    extraArguments: JDIArgument*
  ): Try[IdentityPipeline[ExceptionEventAndData]] = Try {
    require(exceptionName != null, "Exception name cannot be null!")
    val JDIArgumentGroup(rArgs, eArgs, _) = JDIArgumentGroup(extraArguments: _*)
    val requestId = newExceptionRequest((
      exceptionName,
      notifyCaught,
      notifyUncaught,
      rArgs
    ))
    newExceptionPipeline(
      requestId,
      (exceptionName, notifyCaught, notifyUncaught, eArgs)
    )
  }

  /**
   * Creates a new exception request using the given arguments. The request is
   * memoized, meaning that the same request will be returned for the same
   * arguments. The memoized result will be thrown out if the underlying
   * request storage indicates that the request has been removed.
   *
   * @return The id of the created exception request
   */
  protected val newExceptionRequest = {
    type Input = (String, Boolean, Boolean, Seq[JDIRequestArgument])
    type Key = (String, Boolean, Boolean, Seq[JDIRequestArgument])
    type Output = String

    new Memoization[Input, Key, Output](
      memoFunc = (input: Input) => {
        val requestId = newExceptionRequestId()
        val args = UniqueIdProperty(id = requestId) +: input._4

        exceptionManager.createExceptionRequestWithId(
          requestId,
          input._1,
          input._2,
          input._3,
          args: _*
        ).get

        requestId
      },
      cacheInvalidFunc = (key: Key) => {
        !exceptionManager.hasExceptionRequest(key._1)
      }
    )
  }

  /**
   * Creates a new catchall exception request using the given arguments. The
   * request is memoized, meaning that the same request will be returned for
   * the same arguments. The memoized result will be thrown out if the
   * underlying request storage indicates that the request has been removed.
   *
   * @return The id of the created exception request
   */
  protected val newCatchallExceptionRequest = {
    type Input = (Boolean, Boolean, Seq[JDIRequestArgument])
    type Key = (Boolean, Boolean, Seq[JDIRequestArgument])
    type Output = String

    new Memoization[Input, Key, Output](
      memoFunc = (input: Input) => {
        val requestId = newExceptionRequestId()
        val args = UniqueIdProperty(id = requestId) +: input._3

        exceptionManager.createCatchallExceptionRequestWithId(
          requestId,
          input._1,
          input._2,
          args: _*
        ).get

        requestId
      },
      cacheInvalidFunc = (key: Key) => {
        !exceptionManager.hasCatchallExceptionRequest
      }
    )
  }

  /**
   * Creates a new pipeline of exception events and data using the given
   * arguments. The pipeline is NOT memoized; therefore, each call creates a
   * new pipeline with a new underlying event handler feeding the pipeline.
   * This means that the pipeline needs to be properly closed to remove the
   * event handler.
   *
   * @param requestId The id of the request whose events to stream through the
   *                  new pipeline
   * @param args The additional event arguments to provide to the event handler
   *             feeding the new pipeline
   * @return The new exception event and data pipeline
   */
  protected def newExceptionPipeline(
    requestId: String,
    args: (String, Boolean, Boolean, Seq[JDIEventArgument])
  ): IdentityPipeline[ExceptionEventAndData] = {
    val eArgsWithFilter = UniqueIdPropertyFilter(id = requestId) +: args._4
    val newPipeline = eventManager
      .addEventDataStream(ExceptionEventType, eArgsWithFilter: _*)
      .map(t => (t._1.asInstanceOf[ExceptionEvent], t._2))
      .noop()

    // Create a companion pipeline who, when closed, checks to see if there
    // are no more pipelines for the given request and, if so, removes the
    // request as well
    val closePipeline = Pipeline.newPipeline(
      classOf[ExceptionEventAndData],
      newExceptionPipelineCloseFunc(requestId, args)
    )

    // Increment the counter for open pipelines
    pipelineCounter
      .getOrElseUpdate(args, new AtomicInteger(0))
      .incrementAndGet()

    val combinedPipeline = newPipeline.unionOutput(closePipeline)
    combinedPipeline
  }

  /**
   * Creates a new function used for closing generated pipelines.
   *
   * @param requestId The id of the request
   * @param args The arguments associated with the request
   *
   * @return The new function for closing the pipeline
   */
  protected def newExceptionPipelineCloseFunc(
    requestId: String,
    args: (String, Boolean, Boolean, Seq[JDIEventArgument])
  ): () => Unit = () => {
    val pCounter = pipelineCounter(args)

    val totalPipelinesRemaining = pCounter.decrementAndGet()

    if (totalPipelinesRemaining == 0) {
      exceptionManager.removeExceptionRequestWithId(requestId)
    }
  }

  /**
   * Used to generate new request ids to capture request/event matches.
   *
   * @return The new id as a string
   */
  protected def newExceptionRequestId(): String =
    java.util.UUID.randomUUID().toString
}
