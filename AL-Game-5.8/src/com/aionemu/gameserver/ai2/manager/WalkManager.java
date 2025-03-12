/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 *  Aion-Lightning is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Aion-Lightning is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details. *
 *  You should have received a copy of the GNU General Public License
 *  along with Aion-Lightning.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.ai2.manager;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.walker.RouteStep;
import com.aionemu.gameserver.model.templates.walker.WalkerTemplate;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.World;

import com.aionemu.gameserver.path.Cell;
import com.aionemu.gameserver.path.Pathfinder;

/**
 * @author ATracer
 */
public class WalkManager {
	private static final Logger log = LoggerFactory.getLogger(WalkManager.class);
	private static final int WALK_RANDOM_RANGE = 5;

	static {
		log.info("[WalkManager] Pathfinder.setGridStepping.");
		Pathfinder.setGridStepping(Pathfinder.DIAGONAL_NEIGHBORS, AIConfig.PATHFINDING_STEPS);
	}

	/**
	 * @param npcAI
	 */
	public static boolean startWalking(NpcAI2 npcAI) {
		//log.info("[WalkManager] startWalking");
		npcAI.setStateIfNot(AIState.WALKING);
		Npc owner = npcAI.getOwner();
		WalkerTemplate template = DataManager.WALKER_DATA.getWalkerTemplate(owner.getSpawn().getWalkerId());
		if (template != null) {
			npcAI.setSubStateIfNot(AISubState.WALK_PATH);
			startRouteWalking(npcAI, owner, template);
		}
		else {
			//log.info("[WalkManager] startRandomWalking");
			return startRandomWalking(npcAI, owner);
		}
		return true;
	}

	public static boolean startRouteWalking(NpcAI2 npcAI, WalkerTemplate template) {
		npcAI.setStateIfNot(AIState.WALKING);
		Npc owner = npcAI.getOwner();

		if (template != null) {
			npcAI.setSubStateIfNot(AISubState.WALK_PATH);
			startRouteWalking(npcAI, owner, template);
		}
		else {
			return startRandomWalking(npcAI, owner);
		}
		return true;
	}

	public static boolean startPathWalking(NpcAI2 npcAI, float px, float py, float pz) {
		if (!GeoDataConfig.GEO_ENABLE || !GeoDataConfig.GEO_NPC_MOVE || !AIConfig.PATHFINDING_ENABLED) {
			return false;
		}

		if (startPathWalking(npcAI, npcAI.getOwner(), px, py, pz)) {
			return true;
		}
		return false;
	}

	/**
	 * @param owner
	 */
	protected static boolean startPathWalking(NpcAI2 npcAI, Npc owner, float px, float py, float pz) {
		if (owner.isInFlyingState())
			return false;

		//if (owner.getMoveController().hasCurrentRoute())
			//return false;


		pz = owner.getMoveController().getZ(owner, px, py, pz);
		final Cell cellOwner = new Cell(owner.getX(), owner.getY(), owner.getZ());

		final Cell cellDest = new Cell(px, py, pz);

		float dist = (float) MathUtil.getDistance(cellOwner.x, cellOwner.y, cellOwner.z,
			cellDest.x, cellDest.y, cellDest.z);
		if (dist > AIConfig.PATHFINDING_MAX_DISTANCE)
			return false;

		final Pathfinder pathfinder = new Pathfinder(cellOwner, cellDest, AIConfig.MAXIMUM_MOVE_SLANT);
		pathfinder.setOwner(owner);
		final ArrayList<Cell> path = pathfinder.findPath(AIConfig.PATHFINDING_ITERATIONS);
		List<RouteStep> route = new ArrayList<RouteStep>();
		if (path.size() == 0)
			return true;

		int routeStepIndex = 0;
		for(Cell cell : path) {
			RouteStep routeStep = new RouteStep(cell.x, cell.y, cell.z, 0);
			routeStep.setRouteStep(++routeStepIndex);
			route.add(routeStep);
			log.info("[WalkManager] cell.x: " + cell.x + " cell.y: " + cell.y + " cell.z " + cell.z);
		}
		RouteStep routeStep = new RouteStep(px, py, pz, 0);
		log.info("[WalkManager] end.x: " + px + " end.y: " + py + " end.z " + pz);
		routeStep.setRouteStep(++routeStepIndex);
		route.add(routeStep);
		//log.info("route_size:" + route.size());


		int currentPoint = owner.getMoveController().getCurrentPoint();
		RouteStep nextStep = findNextRoutStep(owner, route);
		owner.getMoveController().setCurrentRoute(route);
		owner.getMoveController().setRouteStep(nextStep, route.get(currentPoint));
		EmoteManager.emoteStartWalking(npcAI.getOwner());
		owner.getMoveController().moveToNextPoint();
		npcAI.isPathWalking = true;
		npcAI.prevSubState = npcAI.getSubState();
		npcAI.setStateIfNot(AIState.WALKING);
		npcAI.setSubStateIfNot(AISubState.WALK_PATH);
		return true;
	}

