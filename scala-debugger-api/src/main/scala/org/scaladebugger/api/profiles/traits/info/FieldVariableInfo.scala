package org.scaladebugger.api.profiles.traits.info

import com.sun.jdi.Field

import scala.util.Try


/**
 * Represents the interface for variable-based interaction with field-specific
 * information.
 */
trait FieldVariableInfo extends VariableInfo with CreateInfo with CommonInfo {
  /**
   * Converts the current profile instance to a representation of
   * low-level Java instead of a higher-level abstraction.
   *
   * @return The profile instance providing an implementation corresponding
   *         to Java
   */
  override def toJavaInfo: FieldVariableInfo

  /**
    * Returns the JDI representation this profile instance wraps.
    *
    * @return The JDI instance
    */
  override def toJdiInstance: Field

  /**
   * Returns the parent that contains this field.
   *
   * @return The reference type information (if a static field) or object
   *         information (if a non-static field)
   */
  def parent: Either[ObjectInfo, ReferenceTypeInfo]

  /**
   * Returns the type where this field was declared.
   *
   * @return The reference type information that declared this field
   */
  def declaringType: ReferenceTypeInfo

  /**
   * Returns the type where this field was declared.
   *
   * @return The reference type information that declared this field
   */
  def tryDeclaringType: Try[ReferenceTypeInfo] = Try(declaringType)
}
