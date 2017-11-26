package test;

public class MyEntityImpl implements MyEntity {
	private int x;

	public int f(int x) {
		return x+13;
	}

	public void g() {
		x++;
	}

	public int getX() {
		return x;
	}
}
