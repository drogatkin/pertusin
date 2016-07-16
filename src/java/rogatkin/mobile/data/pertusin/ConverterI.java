package rogatkin.mobile.data.pertusin;

public interface ConverterI<T> {
	String to(T v);

	T from(String f);
}
