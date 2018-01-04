import hlt.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

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
        final ArrayList<Move> moveList = new ArrayList<>();
        final HashMap<Integer, Position> shipTargets = new HashMap<>();
        final HashMap<Integer, Position> planetTargets = new HashMap<>();
        final HashMap<Integer, Integer> planetTag = new HashMap<>();

        int myId = gameMap.getMyPlayer().getId();
        for (;;) {
            moveList.clear();
            networking.updateMap(gameMap);
            boolean fullHouse = fullhouse(gameMap);
            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    continue;
                }
                if(fullHouse == true){
                    planetTargets.clear();
                    if(shipTargets.get(ship.getId()) == null) {
                        Position newTarget = findTarget(gameMap, ship, myId, fullHouse);
                        shipTargets.put(ship.getId(), newTarget);
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
                        if(reachedShip(ship, shipTargets.get(ship.getId()))){
                            Position newTarget = findTarget(gameMap, ship, myId, fullHouse);
                            shipTargets.remove(ship.getId());
                            shipTargets.put(ship.getId(), newTarget);
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
                            final ThrustMove attackDockedShips = Navigation.navigateShipTowardsTarget(
                                    gameMap,
                                    ship,
                                    shipTargets.get(ship.getId()),
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
                    if(planetTargets.get(ship.getId()) == null) {
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
                    } else {
                        if(ship.canDock(gameMap.getPlanet(planetTag.get(ship.getId())))){
                            Planet reachedPlanet = gameMap.getPlanet(planetTag.get(ship.getId()));
                            if(reachedPlanet.getDockingSpots() > reachedPlanet.getDockedShips().size()){
                                moveList.add(new DockMove(ship, gameMap.getPlanet(planetTag.get(ship.getId()))));
                                planetTag.remove(ship.getId());
                                planetTargets.remove(ship.getId());
                            } else {
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
}