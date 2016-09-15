package rogatkin.mobile.data.pertusin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface WebA {
	String value() default "";
	boolean header() default false;
	boolean response() default false;
	boolean path() default false;
}
