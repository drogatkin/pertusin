package rogatkin.mobile.data.pertusin;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

@Target(value={ElementType.METHOD,ElementType.CONSTRUCTOR,ElementType.FIELD})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
/** It is used to inject dependencies
 * 
 * @author dmitriy
 *
 */
public @interface InjectA {

}
