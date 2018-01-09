package blockchain.server.utils;


public class Tuple <X, Y> { 
	private X x; 
	private Y y; 
	
	public Tuple(X x, Y y) { 
		this.x = x; 
		this.y = y; 
	} 
	
	public X getVal1() {
		return this.x;
	}
	
	public Y getVal2() {
		return this.y;
	}
	
	public void setVal1(X x) {
		this.x = x;
	}
	
	public void setVal2(Y y) {
		this.y = y;
	}
} 
