package org.scaladebugger.api.profiles.swappable.methods

import org.scaladebugger.api.lowlevel.JDIArgument
import org.scaladebugger.api.pipelines.Pipeline.IdentityPipeline
import org.scaladebugger.api.profiles.swappable.SwappableDebugProfile
import org.scaladebugger.api.profiles.traits.methods.MethodExitProfile

import scala.util.Try

/**
 * Represents a swappable profile for method exit events that redirects the
 * invocation to another profile.
 */
trait SwappableMethodExitProfile extends MethodExitProfile {
  this: SwappableDebugProfile =>

  override def onMethodExitWithData(
    className: String,
    methodName: String,
    extraArguments: JDIArgument*
  ): Try[IdentityPipeline[MethodExitEventAndData]] = {
    withCurrentProfile.onMethodExitWithData(
      className,
      methodName,
      extraArguments: _*
    )
  }
}
