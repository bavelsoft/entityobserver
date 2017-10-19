package com.bavelsoft.entityobserver;

public interface Observer<T, V> {
	V beforeChange(T t);
	void afterChange(T t, V v);
}
