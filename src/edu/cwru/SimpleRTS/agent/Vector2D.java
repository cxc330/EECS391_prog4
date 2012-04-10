package edu.cwru.SimpleRTS.agent;

public class Vector2D {
	
	public Vector2D(){}
	public Vector2D(Integer X, Integer Y) {
		x = X;
		y = Y;
	}
	
	public Integer x;
	public Integer y;
	
	public String toString()
	{
		return "("  + x + ", " + y + ")"; 
	}
	
	public boolean equals(Vector2D v)
	{
		return (v.x == x && v.y == y);
	}
}
