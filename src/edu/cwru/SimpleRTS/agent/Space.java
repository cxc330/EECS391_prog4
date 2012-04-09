package edu.cwru.SimpleRTS.agent;

public class Space {
	
	public Space (){}
	
	public Space (Vector2D position)
	{
		pos = position;
	}
	
	Space parent;
	Vector2D pos = new Vector2D();
	boolean visited = false;
	boolean gold = false;

}
