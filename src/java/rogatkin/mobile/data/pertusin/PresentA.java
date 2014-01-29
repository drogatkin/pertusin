package rogatkin.mobile.data.pertusin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.FIELD, ElementType.METHOD })
public @interface PresentA {
	enum FieldType {
		Text, Number, Phone, Password, Money, Date, Time, Percent, Quantity
	}
	

	/** Tells if the field requires to have value
	 *  
	 * @return
	 */
	boolean required() default false;
	
	/** can be taken by validator classes to take extra parameters, like max length
	 * 
	 * @return
	 */
	String validationExpression() default "";

	
	/** Default field value to a value when no user input
	 * 
	 * @return
	 */
	String defaultTo() default "";


	/** Form field name if different than a field name
	 * 
	 * @return
	 */
	String viewFieldName() default "";
	
	/** filed is stored in view tag
	 * 
	 * @return
	 */
	String viewTagName() default "";
	
	/** defines view name to populate in list view
	 * 
	 * @return
	 */
	String listViewFieldName() default "";

	/** Defines presentation attribute of a field
	 * 
	 * @return
	 */
	FieldType presentType() default FieldType.Text;

	/** Defines presentation size
	 * 
	 */
	int presentSize() default -1;

	/** Defines a number of presentation rows
	 * 
	 * @return
	 */
	int presentRows() default -1;
	
	int presentPrecision() default -1;
	
	String fillValuesResource() default "";

}
