package de.stammtischHub.terminPilot.persistence.entity.constraints

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Exactly one of two nullable properties must be set - never both, never neither.
 *
 * Usage:
 * ```
 * @ExactlyOneNotNull(fieldOne = "googleAccess", fieldTwo = "calDavAccess")
 * class Calendar : BaseLongId() {
 *   var googleAccess: GoogleAccess? = null
 *   var calDavAccess: CalDavAccess? = null
 * }
 * ```
 *
 * `fieldOne` / `fieldTwo` must name real properties of the annotated class (validated via
 * reflection), so a typo fails fast with a clear error rather than silently passing.
 * Requires kotlin-reflect on the classpath.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [ExactlyOneNotNullValidator::class])
annotation class ExactlyOneNotNull(
  val fieldOne: String,
  val fieldTwo: String,
  val message: String = "exactly one of {fieldOne} and {fieldTwo} must be set",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

class ExactlyOneNotNullValidator : ConstraintValidator<ExactlyOneNotNull, Any> {
  private lateinit var fieldOne: String
  private lateinit var fieldTwo: String

  override fun initialize(constraintAnnotation: ExactlyOneNotNull) {
    fieldOne = constraintAnnotation.fieldOne
    fieldTwo = constraintAnnotation.fieldTwo
  }

  override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
    if (value == null) return true

    val valueOne = readProperty(value, fieldOne)
    val valueTwo = readProperty(value, fieldTwo)

    return (valueOne == null) != (valueTwo == null)
  }

  private fun readProperty(target: Any, propertyName: String): Any? {
    val property = target::class.memberProperties.find { it.name == propertyName }
      ?: error(
        "@ExactlyOneNotNull references unknown property '$propertyName' " +
          "on ${target::class.simpleName}",
      )

    property.isAccessible = true
    return property.getter.call(target)
  }
}
