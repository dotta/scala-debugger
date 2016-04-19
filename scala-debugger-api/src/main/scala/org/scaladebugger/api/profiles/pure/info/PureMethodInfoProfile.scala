package org.scaladebugger.api.profiles.pure.info
//import acyclic.file

import com.sun.jdi.Method
import org.scaladebugger.api.profiles.traits.info.MethodInfoProfile
import org.scaladebugger.api.virtualmachines.ScalaVirtualMachine

/**
 * Represents a pure implementation of a method profile that adds no
 * custom logic on top of the standard JDI.
 *
 * @param scalaVirtualMachine The high-level virtual machine containing the
 *                            method
 * @param _method The reference to the underlying JDI method
 */
class PureMethodInfoProfile(
  val scalaVirtualMachine: ScalaVirtualMachine,
  private val _method: Method
) extends MethodInfoProfile {
  /**
   * Returns the JDI representation this profile instance wraps.
   *
   * @return The JDI instance
   */
  override def toJdiInstance: Method = _method

  /**
   * Returns the name of this method.
   *
   * @return The name of the method
   */
  override def name: String = _method.name

  /**
   * Returns the fully-qualified class name of the type for the return value
   * of this method.
   *
   * @return The return type name
   */
  override def returnTypeName: String = _method.returnTypeName()

  /**
   * Returns the fully-qualified class names of the types for the parameters
   * of this method.
   *
   * @return The collection of parameter type names
   */
  override def parameterTypeNames: Seq[String] = {
    import scala.collection.JavaConverters._
    _method.argumentTypeNames().asScala
  }
}