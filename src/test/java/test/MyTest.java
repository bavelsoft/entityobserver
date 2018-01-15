package test;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import com.bavelsoft.entityobserver.Observer;

public class MyTest {
	int counter = 0;

	@Test public void simple() {
		MyEntity e = new MyEntityObservable(new MyEntityImpl(), new Observer<MyEntity, Void>() {
			public void afterChange(MyEntity e, Void v) {
				counter++;
			}
		});
		e.g();
		e.g();
		e.g();
		assertEquals(4, counter);
	}

/*
	@Test public void simple() {
		MyEntity e = new MyEntityObservable(new MyEntityImpl(), new Observer<MyEntity, Integer>() {
			public Integer beforeChange(MyEntity e) {
				return e.getX();
			}

			public void afterChange(MyEntity e, Integer v) {
			}
		});
	}
*/
}
