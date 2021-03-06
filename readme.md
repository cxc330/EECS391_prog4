#EECS391 Programming Assignment 4

Developed By: Chien-Hung Chen and Christopher Gross

Case IDs: cxc330 and cjg28

Last update: 4/10/2012

Github Link: https://github.com/cxc330/EECS391_prog4

In this assignment we will use probabilistic reasoning to solve a scouting/resource collection scenario in the SimpleRTS game.

##1. How To Run
###Using Shell Scripts (The Easy Way)
We have included a few shell scripts for your pleasure.
Please run the following to run our agent:
	./19x25.sh
	./32x32.sh
	
The following is to run just the demo map:
	./19x25demo.sh
	./32x32demo.sh

To clean the .class files up:
	./clean.sh

###Using Commands
To compile:
	javac -cp 'lib/SimpleRTSv3.1.jar' src/edu/cwru/SimpleRTS/agent/ProbAgent.java src/edu/cwru/SimpleRTS/agent/Vector2D.java src/edu/cwru/SimpleRTS/agent/Space.java
	
To Run:
	java -cp lib/SimpleRTSv3.1.jar:src edu.cwru.SimpleRTS.Main --config data/midasConfig.xml data/scout_19x25.map --agent  edu.cwru.SimpleRTS.agent.ProbAgent 0 --agent edu.cwru.SimpleRTS.agent.visual.VisualAgent 0 --agentparam true --agentparam true --agent  TowerAgent 1
	
Where the data/scout_19x25.map can be changed for the map needed.

##2. Problem Setup
The scenarios we will solve are built around the “scout_19x25.map”  and “scout_32x32.map” maps in SimpleRTS and the “pa4Config” configuration file. In these maps, we start with a townhall and three peasants in the lower left corner. In the upper right corner there is a goldmine. The goal in this scenario is to collect 2000 gold. However, this map is only partially observable to our units. Somewhere hidden on the map are several enemy Towers. Towers are immobile units that shoot arrows at any unit in 
their  attack range.  If while moving our peasants come to close to a tower they will be shot and eventually die. (We can build more peasants if we have collected some gold.) Our goal is to collect the 2000 gold while losing as few peasants as possible (essentially by discovering where the towers are hidden and then avoiding them.)

To do this, our agent should maintain a probability distribution over each cell that describes the chance that it has a Tower. As our peasants move around, they collect observations. Cells that become visible and do not contain towers immediately will have probabilities of zero of having a Tower, and likewise, visible cells containing towers have probability one. If a peasant moves to a cell and does not get shot, there is no Tower nearby. On the other hand, if it does get shot, there is a Tower nearby (note that a Tower’s attack range (4) is larger than a peasant’s sight radius so a peasant can be shot without being able to identify the location of the shooting unit). Further, Towers are rare: most cells do not have Towers. However, cells with forests are quite likely to have a Tower nearby somewhere.At each step, our agent will have a posterior probability distribution that tells the agent how likely each cell is to have a tower. For each location to which it is possible to move, our agent needs to balance the risk of being shot by a Tower if it moves there (note that this is not the probability that the cell contains a Tower)  to the benefit of being closer to the goldmine to collect gold, and pick a cell to move to that offers the best tradeoff. We can write this objective function as you see fit. Note that we may also want a term for rewarding a visit to a cell that may not  be  “closer” to the goldmine but will allow us to pinpoint a Tower’s location. By repeatedly choosing the best location according to this objective, and updating the probability density as new observations are collected, our agent should be able to find a safe path to the goldmine. There are at least two completely safe paths in each map.

Two additional maps are provided in the zipfile (named *demo.map) that show you the actual locations of the Towers in the provided maps, so that you can verify that your posterior probability estimates are accurate and your agent is trying to do the right thing. The cells marked “B” in these maps are the danger zones within the Towers’ attack range. Do not, however, tailor your code to these maps/Tower locations (e.g., by simply always choosing the safe paths from the start) as we will run your code with other maps.From the zipfile, use TowerAgent to control the opponent (Towers). Place the unit_templates file in data/, overwriting your existing unit_templates file. One minor issue is that the version of SimpleRTS.jar you are using has a bug in VisualAgent such that if you run VisualAgent and your peasants die, VisualAgent might throw an exception. This is irrelevant to the assignment however, so we are not going to update the jar file at the moment. You can continue to run the game even with the exception (it is not fatal), or restart.

