package rogatkin.mobile.data.pertusin;


public @interface PresentA {
	enum FieldType {
		Text, Number, Phone, Password, Money, Date, Time
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

	/** Name of storage field corresponding to the form field
	 * 
	 * @return
	 */
	String storeFieldName() default "";

	/** Form field name if different than a field name
	 * 
	 * @return
	 */
	String viewFieldName() default "";

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

}
