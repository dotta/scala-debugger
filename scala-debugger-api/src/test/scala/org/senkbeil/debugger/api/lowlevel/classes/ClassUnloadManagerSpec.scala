package org.senkbeil.debugger.api.lowlevel.classes

import java.util.concurrent.atomic.AtomicInteger

import com.sun.jdi.request.{EventRequest, EventRequestManager, ClassUnloadRequest}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers, OneInstancePerTest}
import org.senkbeil.debugger.api.lowlevel.requests.{JDIRequestArgument, JDIRequestProcessor}

import scala.util.{Failure, Success}

class ClassUnloadManagerSpec extends FunSpec with Matchers with MockFactory
  with OneInstancePerTest with org.scalamock.matchers.Matchers
{
  private val TestId = java.util.UUID.randomUUID().toString
  private val mockEventRequestManager = mock[EventRequestManager]

  private val classUnloadManager = new ClassUnloadManager(mockEventRequestManager) {
    override protected def newRequestId(): String = TestId
  }

  describe("ClassUnloadManager") {
    describe("#classUnloadRequestList") {
      it("should contain all class unload requests in the form of id -> request stored in the manager") {
        val classUnloadRequests = Seq(
          java.util.UUID.randomUUID().toString,
          java.util.UUID.randomUUID().toString
        )

        // Create a ClassUnloadManager whose generated request ids match the list
        // above
        val classUnloadManager = new ClassUnloadManager(mockEventRequestManager) {
          private val counter = new AtomicInteger(0)
          override protected def newRequestId(): String = {
            classUnloadRequests(counter.getAndIncrement % classUnloadRequests.length)
          }
        }

        classUnloadRequests.foreach { case id =>
          (mockEventRequestManager.createClassUnloadRequest _).expects()
            .returning(stub[ClassUnloadRequest]).once()
          classUnloadManager.createClassUnloadRequest()
        }

        classUnloadManager.classUnloadRequestList should
          contain theSameElementsAs (classUnloadRequests)
      }
    }

    describe("#createClassUnloadRequest") {
      it("should create the class unload request and return Success(id)") {
        val expected = Success(TestId)

        val mockClassUnloadRequest = mock[ClassUnloadRequest]
        (mockEventRequestManager.createClassUnloadRequest _).expects()
          .returning(mockClassUnloadRequest).once()

        // Should set enabled to true by default, and
        // set the suspend policy to vm level by default
        (mockClassUnloadRequest.setSuspendPolicy _)
          .expects(EventRequest.SUSPEND_EVENT_THREAD).once()
        (mockClassUnloadRequest.setEnabled _).expects(true).once()

        val actual = classUnloadManager.createClassUnloadRequest()
        actual should be (expected)
      }

      it("should return the exception if unable to create the request") {
        val expected = Failure(new Throwable)

        (mockEventRequestManager.createClassUnloadRequest _).expects()
          .throwing(expected.failed.get).once()

        val actual = classUnloadManager.createClassUnloadRequest()
        actual should be (expected)
      }
    }

    describe("#hasClassUnloadRequest") {
      it("should return true if it exists") {
        val expected = true

        (mockEventRequestManager.createClassUnloadRequest _).expects()
          .returning(stub[ClassUnloadRequest]).once()

        val id = classUnloadManager.createClassUnloadRequest().get

        val actual = classUnloadManager.hasClassUnloadRequest(id)
        actual should be (expected)
      }

      it("should return false if it does not exist") {
        val expected = false

        val actual = classUnloadManager.hasClassUnloadRequest(TestId)
        actual should be (expected)
      }
    }

    describe("#getClassUnloadRequest") {
      it("should return Some(ClassUnloadRequest) if found") {
        val expected = stub[ClassUnloadRequest]

        (mockEventRequestManager.createClassUnloadRequest _).expects()
          .returning(expected).once()

        val id = classUnloadManager.createClassUnloadRequest().get

        val actual = classUnloadManager.getClassUnloadRequest(id)
        actual should be (Some(expected))
      }

      it("should return None if not found") {
        val expected = None

        val actual = classUnloadManager.getClassUnloadRequest(TestId)
        actual should be (expected)
      }
    }

    describe("#getClassUnloadArguments") {
      it("should return Some(Seq(input args)) if found") {
        val expected = Seq(mock[JDIRequestArgument], mock[JDIRequestArgument])
        expected.foreach(a => {
          val mockRequestProcessor = mock[JDIRequestProcessor]
          (mockRequestProcessor.process _).expects(*)
            .onCall((er: EventRequest) => er).once()
          (a.toProcessor _).expects().returning(mockRequestProcessor).once()
        })

        (mockEventRequestManager.createClassUnloadRequest _).expects()
          .returning(stub[ClassUnloadRequest]).once()

        val id = classUnloadManager.createClassUnloadRequest(expected: _*).get

        val actual = classUnloadManager.getClassUnloadRequestArguments(id)
        actual should be (Some(expected))
      }

      it("should return None if not found") {
        val expected = None

        val actual = classUnloadManager.getClassUnloadRequestArguments(TestId)
        actual should be (expected)
      }
    }

    describe("#removeClassUnloadRequest") {
      it("should return true if the class unload request was removed") {
        val expected = true
        val stubRequest = stub[ClassUnloadRequest]

        (mockEventRequestManager.createClassUnloadRequest _).expects()
          .returning(stubRequest).once()

        val id = classUnloadManager.createClassUnloadRequest().get

        (mockEventRequestManager.deleteEventRequest _)
          .expects(stubRequest).once()

        val actual = classUnloadManager.removeClassUnloadRequest(id)
        actual should be (expected)
      }

      it("should return false if the class unload request was not removed") {
        val expected = false

        val actual = classUnloadManager.removeClassUnloadRequest(TestId)
        actual should be (expected)
      }
    }
  }
}