package org.senkbeil.debugger.api.lowlevel

import com.sun.jdi.VirtualMachine
import com.sun.jdi.request.EventRequestManager
import org.senkbeil.debugger.api.lowlevel.breakpoints._
import org.senkbeil.debugger.api.lowlevel.classes._
import org.senkbeil.debugger.api.lowlevel.events._
import org.senkbeil.debugger.api.lowlevel.exceptions._
import org.senkbeil.debugger.api.lowlevel.methods._
import org.senkbeil.debugger.api.lowlevel.monitors._
import org.senkbeil.debugger.api.lowlevel.steps._
import org.senkbeil.debugger.api.lowlevel.threads._
import org.senkbeil.debugger.api.lowlevel.vm._
import org.senkbeil.debugger.api.lowlevel.watchpoints._
import org.senkbeil.debugger.api.utils.LoopingTaskRunner

/**
 * Represents a container for low-level managers.
 */
case class ManagerContainer(
  accessWatchpointManager: AccessWatchpointManager,
  breakpointManager: BreakpointManager,
  classManager: ClassManager,
  classPrepareManager: ClassPrepareManager,
  classUnloadManager: ClassUnloadManager,
  eventManager: EventManager,
  exceptionManager: ExceptionManager,
  methodEntryManager: MethodEntryManager,
  methodExitManager: MethodExitManager,
  modificationWatchpointManager: ModificationWatchpointManager,
  monitorContendedEnteredManager: MonitorContendedEnteredManager,
  monitorContendedEnterManager: MonitorContendedEnterManager,
  monitorWaitedManager: MonitorWaitedManager,
  monitorWaitManager: MonitorWaitManager,
  requestManager: EventRequestManager,
  stepManager: StepManager,
  threadDeathManager: ThreadDeathManager,
  threadStartManager: ThreadStartManager,
  vmDeathManager: VMDeathManager
) {
  /**
   * Processes any pending requests in the managers of the provided manager
   * container by applying them to the managers in this manager container.
   *
   * @note This will not remove the pending requests from the managers
   *       contained in the provided manager container!
   *
   * @param managerContainer The manager container whose managers with pending
   *                         requests will have their requests processed in
   *                         this manager container
   */
  def processPendingRequests(
    managerContainer: ManagerContainer
  ): Unit = managerContainer.productIterator.foreach {
    case accessWatchpointManager: PendingAccessWatchpointSupport =>
      accessWatchpointManager.pendingAccessWatchpointRequests.foreach(i =>
        managerContainer.accessWatchpointManager.createAccessWatchpointRequestWithId(
          i.requestId,
          i.className,
          i.fieldName,
          i.extraArguments: _*
        )
      )
    case breakpointManager: PendingBreakpointSupport =>
      breakpointManager.pendingBreakpointRequests.foreach(i =>
        managerContainer.breakpointManager.createBreakpointRequestWithId(
          i.requestId,
          i.fileName,
          i.lineNumber,
          i.extraArguments: _*
        )
      )
    case classPrepareManager: PendingClassPrepareSupport =>
      classPrepareManager.pendingClassPrepareRequests.foreach(i =>
        managerContainer.classPrepareManager.createClassPrepareRequestWithId(
          i.requestId,
          i.extraArguments: _*
        )
      )
    case classUnloadManager: PendingClassUnloadSupport =>
      classUnloadManager.pendingClassUnloadRequests.foreach(i =>
        managerContainer.classUnloadManager.createClassUnloadRequestWithId(
          i.requestId,
          i.extraArguments: _*
        )
      )
    case eventManager: PendingEventHandlerSupport =>
      eventManager.pendingEventHandlers.foreach(i =>
        managerContainer.eventManager.addEventHandlerWithId(
          i.eventHandlerId,
          i.eventType,
          i.eventHandler,
          i.extraArguments: _*
        )
      )
    case exceptionManager: PendingExceptionSupport =>
      exceptionManager.pendingExceptionRequests.filter(_.className == null).foreach(i =>
        managerContainer.exceptionManager.createCatchallExceptionRequestWithId(
          i.requestId,
          i.notifyCaught,
          i.notifyUncaught,
          i.extraArguments: _*
        )
      )
      exceptionManager.pendingExceptionRequests.filter(_.className != null).foreach(i =>
        managerContainer.exceptionManager.createExceptionRequestWithId(
          i.requestId,
          i.className,
          i.notifyCaught,
          i.notifyUncaught,
          i.extraArguments: _*
        )
      )
    case methodEntryManager: PendingMethodEntrySupport =>
      methodEntryManager.pendingMethodEntryRequests.foreach(i =>
        managerContainer.methodEntryManager.createMethodEntryRequestWithId(
          i.requestId,
          i.className,
          i.methodName,
          i.extraArguments: _*
        )
      )
    case methodExitManager: PendingMethodExitSupport =>
      methodExitManager.pendingMethodExitRequests.foreach(i =>
        managerContainer.methodExitManager.createMethodExitRequestWithId(
          i.requestId,
          i.className,
          i.methodName,
          i.extraArguments: _*
        )
      )
    case modificationWatchpointManager: PendingModificationWatchpointSupport =>
      modificationWatchpointManager.pendingModificationWatchpointRequests.foreach(i =>
        managerContainer.modificationWatchpointManager.createModificationWatchpointRequestWithId(
          i.requestId,
          i.className,
          i.fieldName,
          i.extraArguments: _*
        )
      )
    case monitorContendedEnteredManager: PendingMonitorContendedEnteredSupport =>
      monitorContendedEnteredManager.pendingMonitorContendedEnteredRequests.foreach(i =>
        managerContainer.monitorContendedEnteredManager.createMonitorContendedEnteredRequestWithId(
          i.requestId,
          i.extraArguments: _*
        )
      )
    case monitorContendedEnterManager: PendingMonitorContendedEnterSupport =>
      monitorContendedEnterManager.pendingMonitorContendedEnterRequests.foreach(i =>
        managerContainer.monitorContendedEnterManager.createMonitorContendedEnterRequestWithId(
          i.requestId,
          i.extraArguments: _*
        )
      )
    case monitorWaitedManager: PendingMonitorWaitedSupport =>
      monitorWaitedManager.pendingMonitorWaitedRequests.foreach(i =>
        managerContainer.monitorWaitedManager.createMonitorWaitedRequestWithId(
          i.requestId,
          i.extraArguments: _*
        )
      )
    case monitorWaitManager: PendingMonitorWaitSupport =>
      monitorWaitManager.pendingMonitorWaitRequests.foreach(i =>
        managerContainer.monitorWaitManager.createMonitorWaitRequestWithId(
          i.requestId,
          i.extraArguments: _*
        )
      )
    case stepManager: PendingStepSupport =>
      stepManager.pendingStepRequests.foreach(i =>
        managerContainer.stepManager.createStepRequestWithId(
          i.requestId,
          i.threadReference,
          i.size,
          i.depth,
          i.extraArguments: _*
        )
      )
    case threadDeathManager: PendingThreadDeathSupport =>
      threadDeathManager.pendingThreadDeathRequests.foreach(i =>
        managerContainer.threadDeathManager.createThreadDeathRequestWithId(
          i.requestId,
          i.extraArguments: _*
        )
      )
    case threadStartManager: PendingThreadStartSupport =>
      threadStartManager.pendingThreadStartRequests.foreach(i =>
        managerContainer.threadStartManager.createThreadStartRequestWithId(
          i.requestId,
          i.extraArguments: _*
        )
      )
    case vmDeathManager: PendingVMDeathSupport =>
      vmDeathManager.pendingVMDeathRequests.foreach(i =>
        managerContainer.vmDeathManager.createVMDeathRequestWithId(
          i.requestId,
          i.extraArguments: _*
        )
      )
  }

  /** Enables pending support for all managers supporting pending requests. */
  def enablePendingSupport(): Unit = setPendingSupportForAll(true)

  /** Disables pending support for all managers supporting pending requests. */
  def disablePendingSupport(): Unit = setPendingSupportForAll(false)

  /**
   * Sets the pending support enablement to the specified value.
   *
   * @param value True if enabling pending support on managers, otherwise false
   */
  private def setPendingSupportForAll(value: Boolean): Unit = {
    this.productIterator.foreach {
      case p: PendingRequestSupport => p.setPendingSupport(value)
      case _                        => // Do nothing in this case
    }
  }
}

