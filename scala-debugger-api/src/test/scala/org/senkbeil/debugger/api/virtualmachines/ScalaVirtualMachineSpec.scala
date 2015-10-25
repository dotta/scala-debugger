package org.senkbeil.debugger.api.virtualmachines

import org.senkbeil.debugger.api.lowlevel.ManagerContainer
import org.senkbeil.debugger.api.lowlevel.classes.ClassManager
import com.sun.jdi._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers, OneInstancePerTest}
import org.senkbeil.debugger.api.lowlevel.events.EventManager
import org.senkbeil.debugger.api.utils.LoopingTaskRunner
import test.JDIMockHelpers

import org.senkbeil.debugger.api.lowlevel

class ScalaVirtualMachineSpec extends FunSpec with Matchers
  with OneInstancePerTest with MockFactory with JDIMockHelpers
{
  private val mockVirtualMachine = mock[VirtualMachine]

  // NOTE: Needed until https://github.com/paulbutcher/ScalaMock/issues/56
  private class ZeroArgClassManager
    extends ClassManager(mockVirtualMachine, loadClasses = false)
  private val mockClassManager = mock[ZeroArgClassManager]

  // NOTE: Needed until https://github.com/paulbutcher/ScalaMock/issues/56
  private class ZeroArgEventManager extends EventManager(
    mockVirtualMachine,
    stub[LoopingTaskRunner],
    autoStart = false // Set to false to avoid calling extra code
  )
  private val stubEventManager = stub[ZeroArgEventManager]

  private class TestManagerContainer extends lowlevel.ManagerContainer(
    breakpointManager   = null,
    classManager        = mockClassManager,
    eventManager        = stubEventManager,
    exceptionManager    = null,
    methodEntryManager  = null,
    methodExitManager   = null,
    requestManager      = null,
    stepManager         = null
  )

  private class TestScalaVirtualMachine extends ScalaVirtualMachine(
    mockVirtualMachine
  ) {
    override protected def newManagerContainer(
      loopingTaskRunner: LoopingTaskRunner
    ): ManagerContainer = new TestManagerContainer

    override protected def initializeEvents(): Unit = {}
  }

  private val scalaVirtualMachine = new TestScalaVirtualMachine

  describe("ScalaVirtualMachine") {
    describe("#availableLinesForFile") {
      it("should return the lines (sorted) that can have breakpoints") {
        val expected = Seq(1, 8, 999)

        // Setup the return from class manager to be reverse order
        val linesAndLocations = expected.reverseMap(i =>
          (i, Seq(stub[Location]))
        ).toMap
        (mockClassManager.linesAndLocationsForFile _).expects(*)
          .returning(Some(linesAndLocations))

        val actual = scalaVirtualMachine.availableLinesForFile("").get

        actual should contain theSameElementsInOrderAs expected
      }

      it("should return None if the file does not exist") {
        val expected = None

        // Set the return from class manager to be "not found"
        (mockClassManager.linesAndLocationsForFile _).expects(*).returning(None)

        val actual = scalaVirtualMachine.availableLinesForFile("")

        actual should be (expected)
      }
    }
  }
}
