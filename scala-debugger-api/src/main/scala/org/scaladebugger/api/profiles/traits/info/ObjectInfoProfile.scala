package org.scaladebugger.api.profiles.traits.info
//import acyclic.file

import org.scaladebugger.api.lowlevel.JDIArgument

import scala.util.Try

/**
 * Represents the interface for object-based interaction.
 */
trait ObjectInfoProfile extends ValueInfoProfile {
  /**
   * Represents the unique id of this object.
   *
   * @return The unique id as a long
   */
  def uniqueId: Long

  /**
   * Invokes the object's method with matching name and arguments.
   *
   * @param methodName The name of the method to invoke
   * @param arguments The arguments to provide to the method
   * @param jdiArguments Optional arguments to provide custom settings to the
   *                     method invocation
   * @return Success containing the resulting value of the invocation, otherwise
   *         a failure
   */
  def tryInvoke(
    methodName: String,
    arguments: Seq[Any],
    jdiArguments: JDIArgument*
  ): Try[ValueInfoProfile] = tryInvoke(
    methodName,
    arguments.map(_.getClass.getName),
    arguments,
    jdiArguments: _*
  )

  /**
   * Invokes the object's method with matching name and arguments.
   *
   * @param methodName The name of the method to invoke
   * @param arguments The arguments to provide to the method
   * @param jdiArguments Optional arguments to provide custom settings to the
   *                     method invocation
   * @return The resulting value of the invocation
   */
  def invoke(
    methodName: String,
    arguments: Seq[Any],
    jdiArguments: JDIArgument*
  ): ValueInfoProfile = invoke(
    methodName,
    arguments.map(_.getClass.getName),
    arguments,
    jdiArguments: _*
  )

  /**
   * Invokes the object's method with matching name and arguments.
   *
   * @param methodName The name of the method to invoke
   * @param parameterTypeNames The names of the parameter types of the method
   *                           to invoke
   * @param arguments The arguments to provide to the method
   * @param jdiArguments Optional arguments to provide custom settings to the
   *                     method invocation
   * @return Success containing the resulting value of the invocation, otherwise
   *         a failure
   */
  def tryInvoke(
    methodName: String,
    parameterTypeNames: Seq[String],
    arguments: Seq[Any],
    jdiArguments: JDIArgument*
  ): Try[ValueInfoProfile] = Try(invoke(
    methodName,
    parameterTypeNames,
    arguments,
    jdiArguments: _*
  ))

  /**
   * Invokes the object's method with matching name and arguments.
   *
   * @param methodName The name of the method to invoke
   * @param parameterTypeNames The names of the parameter types of the method
   *                           to invoke
   * @param arguments The arguments to provide to the method
   * @param jdiArguments Optional arguments to provide custom settings to the
   *                     method invocation
   * @return The resulting value of the invocation
   */
  def invoke(
    methodName: String,
    parameterTypeNames: Seq[String],
    arguments: Seq[Any],
    jdiArguments: JDIArgument*
  ): ValueInfoProfile

  /**
   * Invokes the object's method.
   *
   * @param methodInfoProfile The method of the object to invoke
   * @param arguments The arguments to provide to the method
   * @param jdiArguments Optional arguments to provide custom settings to the
   *                     method invocation
   * @return Success containing the resulting value of the invocation, otherwise
   *         a failure
   */
  def tryInvoke(
    methodInfoProfile: MethodInfoProfile,
    arguments: Seq[Any],
    jdiArguments: JDIArgument*
  ): Try[ValueInfoProfile] = Try(invoke(
    methodInfoProfile,
    arguments,
    jdiArguments: _*
  ))

  /**
   * Invokes the object's method.
   *
   * @param methodInfoProfile The method of the object to invoke
   * @param arguments The arguments to provide to the method
   * @param jdiArguments Optional arguments to provide custom settings to the
   *                     method invocation
   * @return The resulting value of the invocation
   */
  def invoke(
    methodInfoProfile: MethodInfoProfile,
    arguments: Seq[Any],
    jdiArguments: JDIArgument*
  ): ValueInfoProfile

  /**
   * Returns all visible fields contained in this object.
   *
   * @return Success containing the profiles wrapping the visible fields in
   *         this object, otherwise a failure
   */
  def tryGetFields: Try[Seq[VariableInfoProfile]] = Try(getFields)

  /**
   * Returns all visible fields contained in this object.
   *
   * @return The profiles wrapping the visible fields in this object
   */
  def getFields: Seq[VariableInfoProfile]

  /**
   * Returns the object's field with the specified name.
   *
   * @param name The name of the field
   * @return Success containing the profile wrapping the field, otherwise
   *         a failure
   */
  def tryGetField(name: String): Try[VariableInfoProfile] = Try(getField(name))

  /**
   * Returns the object's field with the specified name.
   *
   * @param name The name of the field
   * @return The profile wrapping the field
   */
  def getField(name: String): VariableInfoProfile

  /**
   * Returns all visible methods contained in this object.
   *
   * @return Success containing the profiles wrapping the visible methods in
   *         this object, otherwise a failure
   */
  def tryGetMethods: Try[Seq[MethodInfoProfile]] = Try(getMethods)

  /**
   * Returns all visible methods contained in this object.
   *
   * @return The profiles wrapping the visible methods in this object
   */
  def getMethods: Seq[MethodInfoProfile]

  /**
   * Returns the object's method with the specified name.
   *
   * @param name The name of the method
   * @param parameterTypeNames The fully-qualified type names of the parameters
   *                           of the method to find
   * @return Success containing the profile wrapping the method, otherwise
   *         a failure
   */
  def tryGetMethod(
    name: String,
    parameterTypeNames: String*
  ): Try[MethodInfoProfile] = Try(getMethod(name, parameterTypeNames: _*))

  /**
   * Returns the object's method with the specified name.
   *
   * @param name The name of the method
   * @param parameterTypeNames The fully-qualified type names of the parameters
   *                           of the method to find
   * @return The profile wrapping the method
   */
  def getMethod(
    name: String,
    parameterTypeNames: String*
  ): MethodInfoProfile
}
