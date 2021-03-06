package org.scaladebugger.api.profiles.swappable.requests.threads

import org.scaladebugger.api.lowlevel.JDIArgument
import org.scaladebugger.api.lowlevel.threads.ThreadStartRequestInfo
import org.scaladebugger.api.pipelines.Pipeline.IdentityPipeline
import org.scaladebugger.api.profiles.swappable.SwappableDebugProfileManagement
import org.scaladebugger.api.profiles.traits.requests.threads.ThreadStartRequest

import scala.util.Try

/**
 * Represents a swappable profile for thread start events that redirects the
 * invocation to another profile.
 */
trait SwappableThreadStartRequest extends ThreadStartRequest {
  this: SwappableDebugProfileManagement =>

  override def tryGetOrCreateThreadStartRequestWithData(
    extraArguments: JDIArgument*
  ): Try[IdentityPipeline[ThreadStartEventAndData]] = {
    withCurrentProfile.tryGetOrCreateThreadStartRequestWithData(extraArguments: _*)
  }

  override def isThreadStartRequestWithArgsPending(
    extraArguments: JDIArgument*
  ): Boolean = {
    withCurrentProfile.isThreadStartRequestWithArgsPending(extraArguments: _*)
  }

  override def removeThreadStartRequestWithArgs(
    extraArguments: JDIArgument*
  ): Option[ThreadStartRequestInfo] = {
    withCurrentProfile.removeThreadStartRequestWithArgs(extraArguments: _*)
  }

  override def removeAllThreadStartRequests(): Seq[ThreadStartRequestInfo] = {
    withCurrentProfile.removeAllThreadStartRequests()
  }

  override def threadStartRequests: Seq[ThreadStartRequestInfo] = {
    withCurrentProfile.threadStartRequests
  }
}
