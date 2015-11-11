package org.senkbeil.debugger.api.profiles.pure.breakpoints

import com.sun.jdi.VirtualMachine
import com.sun.jdi.event.{Event, EventQueue}
import com.sun.jdi.request.EventRequestManager
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers, OneInstancePerTest}
import org.senkbeil.debugger.api.lowlevel.breakpoints.BreakpointManager
import org.senkbeil.debugger.api.lowlevel.classes.ClassManager
import org.senkbeil.debugger.api.lowlevel.events.EventManager
import org.senkbeil.debugger.api.lowlevel.events.data.JDIEventDataResult
import org.senkbeil.debugger.api.lowlevel.events.filters.UniqueIdPropertyFilter
import org.senkbeil.debugger.api.lowlevel.requests.JDIRequestArgument
import org.senkbeil.debugger.api.lowlevel.requests.properties.UniqueIdProperty
import org.senkbeil.debugger.api.pipelines.Pipeline
import org.senkbeil.debugger.api.utils.LoopingTaskRunner
import test.JDIMockHelpers
import scala.util.{Failure, Success}
import org.senkbeil.debugger.api.lowlevel.events.EventType.BreakpointEventType

class PureBreakpointProfileSpec extends FunSpec with Matchers
  with OneInstancePerTest with MockFactory with JDIMockHelpers
{
  private val TestRequestId = java.util.UUID.randomUUID().toString

  // Workaround - see https://github.com/paulbutcher/ScalaMock/issues/33
  private class ZeroArgClassManager extends ClassManager(
    stub[VirtualMachine], loadClasses = false
  )
  private val stubClassManager = stub[ZeroArgClassManager]

  // Workaround - see https://github.com/paulbutcher/ScalaMock/issues/33
  private class ZeroArgBreakpointManager extends BreakpointManager(
    stub[EventRequestManager],
    stubClassManager
  )
  private val mockBreakpointManager = mock[ZeroArgBreakpointManager]

  // Workaround - see https://github.com/paulbutcher/ScalaMock/issues/33
  private class ZeroArgEventManager extends EventManager(
    stub[EventQueue],
    stub[LoopingTaskRunner],
    autoStart = false
  )
  private val mockEventManager = mock[ZeroArgEventManager]

  private val pureBreakpointProfile = new Object with PureBreakpointProfile {
    private var requestId: String = _
    def setRequestId(requestId: String): Unit = this.requestId = requestId

    // NOTE: If we set a specific request id, return that, otherwise use the
    //       default behavior
    override protected def newBreakpointRequestId(): String =
      if (requestId != null) requestId else super.newBreakpointRequestId()

    override protected val breakpointManager = mockBreakpointManager
    override protected val eventManager: EventManager = mockEventManager
  }

  describe("PureBreakpointProfile") {
    describe("#onBreakpointWithData") {
      it("should create a new request if one has not be made yet") {
        val fileName = "some file"
        val lineNumber = 999
        val arguments = Seq(mock[JDIRequestArgument])

        val uniqueIdProperty = UniqueIdProperty(id = TestRequestId)
        val uniqueIdPropertyFilter = UniqueIdPropertyFilter(id = TestRequestId)

        // Set a known test id so we can validate the unique property is added
        import scala.language.reflectiveCalls
        pureBreakpointProfile.setRequestId(TestRequestId)

        inSequence {
          // Memoized request function first checks to make sure the cache
          // has not been invalidated underneath (first call will always be
          // false since we have never created the request)
          (mockBreakpointManager.hasLineBreakpointRequest _)
            .expects(fileName, lineNumber)
            .returning(false).once()

          // NOTE: Expect the request to be created with a unique id
          (mockBreakpointManager.createLineBreakpointRequest _)
            .expects(fileName, lineNumber, uniqueIdProperty +: arguments)
            .returning(Success(true)).once()

          (mockEventManager.addEventDataStream _)
            .expects(BreakpointEventType, Seq(uniqueIdPropertyFilter))
            .returning(Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            )).once()
        }

        pureBreakpointProfile.onBreakpointWithData(
          fileName,
          lineNumber,
          arguments: _*
        )
      }

      it("should capture exceptions thrown when creating the request") {
        val expected = Failure(new Throwable)
        val fileName = "some file"
        val lineNumber = 999
        val arguments = Seq(mock[JDIRequestArgument])

        val uniqueIdProperty = UniqueIdProperty(id = TestRequestId)

        // Set a known test id so we can validate the unique property is added
        import scala.language.reflectiveCalls
        pureBreakpointProfile.setRequestId(TestRequestId)

        inSequence {
          // Memoized request function first checks to make sure the cache
          // has not been invalidated underneath (first call will always be
          // false since we have never created the request)
          (mockBreakpointManager.hasLineBreakpointRequest _)
            .expects(fileName, lineNumber)
            .returning(false).once()

          // NOTE: Expect the request to be created with a unique id
          (mockBreakpointManager.createLineBreakpointRequest _)
            .expects(fileName, lineNumber, uniqueIdProperty +: arguments)
            .throwing(expected.failed.get).once()
        }

        val actual = pureBreakpointProfile.onBreakpointWithData(
          fileName,
          lineNumber,
          arguments: _*
        )

        actual should be (expected)
      }

      it("should create a new request if the previous one was removed") {
        val fileName = "some file"
        val lineNumber = 999
        val arguments = Seq(mock[JDIRequestArgument])

        // Set a known test id so we can validate the unique property is added
        import scala.language.reflectiveCalls
        pureBreakpointProfile.setRequestId(TestRequestId)

        inSequence {
          // Set a known test id so we can validate the unique property is added
          import scala.language.reflectiveCalls
          pureBreakpointProfile.setRequestId(TestRequestId)

          val uniqueIdProperty = UniqueIdProperty(id = TestRequestId)
          val uniqueIdPropertyFilter =
            UniqueIdPropertyFilter(id = TestRequestId)

          // Memoized request function first checks to make sure the cache
          // has not been invalidated underneath (first call will always be
          // false since we have never created the request)
          (mockBreakpointManager.hasLineBreakpointRequest _)
            .expects(fileName, lineNumber)
            .returning(false).once()

          // NOTE: Expect the request to be created with a unique id
          (mockBreakpointManager.createLineBreakpointRequest _)
            .expects(fileName, lineNumber, uniqueIdProperty +: arguments)
            .returning(Success(true)).once()

          (mockEventManager.addEventDataStream _)
            .expects(BreakpointEventType, Seq(uniqueIdPropertyFilter))
            .returning(Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            )).once()
        }

        pureBreakpointProfile.onBreakpointWithData(
          fileName,
          lineNumber,
          arguments: _*
        )

        inSequence {
          // Set a known test id so we can validate the unique property is added
          import scala.language.reflectiveCalls
          pureBreakpointProfile.setRequestId(TestRequestId + "other")

          val uniqueIdProperty = UniqueIdProperty(id = TestRequestId + "other")
          val uniqueIdPropertyFilter =
            UniqueIdPropertyFilter(id = TestRequestId + "other")

          // Return false this time to indicate that the breakpoint request
          // was removed some time between the two calls
          (mockBreakpointManager.hasLineBreakpointRequest _)
            .expects(fileName, lineNumber)
            .returning(false).once()

          // NOTE: Expect the request to be created with a unique id
          (mockBreakpointManager.createLineBreakpointRequest _)
            .expects(fileName, lineNumber, uniqueIdProperty +: arguments)
            .returning(Success(true)).once()

          (mockEventManager.addEventDataStream _)
            .expects(BreakpointEventType, Seq(uniqueIdPropertyFilter))
            .returning(Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            )).once()
        }

        pureBreakpointProfile.onBreakpointWithData(
          fileName,
          lineNumber,
          arguments: _*
        )
      }

      it("should not create a new request if the previous one still exists") {
        val fileName = "some file"
        val lineNumber = 999
        val arguments = Seq(mock[JDIRequestArgument])

        inSequence {
          // Set a known test id so we can validate the unique property is added
          import scala.language.reflectiveCalls
          pureBreakpointProfile.setRequestId(TestRequestId)

          val uniqueIdProperty = UniqueIdProperty(id = TestRequestId)
          val uniqueIdPropertyFilter =
            UniqueIdPropertyFilter(id = TestRequestId)

          // Memoized request function first checks to make sure the cache
          // has not been invalidated underneath (first call will always be
          // false since we have never created the request)
          (mockBreakpointManager.hasLineBreakpointRequest _)
            .expects(fileName, lineNumber)
            .returning(false).once()

          // NOTE: Expect the request to be created with a unique id
          (mockBreakpointManager.createLineBreakpointRequest _)
            .expects(fileName, lineNumber, uniqueIdProperty +: arguments)
            .returning(Success(true)).once()

          (mockEventManager.addEventDataStream _)
            .expects(BreakpointEventType, Seq(uniqueIdPropertyFilter))
            .returning(Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            )).once()
        }

        pureBreakpointProfile.onBreakpointWithData(
          fileName,
          lineNumber,
          arguments: _*
        )

        inSequence {
          // Set a known test id so we can validate the unique property is added
          import scala.language.reflectiveCalls
          pureBreakpointProfile.setRequestId(TestRequestId + "other")

          val uniqueIdProperty = UniqueIdProperty(id = TestRequestId)
          val uniqueIdPropertyFilter =
            UniqueIdPropertyFilter(id = TestRequestId)

          // Return true to indicate that we do still have the request
          (mockBreakpointManager.hasLineBreakpointRequest _)
            .expects(fileName, lineNumber)
            .returning(true).once()

          (mockEventManager.addEventDataStream _)
            .expects(BreakpointEventType, Seq(uniqueIdPropertyFilter))
            .returning(Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            )).once()
        }

        pureBreakpointProfile.onBreakpointWithData(
          fileName,
          lineNumber,
          arguments: _*
        )
      }

      it("should create a new request for different input") {
        val fileName = "some file"
        val lineNumber = 999
        val arguments = Seq(mock[JDIRequestArgument])

        // Set a known test id so we can validate the unique property is added
        import scala.language.reflectiveCalls
        pureBreakpointProfile.setRequestId(TestRequestId)

        inSequence {
          // Set a known test id so we can validate the unique property is added
          import scala.language.reflectiveCalls
          pureBreakpointProfile.setRequestId(TestRequestId)

          val uniqueIdProperty = UniqueIdProperty(id = TestRequestId)
          val uniqueIdPropertyFilter =
            UniqueIdPropertyFilter(id = TestRequestId)

          // Memoized request function first checks to make sure the cache
          // has not been invalidated underneath (first call will always be
          // false since we have never created the request)
          (mockBreakpointManager.hasLineBreakpointRequest _)
            .expects(fileName, lineNumber)
            .returning(false).once()

          // NOTE: Expect the request to be created with a unique id
          (mockBreakpointManager.createLineBreakpointRequest _)
            .expects(fileName, lineNumber, uniqueIdProperty +: arguments)
            .returning(Success(true)).once()

          (mockEventManager.addEventDataStream _)
            .expects(BreakpointEventType, Seq(uniqueIdPropertyFilter))
            .returning(Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            )).once()
        }

        pureBreakpointProfile.onBreakpointWithData(
          fileName,
          lineNumber,
          arguments: _*
        )

        inSequence {
          // Set a known test id so we can validate the unique property is added
          import scala.language.reflectiveCalls
          pureBreakpointProfile.setRequestId(TestRequestId + "other")

          val uniqueIdProperty = UniqueIdProperty(id = TestRequestId + "other")
          val uniqueIdPropertyFilter =
            UniqueIdPropertyFilter(id = TestRequestId + "other")

          // Memoized request function first checks to make sure the cache
          // has not been invalidated underneath (first call will always be
          // false since we have never created the request)
          (mockBreakpointManager.hasLineBreakpointRequest _)
            .expects(fileName, lineNumber + 1)
            .returning(false).once()

          // NOTE: Expect the request to be created with a unique id
          (mockBreakpointManager.createLineBreakpointRequest _)
            .expects(fileName, lineNumber + 1, uniqueIdProperty +: arguments)
            .returning(Success(true)).once()

          (mockEventManager.addEventDataStream _)
            .expects(BreakpointEventType, Seq(uniqueIdPropertyFilter))
            .returning(Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            )).once()
        }

        pureBreakpointProfile.onBreakpointWithData(
          fileName,
          lineNumber + 1,
          arguments: _*
        )
      }
    }
  }
}