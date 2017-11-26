package test;

import static org.junit.Assert.assertEquals;
import com.bavelsoft.entityobserver.Observer;

public class Test {
	int counter = 0;
	@org.junit.Test
	public void foo() {
		MyEntity e = new MyEntityObservable(new MyEntityImpl(), new Observer<MyEntity, Void>() {
			public void afterChange(MyEntity e, Void v) {
				new Throwable().printStackTrace();
				counter++;
			}
		});
		e.g();
		e.g();
		e.g();
		assertEquals(4, counter);
	}
}
