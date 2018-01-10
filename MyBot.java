import hlt.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static java.util.stream.Collectors.toList;

public class MyBot {

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Tamagocchi");

        // We now have 1 full minute to analyse the initial map.
        final String initialMapIntelligence =
                "width: " + gameMap.getWidth() +
                "; height: " + gameMap.getHeight() +
                "; players: " + gameMap.getAllPlayers().size() +
                "; planets: " + gameMap.getAllPlanets().size();
        Log.log(initialMapIntelligence);

        // Initialize
	    int width = gameMap.getWidth();
	    int height = gameMap.getHeight();
	    final HashMap<Integer, Position> cornerShip = new HashMap<>();
	    HashMap<Integer, Position> fourCorners = new HashMap<>();
	    fourCorners.put(0, new Position(0,0));
	    fourCorners.put(1, new Position(0,height));
	    fourCorners.put(2, new Position(width,height));
	    fourCorners.put(3, new Position(width,0));
        final ArrayList<Move> moveList = new ArrayList<>();
        final HashMap<Integer, Position> shipTargetsDocked = new HashMap<>();
        final HashMap<Integer, Ship> shipTargets = new HashMap<>();
        final HashMap<Integer, Position> planetTargets = new HashMap<>();
        final HashMap<Integer, Integer> planetTag = new HashMap<>();
        final HashMap<Integer, Ship> attackShips = new HashMap<>();
        int myId = gameMap.getMyPlayer().getId();
        int minAttackShips = 4 - gameMap.getAllPlayers().size();
        final List<Player> enemyList = gameMap.getAllPlayers().stream()
                .filter(p -> p.getId() != myId)
                .collect(toList());
        int attackingShipsCount = 0;
        int currentPlayerTarget = 0;
        int corShip = 0;

        for (;;) {
            moveList.clear();
            networking.updateMap(gameMap);
//            boolean fullHouse = false;
//            if(!fullHouse){
//                fullHouse = fullHouse(gameMap);
//            }
            boolean fullHouse = fullhouse(gameMap);
            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
	            if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
		            continue;
	            }

