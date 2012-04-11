package edu.cwru.SimpleRTS.agent;

public class Space {

	Vector2D pos = new Vector2D();
	Space parent = null;
	boolean visited = false;
	boolean gold = false;
	boolean tree = false;
	int possibleTower = -1; //-1 undetermined, 0 = no tower, 1 = yes tower
	//boolean possibleTower = false;
	boolean possibleHit = false;
	int distanceToNearestTree = -1;
	
	//Default Constructor
	public Space (){
		
	}
	
	//Secondary Constructor
	public Space (Vector2D position)
	{
		pos = position;
	} 
	
	public boolean equals(Space s)
	{
		return s.pos.equals(pos);
	}
}
