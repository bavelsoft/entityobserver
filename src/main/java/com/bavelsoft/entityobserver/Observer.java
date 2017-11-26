package com.bavelsoft.entityobserver;

public interface Observer<T, V> {
	default V beforeChange(T t) {
		return null;
	};
	void afterChange(T t, V v);
}
