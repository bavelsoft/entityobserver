package test;

@com.bavelsoft.entityobserver.Observable
interface MyEntity { //extends MyEntitySuper {
	void f();
	int g();
	int h(int j);
	int h(int j, int k);
}
