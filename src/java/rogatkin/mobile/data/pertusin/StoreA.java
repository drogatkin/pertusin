package rogatkin.mobile.data.pertusin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.FIELD, ElementType.TYPE })
public @interface StoreA {
	/** defines type of field how it appears in table create SQL statement
	 * 
	 * @return
	 */
	String type() default ""; // make it enum?

	/** defines name used for database if different that field
	 * 
	 * @return
	 */
	String storeName() default "";
	
	/** defines size of field in field type units
	 * 
	 * @return
	 */
	int size() default -1;

	/** tells to create index against the field
	 * 
	 * @return
	 */
	boolean index() default false;

	/** tell that the field is primary key
	 * 
	 * @return
	 */
	boolean key() default false;

	/** tells to apply uniqueness constraint
	 * 
	 * @return
	 */
	boolean unique() default false;

	/** Class name used for conversion of field object type to/from string (varchar) stored in db
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	Class<? extends ConverterI> converter() default ConverterI.class;

	/** name of database when defined on a class level
	 * 
	 * @return
	 */
	String sql() default "";

	/** specifies tables and corresponding primary keys of the foreign key
	 * in form "table_name(field_name)"
	 * @return arrays of tables and corresponding keys in format "table_name(key_name)"
	 */
	String[] foreign() default {};
	
	/** auto generated key, if = 0, no auto
	 * 
	 * @return auto key value generation start number, unless 0
	 */
	int auto() default 0;
	
	/** defines precision, number of digits after dot for numeric fields
	 * ignored for other
	 * @return
	 */
	int precision() default 0;
	
	/** indicates to ignore case sensitivity of stored value. It is applied for search and sort operations
	 * 
	 * @return
	 */
	boolean nocase() default false;
	
	/** adds a qualifier to all fields as qualifier. unless a field name has '.' or 'as '
	 * 
	 * @return
	 */
	//String qualifier() default "";
}
