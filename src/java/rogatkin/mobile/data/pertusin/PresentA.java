package rogatkin.mobile.data.pertusin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface PresentA {
	enum FieldType {
		Text, Number, Phone, Password, Money, Date, Time, Percent, Quantity
	}

	/**
	 * Tells if the field requires to have value
	 * 
	 * @return
	 */
	boolean required() default false;

	/**
	 * can be taken by validator classes to take extra parameters, like max
	 * length
	 * 
	 * @return
	 */
	String validationExpression() default "";

	/**
	 * Default field value to a value when no user input
	 * 
	 * @return
	 */
	String defaultTo() default "";

	/**
	 * Form field name if different than a field name
	 * 
	 * @return
	 */
	String viewFieldName() default "";

	/**
	 * filed is stored in view tag
	 * 
	 * @return
	 */
	String viewTagName() default "";

	/**
	 * defines view name to populate in list view
	 * 
	 * @return
	 */
	String listViewFieldName() default "";

	/**
	 * Defines presentation attribute of a field
	 * 
	 * @return
	 */
	FieldType presentType() default FieldType.Text;

	/**
	 * Defines presentation size
	 * 
	 */
	int presentSize() default -1;

	/**
	 * Defines a number of presentation rows
	 * 
	 * @return
	 */
	int presentRows() default -1;

	/**
	 * precision
	 * 
	 * @return
	 */
	int presentPrecision() default -1;

	/**
	 * array resource holding value for spinner
	 * 
	 * @return
	 */
	String fillValuesResource() default "";

	/**
	 * converter class used to convert field value in value for display
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	Class<? extends ConverterI> viewConvertor() default ConverterI.class;

	/**
	 * converter class used to convert field value in value for edit, and then
	 * convert it back to field
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	Class<? extends ConverterI> editConvertor() default ConverterI.class;
	
	/** does normalization value, several normalization codes can be used
	 * <p>
	 * 'U' - to upper case<br>
	 * 'l' - to lover case<br>
	 * 'T', 't' - trim white spaces<br>
	 * 'C' -  to capital case, for example james smith -> James Smith
	 * 'Z' - no null, means if field didn't come from form, no null fill be placed in target field, that
	 * make preserve default value there.
	 * 
	 * @return string concatenation of normalization codes, for example "Ut" - to upper case and trim
	 * <p> normalization happens as at reading form value as at writing it. Normalization
	 * codes are case insensitive.
	 */
	String normalize() default "";
}