	/**
	 * @param npcAI
	 * @param owner
	 */
	private static boolean startRandomWalking(final NpcAI2 npcAI, final Npc owner) {
		if (!AIConfig.ACTIVE_NPC_MOVEMENT) {
			return false;
		}

		int randomWalkNr = owner.getSpawn().getRandomWalk();
		if (randomWalkNr == 0) {
			return false;
		}

		if (npcAI.setSubStateIfNot(AISubState.WALK_RANDOM)) {
			EmoteManager.emoteStartWalking(npcAI.getOwner());
			chooseNextRandomPoint(npcAI);
			return true;
		}
		return false;
	}

	/**
	 * @param npcAI
	 * @param owner
	 * @param template
	 */
	protected static void startRouteWalking(NpcAI2 npcAI, Npc owner, WalkerTemplate template) {
		if (!AIConfig.ACTIVE_NPC_MOVEMENT) {
			return;
		}
		List<RouteStep> route = template.getRouteSteps();
		int currentPoint = owner.getMoveController().getCurrentPoint();
		RouteStep nextStep = findNextRoutStep(owner, route);
		owner.getMoveController().setCurrentRoute(route);
		owner.getMoveController().setRouteStep(nextStep, route.get(currentPoint));
		EmoteManager.emoteStartWalking(npcAI.getOwner());
		npcAI.getOwner().getMoveController().moveToNextPoint();
	}

	/**
	 * @param owner
	 * @param route
	 * @return
	 */
	protected static RouteStep findNextRoutStep(Npc owner, List<RouteStep> route) {
		int currentPoint = owner.getMoveController().getCurrentPoint();
		RouteStep nextStep = null;
		if (currentPoint != 0) {
			nextStep = findNextRouteStepAfterPause(owner, route, currentPoint);
		}
		else {
			nextStep = findClosestRouteStep(owner, route, nextStep);
		}
		return nextStep;
	}

	/**
	 * @param owner
	 * @param route
	 * @param nextStep
	 * @return
	 */
	protected static RouteStep findClosestRouteStep(Npc owner, List<RouteStep> route, RouteStep nextStep) {
		double closestDist = 0;
		float x = owner.getX();
		float y = owner.getY();
		float z = owner.getZ();

		if (owner.getWalkerGroup() != null) {
			// always choose the 1st step, not the last which is close enough
			if (owner.getWalkerGroup().getGroupStep() < 2) {
				nextStep = route.get(0);
			}
			else {
				nextStep = route.get(owner.getWalkerGroup().getGroupStep() - 1);
			}
		}
		else {
			for (RouteStep step : route) {
				double stepDist = MathUtil.getDistance(x, y, z, step.getX(), step.getY(), step.getZ());
				if (closestDist == 0 || stepDist < closestDist) {
					closestDist = stepDist;
					nextStep = step;
				}
			}
		}
		return nextStep;
	}

	/**
	 * @param owner
	 * @param route
	 * @param currentPoint
	 * @return
	 */
	protected static RouteStep findNextRouteStepAfterPause(Npc owner, List<RouteStep> route, int currentPoint) {
		RouteStep nextStep = route.get(currentPoint);
		double stepDist = MathUtil.getDistance(owner.getX(), owner.getY(), owner.getZ(), nextStep.getX(), nextStep.getY(), nextStep.getZ());
		if (stepDist < 1) {
			nextStep = nextStep.getNextStep();
		}
		return nextStep;
	}

	/**
	 * Is this npc will walk. Currently all monsters will walk and those npc wich has walk routes
	 *
	 * @param npcAI
	 * @return
	 */
	public static boolean isWalking(NpcAI2 npcAI) {
		return npcAI.isMoveSupported() && (hasWalkRoutes(npcAI) || npcAI.getOwner().isAttackableNpc());
	}

	/**
	 * @param npcAI
	 * @return
	 */
	public static boolean hasWalkRoutes(NpcAI2 npcAI) {
		return npcAI.getOwner().hasWalkRoutes();
	}

	/**
	 * @param npcAI
	 */
	public static void targetReached(final NpcAI2 npcAI) {
		if (npcAI.isInState(AIState.WALKING)) {
			if (npcAI.isPathWalking)
				npcAI.setSubStateIfNot(npcAI.prevSubState);
			switch (npcAI.getSubState()) {
				case WALK_PATH:
					npcAI.getOwner().updateKnownlist();
					if (npcAI.getOwner().getWalkerGroup() != null) {
						npcAI.getOwner().getWalkerGroup().targetReached(npcAI);
					}
					else {
						chooseNextRouteStep(npcAI);
					}
					break;
				case WALK_WAIT_GROUP:
					npcAI.setSubStateIfNot(AISubState.WALK_PATH);
					chooseNextRouteStep(npcAI);
					break;
				case WALK_RANDOM:
					chooseNextRandomPoint(npcAI);
					break;
				case TALK:
					npcAI.getOwner().getMoveController().abortMove();
					break;
				default:
					break;
			}
			npcAI.isPathWalking = false;
		}
	}

