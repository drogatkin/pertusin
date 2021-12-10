package rogatkin.mobile.data.pertusin;

public interface ValidationHandlerI<T> {
	/** validate a field value accordingly type 
	 * and rises IllegalArgumentException if invalid
	 * @param field
	 */
   void validate(T field) ; 
}
