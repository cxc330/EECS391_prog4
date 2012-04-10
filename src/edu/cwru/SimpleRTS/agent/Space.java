package edu.cwru.SimpleRTS.agent;

public class Space {

	Vector2D pos = new Vector2D();
	
	public Space (){}
	
	public Space (Vector2D position)
	{
		pos = position;
	}
	
	Space parent = null;
	boolean visited = false;
	boolean gold = false;

	public boolean equals(Space s)
	{
		return s.pos.equals(pos);
	}
}
