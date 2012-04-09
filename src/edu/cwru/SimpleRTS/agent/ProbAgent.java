package edu.cwru.SimpleRTS.agent;

import java.util.*;

import edu.cwru.SimpleRTS.action.*;
import edu.cwru.SimpleRTS.environment.State.StateView;
import edu.cwru.SimpleRTS.model.unit.Unit.UnitView;

public class ProbAgent extends Agent {

	private static final long serialVersionUID = 1L;
	static int playernum = 0;
	static String townHall = "TownHall";
	static String peasant = "Peasant";
	static String gather = "gather";
	static String deposit = "deposit";
	private List<Integer> peasantID = new ArrayList<Integer>();
	private List<Integer> townHallIds;
	int waitCounter = 0;
	
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
	
	public ProbAgent(int playernum, String[] args) 
	{
		super(playernum);
	}

	@Override
	public Map<Integer, Action> initialStep(StateView state) 
	{	
		peasantID = findUnitType(state.getAllUnitIds(), state, peasant);
		currentPeasant = state.getUnit(peasantID.get(0));
		int size = spaces.size();
		for (int j1 = 0; j1 <= currentPeasant.getXPosition() - size; j1++)
		{
			spaces.add(new ArrayList<Space>());
		}
		size = spaces.get(currentPeasant.getXPosition()).size();
		for (int j1 = 0; j1 <= currentPeasant.getYPosition() - size; j1++)
		{
			Vector2D location = new Vector2D(currentPeasant.getXPosition(), spaces.size() + j1);
			spaces.get(currentPeasant.getXPosition()).add(new Space(location));
		}
		spaces.get(currentPeasant.getXPosition()).get(currentPeasant.getYPosition()).visited = true;
		return middleStep(state);
	}
	
	@Override
	public Map<Integer, Action> middleStep(StateView state) 
	{		
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
		publicState = state;
		if (publicState.getUnit(currentPeasant.getID()) == null)
		{
			if (peasantID.size() > 0)
			{
				peasantID.remove(0);
				if (peasantID.size() <= 0)
				{
					System.out.println("No more peasants");
					return actions;
				}
				currentPeasant = state.getUnit(peasantID.get(0));
				int size = spaces.size();
				for (int j1 = 0; j1 <= currentPeasant.getXPosition() - size; j1++)
				{
					spaces.add(new ArrayList<Space>());
				}
				size = spaces.get(currentPeasant.getXPosition()).size();
				for (int j1 = 0; j1 <= currentPeasant.getYPosition() - size; j1++)
				{
					Vector2D location = new Vector2D(currentPeasant.getXPosition(), spaces.size() + j1);
					spaces.get(currentPeasant.getXPosition()).add(new Space(location));
				}
				spaces.get(currentPeasant.getXPosition()).get(currentPeasant.getYPosition()).visited = true;
			}
			else
			{
				return actions;
			}
		}
		currentPeasant = state.getUnit(peasantID.get(0));
		
		if (path.size() <= 0) //main loop
		{
			if (move == null) //get and make a move
			{
				System.out.println("making move");
				move = getMove();
				actions.put(currentPeasant.getID(), makeMove(move));
			}
			else //check if we were hit
			{
				if (waitCounter == 1)
				{
					System.out.println("checking hit");
					if (checkHit()) //if we were hit
					{
						System.out.println("hit");
						hitList.add(move);
						actions.put(currentPeasant.getID(), makeMove(move.parent));
						addTowers(move);
					}
					else //add node to safe list
					{
						System.out.println("not hit");
						move.visited = true;
						updateTowers(move);		
						move = new Space();
						move = null;
					}
					waitCounter = 0;
				}
				else
					waitCounter++;
				
				openList.remove(move);		
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
		
		if (publicState.getUnit(currentPeasant.getID()) == null)
		{
			peasantID.remove(0);
			System.out.println(peasantID.size());
			return true;
		}
		else if(currentPeasant.getHP() < peasantHealth)
			return true;
		
		return false;
	}

	private Action makeMove(Space move) {
		
		peasantHealth = currentPeasant.getHP();
		System.out.printf("moving to (%s, %s) with health %s\n", move.pos.x, move.pos.y, peasantHealth);
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
		
		ArrayList<Space> returnList = new ArrayList<Space>();
		for (Space space : neighbors)
		{
			if (!space.visited)
				returnList.add(space);
		}
		return returnList;
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
				try {
					spaces.get(tempX);
						
				} catch (Exception e) {
					int size = spaces.size();
					
					for (int j1 = 0; j1 <= tempX - size; j1++)
					{
						spaces.add(new ArrayList<Space>());
					}
				}
				try {
					spaces.get(tempX).get(tempY);
						
				} catch (Exception e) {
					int size = spaces.get(tempX).size();
					
					for (int j1 = 0; j1 <= tempY - size; j1++)
					{
						Vector2D location = new Vector2D(tempX, size + j1);
						spaces.get(tempX).add(new Space(location));
					}
				}
				spaces.get(tempX).get(tempY).parent = spaces.get(x).get(y);
				neighbors.add(spaces.get(tempX).get(tempY));
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
	
	public List<Integer> findUnitType(List<Integer> ids, StateView state, String name)	{

		List<Integer> unitIds = new ArrayList<Integer>();

		for (int x = 0; x < ids.size(); x++)
		{
			Integer unitId = ids.get(x);
			UnitView unit = state.getUnit(unitId);

			if(unit.getTemplateView().getUnitName().equals(name))
			{
				unitIds.add(unitId);
			}
		}

		return unitIds;
	}
}
