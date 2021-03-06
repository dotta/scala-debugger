package org.scaladebugger.api.profiles.java.info.events

import com.sun.jdi.event._
import com.sun.jdi.{ReferenceType, ThreadReference, VirtualMachine}
import org.scaladebugger.api.lowlevel.JDIArgument
import org.scaladebugger.api.lowlevel.events.JDIEventArgument
import org.scaladebugger.api.lowlevel.requests.JDIRequestArgument
import org.scaladebugger.api.profiles.traits.info.events._
import org.scaladebugger.api.profiles.traits.info.{InfoProducer, ReferenceTypeInfo, ThreadInfo}
import org.scaladebugger.api.virtualmachines.ScalaVirtualMachine
import org.scaladebugger.test.helpers.ParallelMockFunSpec

class JavaClassUnloadEventInfoSpec extends ParallelMockFunSpec {
  private val mockScalaVirtualMachine = mock[ScalaVirtualMachine]
  private val mockInfoProducer = mock[InfoProducer]
  private val mockClassUnloadEvent = mock[ClassUnloadEvent]

  private val mockJdiRequestArguments = Seq(mock[JDIRequestArgument])
  private val mockJdiEventArguments = Seq(mock[JDIEventArgument])
  private val mockJdiArguments =
    mockJdiRequestArguments ++ mockJdiEventArguments

  private val javaClassUnloadEventInfoProfile = new JavaClassUnloadEventInfo(
    scalaVirtualMachine = mockScalaVirtualMachine,
    infoProducer = mockInfoProducer,
    classUnloadEvent = mockClassUnloadEvent,
    jdiArguments = mockJdiArguments
  )

  describe("JavaClassUnloadEventInfo") {
    describe("#toJavaInfo") {
      it("should return a new instance of the Java profile representation") {
        val expected = mock[ClassUnloadEventInfo]

        // Event info producer will be generated in its Java form
        val mockEventInfoProducer = mock[EventInfoProducer]
        (mockInfoProducer.eventProducer _).expects()
          .returning(mockEventInfoProducer).once()
        (mockEventInfoProducer.toJavaInfo _).expects()
          .returning(mockEventInfoProducer).once()

        // Java version of event info producer creates a new event instance
        // NOTE: Cannot validate second set of args because they are
        //       call-by-name, which ScalaMock does not support presently
        (mockEventInfoProducer.newClassUnloadEventInfo _).expects(
          mockScalaVirtualMachine,
          mockClassUnloadEvent,
          mockJdiArguments
        ).returning(expected).once()

        val actual = javaClassUnloadEventInfoProfile.toJavaInfo

        actual should be (expected)
      }
    }

    describe("#isJavaInfo") {
      it("should return true") {
        val expected = true

        val actual = javaClassUnloadEventInfoProfile.isJavaInfo

        actual should be (expected)
      }
    }

    describe("#toJdiInstance") {
      it("should return the JDI instance this profile instance represents") {
        val expected = mockClassUnloadEvent

        val actual = javaClassUnloadEventInfoProfile.toJdiInstance

        actual should be (expected)
      }
    }

    describe("#className") {
      it("should return the event's class name") {
        val expected = "some.class.name"

        (mockClassUnloadEvent.className _).expects()
          .returning(expected).once()

        val actual = javaClassUnloadEventInfoProfile.className

        actual should be (expected)
      }
    }

    describe("#classSignature") {
      it("should return the event's class signature") {
        val expected = "class.signature];"

        (mockClassUnloadEvent.classSignature _).expects()
          .returning(expected).once()

        val actual = javaClassUnloadEventInfoProfile.classSignature

        actual should be (expected)
      }
    }
  }
}
