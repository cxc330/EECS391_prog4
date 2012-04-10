package edu.cwru.SimpleRTS.agent;

public class Space {

	Vector2D pos = new Vector2D();
	
	public Space (){}
	
	public Space (Vector2D position)
	{
		pos = position;
	}
	
	Space parent;
	boolean visited = false;
	boolean gold = false;

}