	/**
	 * @param npcAI
	 */
	protected static void chooseNextRouteStep(final NpcAI2 npcAI) {
		int walkPause = npcAI.getOwner().getMoveController().getWalkPause();
		if (walkPause == 0) {
			npcAI.getOwner().getMoveController().resetMove();
			npcAI.getOwner().getMoveController().chooseNextStep();
			npcAI.getOwner().getMoveController().moveToNextPoint();
		}
		else {
			npcAI.getOwner().getMoveController().abortMove();
			npcAI.getOwner().getMoveController().chooseNextStep();
			ThreadPoolManager.getInstance().schedule(new Runnable() {

				@Override
				public void run() {
					if (npcAI.isInState(AIState.WALKING)) {
						npcAI.getOwner().getMoveController().moveToNextPoint();
					}
				}
			}, walkPause);
		}
	}

	/**
	 * @param npcAI
	 */
	private static void returnToSpawn(NpcAI2 npcAI) {
		//log.info("[WalkManager] returnToSpawn");
		final Npc owner = npcAI.getOwner();
		owner.getMoveController().moveToPoint(owner.getSpawn().getX(), owner.getSpawn().getY(), owner.getSpawn().getZ());
	}

	/**
	 * @param npcAI
	 */
	private static void chooseNextRandomPoint(final NpcAI2 npcAI) {
		final Npc owner = npcAI.getOwner();

		owner.getMoveController().setCurrentRoute(null);
		owner.getMoveController().abortMove();

		final int randomWalkNr = owner.getSpawn().getRandomWalk();
		final int walkRange = Math.max(randomWalkNr, WALK_RANDOM_RANGE);

		ThreadPoolManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				owner.getMoveController().setCurrentRoute(null);
				owner.getMoveController().abortMove();

				//log.info("[WalkManager] startRandomWalking.");
				// TODO - This code makes Creatures move too fast, when out of distance presumably.
				if (AIConfig.RANDOMWALK_THRESHOLD) {
					// This piece of code makes sure a player is in range.
					for (Player player : World.getInstance().getAllPlayers()) {
						if(!player.isOnline())
							continue;
						float dist = (float) MathUtil.getDistance(owner.getX(), owner.getY(), owner.getZ(),
							player.getX(), player.getY(), player.getZ());
						if (dist > AIConfig.RANDOMWALK_PLAYERMAXDIST) {
							chooseNextRandomPoint(npcAI);
							return;
						}
					}
				}

				if (npcAI.isInState(AIState.WALKING)) {
					float distToSpawn = (float) owner.getDistanceToSpawnLocation();
					if (distToSpawn > walkRange) {
						returnToSpawn(npcAI);
					}
					else {
						Vector3f loc = null;
						int i=0;
						while(i++ < AIConfig.RANDOM_MAX_TRIES)
						{
							int nextX = Rnd.nextInt(walkRange * 2) - walkRange;
							int nextY = Rnd.nextInt(walkRange * 2) - walkRange;

							if (GeoDataConfig.GEO_ENABLE && GeoDataConfig.GEO_NPC_MOVE) {
								byte flags = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId() | CollisionIntention.WALK.getId());
								loc = GeoService.getInstance().getClosestCollision(owner, owner.getX() + nextX, owner.getY() + nextY, owner.getZ(), true, flags);

								float dxy = (Math.abs(nextX) + Math.abs(nextY)) * AIConfig.MAXIMUM_MOVE_SLANT;
								float cxy = Math.abs(owner.getZ() - loc.z);
								if (cxy > dxy) {
									continue;
								}
								if (!GeoService.getInstance().canSee(owner, loc.x, loc.y, loc.z))
									continue;
								break;
							}
							else {
								loc = new Vector3f(owner.getX() + nextX, owner.getY() + nextY, owner.getZ());
								break;
							}
						}
						if (i == AIConfig.RANDOM_MAX_TRIES) {
							returnToSpawn(npcAI);
							return;
						}
						if (loc != null) {
							if (!startPathWalking(npcAI, loc.x, loc.y, loc.z))
								owner.getMoveController().moveToPoint(loc.x, loc.y, loc.z);
						}
					}
				}
			}
		}, Rnd.get(AIConfig.MINIMIMUM_DELAY, AIConfig.MAXIMUM_DELAY) * 1000);

	}

	/**
	 * @param npcAI
	 */
	public static void stopWalking(NpcAI2 npcAI) {
		npcAI.getOwner().getMoveController().abortMove();
		npcAI.setStateIfNot(AIState.IDLE);
		npcAI.setSubStateIfNot(AISubState.NONE);
		EmoteManager.emoteStopWalking(npcAI.getOwner());
		/*if (npcAI.isPathWalking) {
			//npcAI.getOwner().getMoveController().setCurrentRoute(null);
			npcAI.isPathWalking = false;
		}*/
	}

	/**
	 * @param owner
	 * @return
	 */
	public static boolean isArrivedAtPoint(NpcAI2 npcAI) {
		return npcAI.getOwner().getMoveController().isReachedPoint();
	}
}
