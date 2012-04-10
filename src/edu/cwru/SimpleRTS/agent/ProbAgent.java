package edu.cwru.SimpleRTS.agent;

import java.util.*;

import edu.cwru.SimpleRTS.action.*;
import edu.cwru.SimpleRTS.environment.State.StateView;
import edu.cwru.SimpleRTS.model.Template.TemplateView;
import edu.cwru.SimpleRTS.model.resource.ResourceNode.Type;
import edu.cwru.SimpleRTS.model.resource.ResourceType;
import edu.cwru.SimpleRTS.model.unit.Unit.UnitView;
import edu.cwru.SimpleRTS.util.DistanceMetrics;

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
	static int towerRadius = 4;
	
	/*
	 * Variables used for searching
	 */
	ArrayList<Space> openList = new ArrayList<Space>();
	ArrayList<Space> hitList = new ArrayList<Space>(); //List of Spaces that resulted in an attack from tower(s)
	ArrayList<Space> path = new ArrayList<Space>(); //List of Spaces that is the path
	ArrayList<ArrayList<Space>> spaces = new ArrayList<ArrayList<Space>>(); //2D array for spaces
	ArrayList<Space> towers = new ArrayList<Space>(); //List of Spaces that has towers
	ArrayList<Space> visitedSpaces = new ArrayList<Space>();
	Space move = new Space();
	int peasantHealth = -1;
	UnitView currentPeasant;
	StateView publicState = null;
	Stack<Space> returnNodes = new Stack<Space>();
	int costOfPeasant;
	
	public ProbAgent(int playernum, String[] args) 
	{
		super(playernum);
	}

	@Override
	public Map<Integer, Action> initialStep(StateView state) 
	{	
		peasantID = findUnitType(state.getAllUnitIds(), state, peasant);
		if(peasantID.size() > 0)
		{	
			currentPeasant = state.getUnit(peasantID.get(0));
			int size = spaces.size();
			for (int j1 = 0; j1 <= currentPeasant.getXPosition() - size; j1++) //instantiating the 2d arraylist
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
			//(spaces.get(currentPeasant.getXPosition()).get(currentPeasant.getYPosition()));
			costOfPeasant =  state.getUnit(peasantID.get(0)).getTemplateView().getGoldCost();
			return middleStep(state);
		}
		else
		{
			System.out.println("No Peasants available. Bad Setup.");
			return new HashMap<Integer, Action>();
		}
	}
	
	@Override
	public Map<Integer, Action> middleStep(StateView state) 
	{		
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
		publicState = state;
		
		if (AllPeasantsAreDead(actions)) //check to see if peasants are dead
		{
			return actions;
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
					checkTrees(move);
					System.out.println("checking hit");
					if (checkHit()) //if we were hit
					{
						System.out.println("hit");
						hitList.add(move);
						actions.put(currentPeasant.getID(), makeMove(move.parent));
						move.visited = true;
						visitedSpaces.add(move);
						addTowers(move);
					}
					else //add node to safe list
					{
						System.out.println("not hit");
						move.visited = true;
						//TODO: JEFF figure out why it's null
						if(move.pos.x != null)
						{
							visitedSpaces.add(move);
						}
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

	private void checkTrees(Space move) {
		List<Integer> resources = publicState.getAllResourceIds();
		
		for (Integer resource : resources)
		{
			spaces.get(publicState.getResourceNode(resource).getXPosition()).get(publicState.getResourceNode(resource).getYPosition()).tree = true;
		}
		
	}

	//returns true if all peasants are dead, else return false
	private boolean AllPeasantsAreDead(Map<Integer, Action> actions) {
		if (publicState.getUnit(currentPeasant.getID()) == null)
		{
			if (peasantID.size() > 0)
			{
				peasantID.remove(0);
				if (peasantID.size() <= 0)
				{
					System.out.println("No more peasants.");
					
						buildPeasant(actions);
						return true;
				}
				currentPeasant = publicState.getUnit(peasantID.get(0));
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
				visitedSpaces.add(spaces.get(currentPeasant.getXPosition()).get(currentPeasant.getYPosition()));
				Space temp = hitList.get(hitList.size() -1).parent;
				
				while (temp.parent != null)
				{
					returnNodes.push(temp);
					temp = temp.parent;
				}
			}
			else
			{
				return true;
			}
		}
		return false;
	}

	private boolean buildPeasant( Map<Integer, Action> actions) {
		
		if (publicState.getResourceAmount(0, ResourceType.GOLD) >= costOfPeasant)
		{
			TemplateView peasantTemplate = publicState.getTemplate(playernum, peasant);
			Action buildPeasants = Action.createCompoundProduction(townHallIds.get(0), peasantTemplate.getID());
			actions.put(townHallIds.get(0), buildPeasants);
			System.out.println("Building new peasant");
			return true;
		}
		return false;
	}

	@Override
	public void terminalStep(StateView state) {}
	
	private void traverse(ArrayList<Space> path) {
		// TODO traverse to next node
		
	}

	private void updateTowers(Space move) {
		
		ArrayList<Space> tempList = new ArrayList<Space>();
		
		for (Space tower : towers)
		{
			Vector2D towerLoc = tower.pos;
			Vector2D moveLoc = move.pos;
			
			int distance = DistanceMetrics.chebyshevDistance(towerLoc.x, towerLoc.y, moveLoc.x, moveLoc.y);
			
			
			//TODO: JEFF NEEDS TO GRAB LOCATION FROM spaces... spaces should gather location on trees/resources and mark as such
			boolean isUnit = publicState.isUnitAt(towerLoc.x, towerLoc.y);
			boolean isResource = publicState.isResourceAt(towerLoc.x, towerLoc.y);
			boolean isValid = publicState.inBounds(towerLoc.x, towerLoc.y);
			
			if (distance <= towerRadius)
			{
				if (!isUnit && !isResource && isValid) //check it can exist there 
				{
					tempList.add(tower);
				}
			}
			else
			{
				System.out.println("Removed tower at " + towerLoc.toString());
			}
		}
		
		towers.clear();
		towers.addAll(tempList);
	}

	private void addTowers(Space move) {
		
		ArrayList<Space> possibleTowers = getPossibleTowers(move);
		
		//remove towers that are not possible because we haven't been hit in that square within radius of it
		for (Space tower : possibleTowers)
		{
			Vector2D towerLoc = tower.pos;
			
			for (Space visited : visitedSpaces)
			{
				Vector2D visitedLoc = visited.pos;
				Integer distance = DistanceMetrics.chebyshevDistance(towerLoc.x, towerLoc.y, visitedLoc.x, visitedLoc.y);
				
				boolean isUnit = publicState.isUnitAt(towerLoc.x, towerLoc.y);
				boolean isResource = publicState.isResourceAt(towerLoc.x, towerLoc.y);
				boolean isValid = publicState.inBounds(towerLoc.x, towerLoc.y);
				
				if (distance <= towerRadius)
				{
					if (!isUnit && !isResource && isValid && !towers.contains(tower)) //check it can exist there 
					{
						towers.add(tower);
						System.out.println("added tower " + towerLoc.toString());
					}
				}
			}
		}
	}
	
	private boolean withinTowerRadius(Space move, Space tower)
	{
		Integer distance = DistanceMetrics.chebyshevDistance(tower.pos.x, tower.pos.y, move.pos.x, move.pos.y);
		
		if (distance <= towerRadius)
			return true;
		
		return false;
	}

	private ArrayList<Space> getPossibleTowers(Space move) {
		
		ArrayList<Space> possibleTowers = new ArrayList<Space>();
		
		Vector2D moveLoc = move.pos;
		
		Integer yMin = moveLoc.y - towerRadius;
		Integer yMax = moveLoc.y + towerRadius;
		
		Integer xMin = moveLoc.x - towerRadius;
		Integer xMax = moveLoc.x + towerRadius;
		
		//from xMin, yMin -> xMax, yMin		
		for (int x = 0; x < xMax - xMin; x++)
		{
			Integer xTemp = xMin + x;
			Space tower = new Space(new Vector2D(xTemp, yMin));
			possibleTowers.add(tower);
		}
		//from xMax, yMin -> xMax, yMax		
		for (int x = 0; x < yMax - yMin; x++)
		{
			Integer yTemp = yMin + x;
			Space tower = new Space(new Vector2D(xMax, yTemp));
			possibleTowers.add(tower);
		}
		//from xMax, yMax -> xMin, yMax		
		for (int x = 0; x < xMax - xMin; x++)
		{
			Integer xTemp = xMax - x;
			Space tower = new Space(new Vector2D(xTemp, yMax));
			possibleTowers.add(tower);
		}
		//from xMin, yMax -> xMin, yMin		
		for (int x = 0; x < yMax - yMin; x++)
		{
			Integer yTemp = yMax - x;
			Space tower = new Space(new Vector2D(xMin, yTemp));
			possibleTowers.add(tower);
		}		
		
		return possibleTowers;
	}

	//This checks to see if the peasant has been hit and returns true for hit and false for no hit.
	private boolean checkHit() {
		
		if (publicState.getUnit(currentPeasant.getID()) == null) //if the peasant has been removed, it has been killed
		{
			peasantID.remove(0);
			System.out.println(peasantID.size());
			return true;
		}
		else if(currentPeasant.getHP() < peasantHealth)
			return true;
		
		return false;
	}

	//This makes the peasant move to the position we've determined to be most optimal
	private Action makeMove(Space move) {
		peasantHealth = currentPeasant.getHP(); //storing the health before the move
		System.out.printf("moving to (%s, %s) with health %s\n", move.pos.x, move.pos.y, peasantHealth);
		return Action.createCompoundMove(currentPeasant.getID(), move.pos.x, move.pos.y);
	}
	
	/*
	 * This method will get a move and return the move
	 * returns null on Gold find
	 */
	private Space getMove() 
	{
		if (returnNodes.size() > 0)
		{
			if (currentPeasant.getXPosition() == returnNodes.peek().pos.x && currentPeasant.getYPosition() == returnNodes.peek().pos.y )
			{
				System.out.println("moving towards previous peasant");
				returnNodes.pop();
			}
			if (returnNodes.size() > 0)
				return returnNodes.peek();
		}
		ArrayList<Space> neighbors = findUnvisitedNeighbors(getNeighbors(currentPeasant)); //get all neighbors then parse out all ready visited neighbors
		
		if (containsGold(neighbors)) //Checks for a gold node, if found, that means no more moves to check
			return null;
		
		addToOpenList(neighbors); //add the valid neighbors to the Open List
		
		Space lowestProbSpace = getLowestProb(openList);
		int lowestProb = getProb(lowestProbSpace, openList);
		
		for (Space neighbor : neighbors) //check if one of our neighbors is the best choice
		{
			if (getProb(neighbor, neighbors) <= lowestProb) //if it's better or the same as best choice
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

	private int getProb(Space space, ArrayList<Space> spaces) {
		
		//Vector2D peasantLoc = new Vector2D(currentPeasant.getXPosition(), currentPeasant.getYPosition());
		
		int numTowersWithRadius = 0;
		int numTowers = towers.size();
		
		for (Space tower : towers)
		{
			if (withinTowerRadius(space, tower))
				numTowersWithRadius++;
		}
		
		Vector2D spaceLoc = space.pos; 
		
		if (numTowers > 0)
			return (numTowersWithRadius * numTowers);// * DistanceMetrics.chebyshevDistance(spaceLoc.x, spaceLoc.y, 50, 0);
		else
			return DistanceMetrics.chebyshevDistance(spaceLoc.x, spaceLoc.y, 50, 0);
		
				
	}

	//returns the lowest space with the lowest probability, lowest probability being most optimal
	private Space getLowestProb(ArrayList<Space> spaces) {
		
		int lowestProb = Integer.MAX_VALUE;
		Space lowestSpace = null;
		int tempProb = -1;
		for (Space space : spaces)
		{	
			tempProb = getProb(space, spaces);
			if (tempProb <= lowestProb)
			{
				lowestSpace = space;
				lowestProb = tempProb;
			}
		}
		
		return lowestSpace;
	}

	//add the neighbors found to the OpenList
	private void addToOpenList(ArrayList<Space> spaces) {
		
		for (Space space : spaces)
		{
			if (!openList.contains(space))
			{
				openList.add(space);
			}
		}
		
	}

	//returns true if one of the neighbors is the goldmine, else false
	private boolean containsGold(ArrayList<Space> neighbors) {
		
		for (Space space : neighbors)
		{
			if (space.gold)
				return true;
		}
		return false;
	}

	//this returns the list of the neighbors that have not been visited
	private ArrayList<Space> findUnvisitedNeighbors(ArrayList<Space> neighbors) {
		
		ArrayList<Space> unvisitedNeighbors = new ArrayList<Space>();
		for (Space space : neighbors)
		{
			if (!space.visited)
				unvisitedNeighbors.add(space);
		}
		return unvisitedNeighbors;
	}

	//This gets all of the neighbors of the peasant's current location.
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
				case 1: //x + 1, y
					tempX = xPlusOne;
					tempY = y;
					break;
				case 0: //x + 1, y + 1
					tempX = xPlusOne;
					tempY = yPlusOne;
					break;
				case 2: //x + 1, y - 1
					tempX = xPlusOne;
					tempY = yMinusOne;
					break;
				case 4: //x, y + 1
					tempX = x;
					tempY = yPlusOne;
					break;
				case 3: //x, y - 1
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
				if (spaces.get(tempX).get(tempY).parent == null && spaces.get(tempX).get(tempY).visited == false)
				{
					spaces.get(tempX).get(tempY).parent = spaces.get(x).get(y);
				}
				neighbors.add(spaces.get(tempX).get(tempY));
			}
		}		
		return neighbors;
	}
	
	//This checks to see if a neighbor is a valid neighbor
	private boolean checkValidNeighbor(Integer x, Integer y)
	{	
		boolean isUnit = publicState.isUnitAt(x, y);
		boolean isValid = publicState.inBounds(x, y);
		boolean isResource = publicState.isResourceAt(x, y);
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
