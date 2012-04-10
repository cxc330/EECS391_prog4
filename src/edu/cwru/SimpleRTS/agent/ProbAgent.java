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
	static int towerRadius = 4;
	
	int waitCounter = 0;
	int costOfPeasant;
	int peasantHealth = -1;
	int mapSize = 100;
	private List<Integer> peasantID = new ArrayList<Integer>();
	private List<Integer> townHallIds;
	
	//Variables used for searching
	ArrayList<ArrayList<Space>> Map_Representation = new ArrayList<ArrayList<Space>>(); //2D array for spaces
	ArrayList<Space> openList = new ArrayList<Space>();
	ArrayList<Space> hitList = new ArrayList<Space>(); //List of Spaces that resulted in an attack from tower(s)
	ArrayList<Space> path = new ArrayList<Space>(); //List of Spaces that is the path
	ArrayList<Space> towers = new ArrayList<Space>(); //List of Spaces that has towers
	ArrayList<Space> visitedSpaces = new ArrayList<Space>();
	
	Space move = new Space();
	UnitView currentPeasant;
	StateView publicState = null;
	Stack<Space> returnNodes = new Stack<Space>();
	
	//Constructor
	public ProbAgent(int playernum, String[] args) 
	{
		super(playernum);
		for(int i = 0; i < mapSize; i++)
		{
			Map_Representation.add(new ArrayList<Space>());
			for(int j = 0; j < mapSize; j++)
				Map_Representation.get(i).add(new Space(new Vector2D(i,j)));
		}
	}

	//Below is the initialStep and setup for our agent
	@Override
	public Map<Integer, Action> initialStep(StateView state) 
	{	
		peasantID = findUnitType(state.getAllUnitIds(), state, peasant); //getting all peasants
		
		if(peasantID.size() > 0)
		{	
			currentPeasant = state.getUnit(peasantID.get(0));
			costOfPeasant = currentPeasant.getTemplateView().getGoldCost();
			getFromMap(new Vector2D(currentPeasant.getXPosition(), currentPeasant.getYPosition())).visited = true;
			return middleStep(state);
		}
		else
		{
			System.out.println("No Peasants available. Bad Setup.");
			return new HashMap<Integer, Action>();
		}
	}
	
	//Below is the middleStep function where the majority of the logic works
	@Override
	public Map<Integer, Action> middleStep(StateView state) 
	{		
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
		publicState = state;
		
		if (AllPeasantsAreDead(actions)) //check to see if peasants are dead
			return actions;
		
		currentPeasant = state.getUnit(peasantID.get(0));
		
		if (path.size() <= 0) //there is no path
		{
			if (move == null) //if no next move
			{
				System.out.println("Moving from location: " + currentPeasant.getXPosition() + ", " + currentPeasant.getYPosition());
				move = getMove();
				actions.put(currentPeasant.getID(), makeMove(move));
			}
			else //check if we were hit
			{
				if (waitCounter == 1)
				{
					checkTrees(move);
					System.out.print("checking hit: ");
					if (checkHit()) //if we were hit
					{
						System.out.println("hit");
						hitList.add(move);
						actions.put(currentPeasant.getID(), makeMove(move.parent)); //moving peasant back
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
						//move = new Space();
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
			//spaces.get(publicState.getResourceNode(resource).getXPosition()).get(publicState.getResourceNode(resource).getYPosition()).tree = true;
		}
		
	}
	
	Space getFromMap(Vector2D location)
	{
		int x = location.x;
		int y = location.y;
		
		return Map_Representation.get(x).get(y);
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
				
				//Getting the next peasant, marking its space as visited
				currentPeasant = publicState.getUnit(peasantID.get(0));
				getFromMap(new Vector2D(currentPeasant.getXPosition(), currentPeasant.getYPosition())).visited = true;
				visitedSpaces.add(getFromMap(new Vector2D(currentPeasant.getXPosition(), currentPeasant.getYPosition())));
				Space temp = hitList.get(hitList.size() - 1).parent;
				
				//Make the path to get from the new peasant to the space before the previous peasant died
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

	//This builds a peasant
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
						System.out.println("Added tower " + towerLoc.toString());
					}
				}
			}
			
			markPossibleHitInMap(tower.pos);
		}
		
	}
	
	private void markPossibleHitInMap(Vector2D tower)
	{
		Integer startX = tower.x - towerRadius;
		Integer startY = tower.y - towerRadius;
		int outerLoopCount = towerRadius * 2;
		int innerLoopCount = outerLoopCount;
		if(startX < 0)
		{
			outerLoopCount += startX;
			startX = 0;
		}
		else if((startX + outerLoopCount) > mapSize)
		{
			outerLoopCount -= (startX + outerLoopCount) - mapSize;
		}
		
		if(startY < 0)
		{
			innerLoopCount += startY;
			startY = 0;
		}
		else if((startY + innerLoopCount) > mapSize)
		{
			innerLoopCount -= (startY + innerLoopCount) - mapSize;
		}
		
		for(int i = 0; i < outerLoopCount; i++)
		{
			for(int j = 0; j < innerLoopCount; j++)
			{
				Map_Representation.get(startX + i).get(startY + j).possibleHit = true;
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
		if (returnNodes.size() > 0) //If a peasant die, this makes the next peasant traverse the safe path so far
		{
			if (currentPeasant.getXPosition() == returnNodes.peek().pos.x && currentPeasant.getYPosition() == returnNodes.peek().pos.y )
			{
				System.out.println("moving towards previous peasant");
				returnNodes.pop();
			}
			if (returnNodes.size() > 0)
				return returnNodes.peek();
		}
		
		//get all unvisited valid neighbors
		ArrayList<Space> neighbors = findUnvisitedNeighbors(getNeighbors(currentPeasant));
		
		//If gold mine is a neighbor, we've reached our goal, return null for no move
		if (containsGold(neighbors))
			return null;
		
		addToOpenList(neighbors); //add the valid neighbors to the openlist
		
		Space lowestProbSpace = getLowestProb(openList);
		int lowestProb = Integer.MAX_VALUE;
		
		for (Space neighbor : neighbors) //check if one of our neighbors is the best choice
		{
			if (getProb(neighbor) <= lowestProb) //if it's better or the same as best choice
			{
				lowestProb = getProb(neighbor);
				lowestProbSpace = neighbor;
			}
		}
		
		if(neighbors.size() == 0)
		{
			Vector2D location = new Vector2D(currentPeasant.getXPosition(), currentPeasant.getYPosition());
			return getFromMap(location).parent;
		}
		
		return getFromOL(lowestProbSpace);
	}

	private Space getFromOL(Space neighbor) {
		
		if (openList.contains(neighbor))
		{
			return openList.get(openList.indexOf(neighbor));
		}
		return null;
	}

	private int getProb(Space space) {
		
		Vector2D spaceLoc = space.pos; 
		
		int returnVal = DistanceMetrics.chebyshevDistance(spaceLoc.x, spaceLoc.y, mapSize, 0); // top right corner
		int numTowersWithRadius = 0;
		int numTowers = towers.size();
		
		for (Space tower : towers)
		{
			if (withinTowerRadius(space, tower))
				numTowersWithRadius++;
		}
		
		/*if (numTowers > 0)
			return (numTowersWithRadius * numTowers);// * DistanceMetrics.chebyshevDistance(spaceLoc.x, spaceLoc.y, 50, 0);
		else
			return DistanceMetrics.chebyshevDistance(spaceLoc.x, spaceLoc.y, 50, 0);
		*/
		
		Space temp = Map_Representation.get(space.pos.x).get(space.pos.y);
		
		if(temp.possibleHit)
			returnVal += 100;
		
		if(temp.visited)
			returnVal += 150;
		
		return returnVal;
		
				
	}

	//returns the lowest space with the lowest probability, lowest probability being most optimal
	private Space getLowestProb(ArrayList<Space> spaces) {
		
		int lowestProb = Integer.MAX_VALUE;
		Space lowestSpace = null;
		int tempProb = -1;
		for (Space space : spaces)
		{	
			tempProb = getProb(space);
			System.out.println("For space (" + space.pos.x + ", " + space.pos.y + "), tempProb: " + tempProb);
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
				case 0: //x + 1, y + 1
					tempX = xPlusOne;
					tempY = yPlusOne;
					break;
				case 1: //x + 1, y
					tempX = xPlusOne;
					tempY = y;
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

			if(isValidNeighbor(tempX, tempY)) //check if it's a valid space
			{			
				System.out.println("VALIDtemp: " + tempX + ", " + tempY);
				Vector2D location = new Vector2D(tempX, tempY);
				
				if (getFromMap(location).parent == null && getFromMap(location).visited == false)
				{
					getFromMap(location).parent = getFromMap(new Vector2D(x,y));
				}
				
				neighbors.add(Map_Representation.get(tempX).get(tempY));
			}
			else
			{
				System.out.println("temp: " + tempX + ", " + tempY);
			}
		}
		
		//Test Printing of valid neighbors returned
		for(int i = 0; i < neighbors.size(); i++)
			System.out.println("Neighbor " + i + ": " + neighbors.get(i).pos.x + ", " + neighbors.get(i).pos.y);
		
		return neighbors;
	}
	
	//This checks to see if a neighbor is a valid neighbor
	private boolean isValidNeighbor(Integer x, Integer y)
	{	
		boolean NeighborIsUnit = publicState.isUnitAt(x, y);
		boolean NeighborIsValid = publicState.inBounds(x, y);
		boolean NeighborIsResource = publicState.isResourceAt(x, y);
		return ((!NeighborIsUnit && !NeighborIsResource) && NeighborIsValid);
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