/** Containers helper methods for initializing a manager container. */
object ManagerContainer {
  /**
   * Initializes all managers for the specified virtual machine. Uses the
   * default instance of the looping task runner for created managers.
   * Automatically starts the event manager.
   *
   * @param virtualMachine The virtual machine whose managers to initialize
   *
   * @return The container holding all of the new managers
   */
  def fromVirtualMachine(virtualMachine: VirtualMachine): ManagerContainer = {
    val loopingTaskRunner = new LoopingTaskRunner()
    fromVirtualMachine(
      virtualMachine,
      loopingTaskRunner,
      autoStartEventManager = true
    )
  }

  /**
   * Initializes all managers for the specified virtual machine.
   *
   * @param virtualMachine The virtual machine whose managers to initialize
   * @param loopingTaskRunner The task runner to use with various managers
   *
   * @return The container holding all of the new managers
   */
  def fromVirtualMachine(
    virtualMachine: VirtualMachine,
    loopingTaskRunner: LoopingTaskRunner,
    autoStartEventManager: Boolean
  ): ManagerContainer = {
    lazy val eventRequestManager = virtualMachine.eventRequestManager()
    lazy val eventQueue = virtualMachine.eventQueue()
    lazy val accessWatchpointManager =
      new StandardAccessWatchpointManager(eventRequestManager, classManager)
        with StandardPendingAccessWatchpointSupport
    lazy val breakpointManager =
      new StandardBreakpointManager(eventRequestManager, classManager)
        with StandardPendingBreakpointSupport
    lazy val classManager =
      new StandardClassManager(virtualMachine, loadClasses = true)
    lazy val classPrepareManager =
      new StandardClassPrepareManager(eventRequestManager)
        with StandardPendingClassPrepareSupport
    lazy val classUnloadManager =
      new StandardClassUnloadManager(eventRequestManager)
        with StandardPendingClassUnloadSupport
    lazy val eventManager = new StandardEventManager(
      eventQueue,
      loopingTaskRunner,
      autoStart = autoStartEventManager
    )
    lazy val exceptionManager =
      new StandardExceptionManager(virtualMachine, eventRequestManager)
        with StandardPendingExceptionSupport
    lazy val methodEntryManager =
      new StandardMethodEntryManager(eventRequestManager)
        with StandardPendingMethodEntrySupport
    lazy val methodExitManager =
      new StandardMethodExitManager(eventRequestManager)
        with StandardPendingMethodExitSupport
    lazy val modificationWatchpointManager =
      new StandardModificationWatchpointManager(eventRequestManager, classManager)
        with StandardPendingModificationWatchpointSupport
    lazy val monitorContendedEnteredManager =
      new StandardMonitorContendedEnteredManager(eventRequestManager)
        with StandardPendingMonitorContendedEnteredSupport
    lazy val monitorContendedEnterManager =
      new StandardMonitorContendedEnterManager(eventRequestManager)
        with StandardPendingMonitorContendedEnterSupport
    lazy val monitorWaitedManager =
      new StandardMonitorWaitedManager(eventRequestManager)
        with StandardPendingMonitorWaitedSupport
    lazy val monitorWaitManager =
      new StandardMonitorWaitManager(eventRequestManager)
        with StandardPendingMonitorWaitSupport
    lazy val requestManager =
      virtualMachine.eventRequestManager()
    lazy val stepManager =
      new StandardStepManager(eventRequestManager)
        with StandardPendingStepSupport
    lazy val threadDeathManager =
      new StandardThreadDeathManager(eventRequestManager)
        with StandardPendingThreadDeathSupport
    lazy val threadStartManager =
      new StandardThreadStartManager(eventRequestManager)
        with StandardPendingThreadStartSupport
    lazy val vmDeathManager =
      new StandardVMDeathManager(eventRequestManager)
        with StandardPendingVMDeathSupport

    ManagerContainer(
      accessWatchpointManager         = accessWatchpointManager,
      breakpointManager               = breakpointManager,
      classManager                    = classManager,
      classPrepareManager             = classPrepareManager,
      classUnloadManager              = classUnloadManager,
      eventManager                    = eventManager,
      exceptionManager                = exceptionManager,
      methodEntryManager              = methodEntryManager,
      methodExitManager               = methodExitManager,
      modificationWatchpointManager   = modificationWatchpointManager,
      monitorContendedEnteredManager  = monitorContendedEnteredManager,
      monitorContendedEnterManager    = monitorContendedEnterManager,
      monitorWaitedManager            = monitorWaitedManager,
      monitorWaitManager              = monitorWaitManager,
      requestManager                  = requestManager,
      stepManager                     = stepManager,
      threadDeathManager              = threadDeathManager,
      threadStartManager              = threadStartManager,
      vmDeathManager                  = vmDeathManager
    )
  }