            	if (gameMap.getMyPlayer().getShips().size() >= 10) {
            		if(cornerShip.size() == 0){
			            Position target = nearestCorner(fourCorners, gameMap, ship);
			            cornerShip.put(ship.getId(), target);
			            corShip = ship.getId();
			            final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(
					            gameMap,
					            ship,
					            target,
					            Constants.MAX_SPEED,
					            true,
					            Constants.MAX_NAVIGATION_CORRECTIONS,
					            Math.PI/180.0
			            );
			            if (newThrustMove != null){
				            moveList.add(newThrustMove);
			            }
			            continue;
		            } else {
            			if(gameMap.getShip(myId, corShip) == null){
            				cornerShip.remove(corShip);
				            Position target = nearestCorner(fourCorners, gameMap, ship);
				            cornerShip.put(ship.getId(), target);
				            corShip = ship.getId();
				            final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(
						            gameMap,
						            ship,
						            target,
						            Constants.MAX_SPEED,
						            true,
						            Constants.MAX_NAVIGATION_CORRECTIONS,
						            Math.PI/180.0
				            );
				            if (newThrustMove != null){
					            moveList.add(newThrustMove);
				            }
				            continue;
			            } else {
            				if(ship.getId() == corShip){
					            final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(
							            gameMap,
							            ship,
							            cornerShip.get(corShip),
							            Constants.MAX_SPEED,
							            true,
							            Constants.MAX_NAVIGATION_CORRECTIONS,
							            Math.PI/180.0
					            );
					            if (newThrustMove != null){
						            moveList.add(newThrustMove);
					            }
					            continue;
				            }
			            }
		            }

            	}
                //if all planets are taken change strat
                if(fullHouse == true){
                    planetTargets.clear();
                    //if ship has no target
                    if(shipTargetsDocked.get(ship.getId()) == null) {
                    	if(ship.getId() == corShip) {
		                    continue;
	                    }
                        //find the closest enemy planet and target docked ships
                        Position newTarget = findTarget(gameMap, ship, myId, fullHouse);
                        shipTargetsDocked.put(ship.getId(), newTarget);
                        final ThrustMove attackDockedShips = Navigation.navigateShipTowardsTarget(
                                gameMap,
                                ship,
                                newTarget,
                                Constants.MAX_SPEED,
                                true,
                                Constants.MAX_NAVIGATION_CORRECTIONS,
                                Math.PI/180.0
                        );
                        if(attackDockedShips != null) {
                            moveList.add(attackDockedShips);
                        }
                    } else {
                        //if ship has reached target
                        if(reachedShip(ship, shipTargetsDocked.get(ship.getId()))){
                            //find new target
                            Position newTarget = findTarget(gameMap, ship, myId, fullHouse);
                            shipTargetsDocked.remove(ship.getId());
                            shipTargetsDocked.put(ship.getId(), newTarget);
                            final ThrustMove attackDockedShips = Navigation.navigateShipTowardsTarget(
                                    gameMap,
                                    ship,
                                    newTarget,
                                    Constants.MAX_SPEED,
                                    true,
                                    Constants.MAX_NAVIGATION_CORRECTIONS,
                                    Math.PI/180.0
                            );
                            if(attackDockedShips != null) {
                                moveList.add(attackDockedShips);
                            }
                        } else {
                            //continue attack
                            final ThrustMove attackDockedShips = Navigation.navigateShipTowardsTarget(
                                    gameMap,
                                    ship,
                                    shipTargetsDocked.get(ship.getId()),
                                    Constants.MAX_SPEED,
                                    true,
                                    Constants.MAX_NAVIGATION_CORRECTIONS,
                                    Math.PI/180.0
                            );
                            if(attackDockedShips != null) {
                                moveList.add(attackDockedShips);
                            }
                        }
                    }
                } else {
                    if(attackingShipsCount < minAttackShips){
                        int enemyId = enemyList.get(0).getId();
                        Ship enemyShip = targetAEnemyShip(gameMap, ship, enemyId);
                        shipTargets.put(ship.getId(), enemyShip);
                        attackShips.put(ship.getId(), ship);
                        attackingShipsCount++;
                    }
                    if(attackShips.get(ship.getId()) != null) {
                        int enemyId = enemyList.get(0).getId();
                        Ship enemyShip = targetAEnemyShip(gameMap, ship, enemyId);
                        shipTargets.put(ship.getId(), enemyShip);
                        int enemyShipId = shipTargets.get(ship.getId()).getOwner();
                        int shipId = shipTargets.get(ship.getId()).getId();
                        Ship targetShip = gameMap.getShip(enemyShipId, shipId);

                        final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(
                                gameMap,
                                ship,
                                ship.getClosestPoint(targetShip),
		                        Constants.MAX_SPEED,
                                true,
                                Constants.MAX_NAVIGATION_CORRECTIONS,
                                Math.PI/180.0
                        );
                        Log.log("hi");
                        if (newThrustMove != null){
                            moveList.add(newThrustMove);
                        }
                    } else {
                        //if ship has no docking target
                        if(planetTargets.get(ship.getId()) == null) {
                            //find closest planet
                            Integer targetPlanetId = identifyPlanet(gameMap, ship, myId, fullHouse);
                            Position targetPlanet = findPlanet(gameMap, targetPlanetId, ship);
                            planetTargets.put(ship.getId(), targetPlanet);
                            planetTag.put(ship.getId(), targetPlanetId);
                            //move to planet
                            final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(
                                    gameMap,
                                    ship,
                                    targetPlanet,
                                    Constants.MAX_SPEED,
                                    true,
                                    Constants.MAX_NAVIGATION_CORRECTIONS,
                                    Math.PI/180.0
                            );
                            if (newThrustMove != null) {
                                moveList.add(newThrustMove);
                            }
                        } else {
                            //is ship close enought to dock?
                            if(ship.canDock(gameMap.getPlanet(planetTag.get(ship.getId())))){
                                Planet reachedPlanet = gameMap.getPlanet(planetTag.get(ship.getId()));
                                //are their docking spots?
                                if(reachedPlanet.getDockingSpots() > reachedPlanet.getDockedShips().size()){
                                    //if so dock with planet and remove it from planet targets
                                    moveList.add(new DockMove(ship, gameMap.getPlanet(planetTag.get(ship.getId()))));
                                    planetTag.remove(ship.getId());
                                    planetTargets.remove(ship.getId());
                                } else {
                                    //if no docking spots: find closest unclaimed planet and fly there
                                    Integer targetPlanetId = identifyPlanet(gameMap, ship, myId, fullHouse);
                                    Position targetPlanet = findPlanet(gameMap, targetPlanetId, ship);
                                    planetTargets.put(ship.getId(), targetPlanet);
                                    planetTag.put(ship.getId(), targetPlanetId);
                                    final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(
                                            gameMap,
                                            ship,
                                            targetPlanet,
                                            Constants.MAX_SPEED,
                                            true,
                                            Constants.MAX_NAVIGATION_CORRECTIONS,
                                            Math.PI/180.0
                                    );
                                    if (newThrustMove != null) {
                                        moveList.add(newThrustMove);
                                    }
                                }
                            } else {
                                //continue moves
                                final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(
                                        gameMap,
                                        ship,
                                        planetTargets.get(ship.getId()),
                                        Constants.MAX_SPEED,
                                        true,
                                        Constants.MAX_NAVIGATION_CORRECTIONS,
                                        Math.PI/180.0
                                );
                                if (newThrustMove != null) {
                                    moveList.add(newThrustMove);
                                }
                            }
                        }
                    }


                }
            }
            Log.log("Number of PlanetTargets:" + Integer.toString(planetTargets.size()));
            Log.log("Size of MoveList:" + Integer.toString(moveList.size()));
            Log.log("Number of Ships:" + Integer.toString(gameMap.getMyPlayer().getShips().size()));
            Networking.sendMoves(moveList);
        }
    }

    public static Integer closestPlanet(GameMap gameMap, Ship ship, int myId, boolean attack) {
        double min = gameMap.getWidth();
        if(gameMap.getHeight() > min) {
            min = gameMap.getHeight();
        }

        Integer planetId = 0;

        for(final Planet planet : gameMap.getAllPlanets().values()) {
            if (attack == true) {
                if(planet.getOwner() != myId && planet.getOwner() != -1) {
                    double temp = Math.hypot(ship.getXPos() - planet.getXPos(), ship.getYPos() - planet.getYPos());
                    if (min > temp) {
                        min = temp;
                        planetId = planet.getId();
                    }
                }
            } else {
                if(planet.getOwner() == myId || planet.getOwner() == -1) {
                    if(planet.getDockingSpots() > planet.getDockedShips().size()) {
                        double temp = Math.hypot(ship.getXPos() - planet.getXPos(), ship.getYPos() - planet.getYPos());
                        if (min > temp) {
                            min = temp;
                            planetId = planet.getId();
                        }
                    }
                }
            }
        }
        return planetId;
    }

    public static boolean fullhouse(GameMap gameMap){
        boolean result = true;
        for(final Planet planet : gameMap.getAllPlanets().values()) {
            if(planet.isOwned() == false){
                result = false;
                break;
            }
        }
        return result;
    }

    public static Position findTarget(GameMap gameMap, Ship ship, int myId, boolean fullHouse) {
        Integer planetId = closestPlanet(gameMap, ship, myId, fullHouse);
        Planet targetPlanet = gameMap.getPlanet(planetId);
        if(fullHouse == false) {
            return ship.getClosestPoint(targetPlanet);
        }
        Integer targetPlayerId = targetPlanet.getOwner();
        List<Integer> shipList = gameMap.getPlanet(planetId).getDockedShips();
        Integer enemyShipId = shipList.get(shipList.size() - 1);
        Ship enemyShip = gameMap.getShip(targetPlayerId, enemyShipId);
        Position enemyShipPosition = ship.getClosestPoint(enemyShip);
        return enemyShipPosition;
    }

    public static Integer identifyPlanet(GameMap gameMap, Ship ship, int myId, boolean fullHouse){
        Integer planetId = closestPlanet(gameMap, ship, myId, fullHouse);
        return planetId;
    }

    public static Position findPlanet(GameMap gameMap, int planetId, Ship ship) {
        Planet targetPlanet = gameMap.getPlanet(planetId);
        Position target = ship.getClosestPoint(targetPlanet);
        return target;
    }

    public static boolean reachedPlanet(Ship ship, Entity target){
        double radius = target.getRadius();
        double distance = target.getDistanceTo(ship);
        if(distance < radius){
            return true;
        }
        return false;
    }

    public static boolean reachedShip(Ship ship, Position position){
        double radius = 0.75;
        double distance = ship.getDistanceTo(position);
        if(distance < radius) {
            return true;
        }
        return false;
    }

    public static Ship targetAEnemyShip(GameMap gameMap, Ship ship, int enemyId) {
        int enemyShipId = gameMap.getPlayer(enemyId).getShips().keySet().toArray(new Integer[gameMap.getPlayer(enemyId).getShips().keySet().size()])[0];
        return gameMap.getShip(enemyId, enemyShipId);
    }

    public static Position nearestCorner(HashMap<Integer, Position> fourCorners, GameMap gameMap, Ship ship) {
	    double min = gameMap.getWidth();
	    if(gameMap.getHeight() > min) {
		    min = gameMap.getHeight();
	    }
	    Position result = new Position(0,0);
	    double cX = ship.getXPos();
	    double cY = ship.getYPos();
	    for(final Position corner : fourCorners.values()) {
		    double temp = Math.hypot(cX - corner.getXPos(), cY - corner.getYPos());
		    if (min > temp) {
			    min = temp;
			    result = corner;
		    }
	    }
        return result;
    }
}