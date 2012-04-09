package edu.cwru.SimpleRTS.agent;

import java.util.*;

import edu.cwru.SimpleRTS.action.*;
import edu.cwru.SimpleRTS.environment.State.StateView;
import edu.cwru.SimpleRTS.model.unit.Unit.UnitView;

public class TowerAgent extends Agent {

	private static final long serialVersionUID = 1L;
	static int playernum = 0;
	static String townHall = "TownHall";
	static String peasant = "Peasant";
	static String gather = "gather";
	static String deposit = "deposit";
	private ArrayList<Integer> peasantID = new ArrayList<Integer>();
	private List<Integer> townHallIds;
	
	/*
	 * Variables used for searching
	 */
	ArrayList<Space> openList = new ArrayList<Space>();
	ArrayList<Space> hitList = new ArrayList<Space>();
	ArrayList<Space> path = new ArrayList<Space>();
	ArrayList<ArrayList<Space>> spaces = new ArrayList<ArrayList<Space>>(); //2D array for spaces
	Space move = new Space();
	int peasantHealth = -1;
	UnitView currentPeasant;
	StateView publicState = null;
	
	public TowerAgent(int playernum, String[] args) 
	{
		super(playernum);
	}

	@Override
	public Map<Integer, Action> initialStep(StateView state) 
	{	
		currentPeasant = state.getUnit(peasantID.get(0));
		return middleStep(state);
	}
	
	@Override
	public Map<Integer, Action> middleStep(StateView state) 
	{		
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
		publicState = state;

		if (path.size() <= 0) //main loop
		{
			if (move == null) //get and make a move
			{
				move = getMove();
				actions.put(currentPeasant.getID(), makeMove(move));
			}
			else //check if we were hit
			{
				if (checkHit()) //if we were hit
				{
					hitList.add(move);
					makeMove(move.parent);
					addTowers(move);
				}
				else //add node to safe list
				{
					move.visited = true;
					updateTowers(move);
				}
				
				openList.remove(move);				
				move = new Space();
				move = null;
			}
		}
		else
		{
			traverse(path);
		}
		return actions;
	}

	@Override
	public void terminalStep(StateView state) {
	}
	
	private void traverse(ArrayList<Space> path) {
		// TODO traverse to next node
		
	}

	private void updateTowers(Space move) {
		// TODO Auto-generated method stub
		
	}

	private void addTowers(Space move) {
		// TODO Auto-generated method stub
		
	}

	private boolean checkHit() {
		
		if (currentPeasant.getHP() < peasantHealth)
			return true;
		
		return false;
	}

	private Action makeMove(Space move) {
		
		peasantHealth = currentPeasant.getHP();
		return Action.createCompoundMove(currentPeasant.getID(), move.pos.x, move.pos.y);
	}
	
	/*
	 * This method will get a move and return the move
	 * returns null on Gold find
	 */
	private Space getMove() 
	{
		ArrayList<Space> neighbors = getNeighbors(currentPeasant); //get all neighbors
		neighbors = checkVisited(neighbors); //parse out all ready visited neighbors
		
		if (containsGold(neighbors)) //make sure we didn't return a gold node
			return null;
		
		addToOL(neighbors); //add the valid neighbors to the OL
		
		Space lowestProbSpace = getLowestProb(openList);
		int lowestProb = getProb(lowestProbSpace);
		
		for (Space neighbor : neighbors) //check if one of our neighbors is the best choice
		{
			if (getProb(neighbor) <= lowestProb) //if it's better or the same as best choice
			{
				return getFromOL(neighbor); //return it
			}
		}
		
		return lowestProbSpace;
	}

	private Space getFromOL(Space neighbor) {
		
		if (openList.contains(neighbor))
		{
			return openList.get(openList.indexOf(neighbor));
		}
		return null;
	}

	private int getProb(Space lowestProbSpace) {
		// TODO calculate the probability of the square... should be constant if outside range of tower
		return 0;
	}

	private Space getLowestProb(ArrayList<Space> spaces) {
		
		int lowestProb = Integer.MAX_VALUE;
		
		for (Space space : spaces)
		{
			if (getProb(space) <= lowestProb)
				return space;
		}
		
		return null;
	}

	private void addToOL(ArrayList<Space> spaces) {
		
		for (Space space : spaces)
		{
			if (!openList.contains(space))
			{
				openList.add(space);
			}
		}
		
	}

	private boolean containsGold(ArrayList<Space> neighbors) {
		
		for (Space space : neighbors)
		{
			if (space.gold)
				return true;
		}
		
		return false;
	}

	private ArrayList<Space> checkVisited(ArrayList<Space> neighbors) {
		
		for (Space space : neighbors)
		{
			if (space.visited)
				neighbors.remove(space);
		}
		return neighbors;
	}

	private ArrayList<Space> getNeighbors(UnitView peasant) {

		ArrayList<Space> neighbors = new ArrayList<Space>();
		
		Integer x = peasant.getXPosition();
		Integer y = peasant.getYPosition();
		Integer xPlusOne = x + 1;
		Integer xMinusOne = x - 1;
		Integer yPlusOne = y + 1;
		Integer yMinusOne = y - 1;		
		Integer tempX = 0, tempY = 0;

		//checking all 8 possible spaces in a grid world
		for (int j = 0; j < 8; j++)
		{
			switch(j)
			{
				case 0: //x + 1, y
					tempX = xPlusOne;
					tempY = y;
					break;
				case 1: //x + 1, y + 1
					tempX = xPlusOne;
					tempY = yPlusOne;
					break;
				case 2: //x + 1, y - 1
					tempX = xPlusOne;
					tempY = yMinusOne;
					break;
				case 3: //x, y + 1
					tempX = x;
					tempY = yPlusOne;
					break;
				case 4: //x, y - 1
					tempX = x;
					tempY = yMinusOne;
					break;
				case 5: //x - 1, y
					tempX = xMinusOne;
					tempY = y;
					break;
				case 6: //x - 1, y + 1
					tempX = xMinusOne;
					tempY = yPlusOne;
					break;
				case 7: //x - 1, y - 1
					tempX = xMinusOne;
					tempY = yMinusOne;
					break;
				default:
					break;
			}
			
			Vector2D position = new Vector2D(tempX, tempY);

			Space neighbor = new Space(position);

			if(checkValidNeighbor(tempX, tempY)) //check if it's a valid space
			{
				neighbors.add(neighbor);
			}
		}		
		return neighbors;
	}
	
	private boolean checkValidNeighbor(Integer x, Integer y)
	{
		boolean isUnit = false;
		boolean isResource = false;
		boolean isValid = false;
		
		isUnit = publicState.isUnitAt(x, y);
		isValid = publicState.inBounds(x, y);
		isResource = publicState.isResourceAt(x, y);
		
		return ((!isUnit && !isResource) && isValid);
	}
}