  /**
   * Initializes all managers to dummy implementations with pending requests
   * enabled to allow setting requests ahead of virtual machine connections.
   *
   * @note Currently, classManager and requestManager are null!
   *
   * @return The container holding all of the new managers
   */
  def usingDummyManagers(): ManagerContainer = {
    lazy val accessWatchpointManager =
      new DummyAccessWatchpointManager
        with StandardPendingAccessWatchpointSupport
    lazy val breakpointManager =
      new DummyBreakpointManager
        with StandardPendingBreakpointSupport
    lazy val classManager = null
    lazy val classPrepareManager =
      new DummyClassPrepareManager
        with StandardPendingClassPrepareSupport
    lazy val classUnloadManager =
      new DummyClassUnloadManager
        with StandardPendingClassUnloadSupport
    lazy val eventManager =
      new DummyEventManager
        with StandardPendingEventHandlerSupport
    lazy val exceptionManager =
      new DummyExceptionManager
        with StandardPendingExceptionSupport
    lazy val methodEntryManager =
      new DummyMethodEntryManager
        with StandardPendingMethodEntrySupport
    lazy val methodExitManager =
      new DummyMethodExitManager
        with StandardPendingMethodExitSupport
    lazy val modificationWatchpointManager =
      new DummyModificationWatchpointManager
        with StandardPendingModificationWatchpointSupport
    lazy val monitorContendedEnteredManager =
      new DummyMonitorContendedEnteredManager
        with StandardPendingMonitorContendedEnteredSupport
    lazy val monitorContendedEnterManager =
      new DummyMonitorContendedEnterManager
        with StandardPendingMonitorContendedEnterSupport
    lazy val monitorWaitedManager =
      new DummyMonitorWaitedManager
        with StandardPendingMonitorWaitedSupport
    lazy val monitorWaitManager =
      new DummyMonitorWaitManager
        with StandardPendingMonitorWaitSupport
    lazy val requestManager = null
    lazy val stepManager =
      new DummyStepManager
        with StandardPendingStepSupport
    lazy val threadDeathManager =
      new DummyThreadDeathManager
        with StandardPendingThreadDeathSupport
    lazy val threadStartManager =
      new DummyThreadStartManager
        with StandardPendingThreadStartSupport
    lazy val vmDeathManager =
      new DummyVMDeathManager
        with StandardPendingVMDeathSupport

    ManagerContainer(
      accessWatchpointManager         = accessWatchpointManager,
      breakpointManager               = breakpointManager,
      classManager                    = classManager,
      classPrepareManager             = classPrepareManager,
      classUnloadManager              = classUnloadManager,
      eventManager                    = eventManager,
      exceptionManager                = exceptionManager,
      methodEntryManager              = methodEntryManager,
      methodExitManager               = methodExitManager,
      modificationWatchpointManager   = modificationWatchpointManager,
      monitorContendedEnteredManager  = monitorContendedEnteredManager,
      monitorContendedEnterManager    = monitorContendedEnterManager,
      monitorWaitedManager            = monitorWaitedManager,
      monitorWaitManager              = monitorWaitManager,
      requestManager                  = requestManager,
      stepManager                     = stepManager,
      threadDeathManager              = threadDeathManager,
      threadStartManager              = threadStartManager,
      vmDeathManager                  = vmDeathManager
    )
  }
}
