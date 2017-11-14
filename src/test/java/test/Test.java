package test;

public class Test {
	@org.junit.Test
	public void foo() {
		int c = 0;
		MyEntity myEntity = new MyEntityObservable(new MyEntityImpl(), new Observer<MyEntity, Void> {
	Void beforeChange(MyEntity t) { return null; }
	void afterChange(MyEntity t, Void v) { c++; }
});
		myEntity.f();
		myEntity.f();
		myEntity.f();
		assertEquals(myEntity.g(), 42);
		assertEquals(c, 3);
	}
}
