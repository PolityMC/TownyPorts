package com.earthpol.townyports.commands;

import com.earthpol.earthPolLib.cooldown.PlayerCooldownManager;
import com.earthpol.earthPolLib.entity.vehicle.VehicleUtil;
import com.earthpol.earthPolLib.teleport.TeleportOutcomeHandler;
import com.earthpol.earthPolLib.teleport.Teleporter;
import com.earthpol.earthPolLib.teleport.VehicleTeleporter;
import com.earthpol.townyports.PortsMain;
import com.earthpol.townyports.config.Config;
import com.earthpol.townyports.data.Port;
import com.earthpol.townyports.data.PortDAO;
import com.earthpol.townyports.registry.VehicleRegistry;
import com.earthpol.townyports.util.Msg;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;

public class PortCommand extends BaseCommand implements CommandExecutor {

	private static final PlayerCooldownManager portsCooldownManager = new PlayerCooldownManager();
	private static final String MUST_STAND_PORT_CHUNK_KEY = "port.travel.error.must-stand-port-chunk";

	public PortCommand() {}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
							 @NotNull String label, @NotNull String @NotNull [] args) {

		if(!(sender instanceof Player p)) {
			Msg.error(sender, "common.error.in-game-only");
			return true;
		}

		if (args.length == 0) {
			Msg.usage(p, "port.usage.base");
			return true;
		}

		// Destination town
		final Town destinationTown;
		try {
			destinationTown = getTownOrThrow(args[0]);
		} catch (TownyException e) {
			Msg.errorRaw(p, e.getMessage());
			return true;
		}

		final Port destinationPort = PortDAO.getPort(destinationTown);
		if (destinationPort == null) {
			Msg.error(p, "port.error.town-no-port");
			return true;
		}

		try {
			checkTeleportEligibility(p, destinationTown);
		} catch (TownyException e) {

			String messageKey = e.getMessage();
			if (messageKey != null) {
				Msg.error(p, messageKey);
			} else {
				Msg.error(p, "port.travel.error.cannot-travel-now");
			}

			// For the "must be standing in the same chunk as a port spawn" error,
			// show ONE compact follow-up line with coords + /t port here shortcut.
			if (MUST_STAND_PORT_CHUNK_KEY.equals(messageKey)) {
				sendOriginPortHintIfPossible(p);
			}

			return true;
		}

		// Vehicle restrictions
		if (VehicleUtil.isOnboardVehicle(p)) {
			if (!(p.getVehicle() instanceof Vehicle v) || !VehicleRegistry.isAllowedVehicle(v)) {
				Msg.error(p, "port.travel.error.vehicle-not-allowed");
				return true;
			}
		}

		// Teleporter configuration
		VehicleTeleporter portsVehicleTeleporter = new VehicleTeleporter(true, true);

		TeleportOutcomeHandler handler = new TeleportOutcomeHandler();
		handler.setOnFailedUnaffordable(context ->
				Msg.error(context.getPlayer(), "port.travel.error.cannot-afford")
		);

		long warmupTicks = Config.PORT_TRAVEL_WARMUP_IN_TICKS.getLong();
		long warmupSeconds = warmupTicks / 20;
		String sign = Config.CURRENCY_SIGN.getString();

		RoutePlan routePlan = buildRoutePlan(p, destinationPort);
		if (routePlan == null) {
			Msg.error(p, "port.travel.error.too-far");
			return true;
		}

		double travelCost = routePlan.totalCost();

		if (!hasSufficientBalance(p, travelCost)) {
			Msg.error(p, "port.travel.error.cannot-afford");
			return true;
		}

		Teleporter portsTeleporter = Teleporter.builder(PortsMain.instance)
				.setVehicleTeleporter(portsVehicleTeleporter)

				.preTeleportMessage(
						Component.text()
								.append(Msg.prefix(p))
								.append(Component.text(Msg.tr(p, "port.travel.pre.will-pay"), NamedTextColor.WHITE))
								.append(Component.text(travelCost + " " + sign, Msg.GOOD))
								.append(Component.text(Msg.tr(p, "port.travel.pre.to-travel"), NamedTextColor.WHITE))
								.append(Component.text(destinationPort.getName(), Msg.ACCENT))
								.append(Component.text(Msg.tr(p, "port.travel.pre.via-stops", routePlan.intermediateStopCount()), NamedTextColor.WHITE))
								.append(Component.text(Msg.tr(p, "port.travel.pre.in-seconds", warmupSeconds), NamedTextColor.WHITE))
								.append(Component.text(Msg.tr(p, "port.travel.pre.do-not-move"), Msg.MUTED))
								.build()
				)
				.postTeleportMessage(
						Component.text()
								.append(Msg.prefix(p))
								.append(Component.text(Msg.tr(p, "port.travel.post.arrived"), Msg.GOOD))
								.build()
				)

				.enableWarmup(warmupTicks)
				.enableDestinationSafety()
				.disablePreTeleportMovement()
				.setOutcomeHandler(handler)

				.build();

		// Cooldown
		if (portsCooldownManager.hasCooldown(p)) {
			Msg.warn(p, "port.travel.cooldown", portsCooldownManager.getCooldown(p));
			return true;
		}

		portsTeleporter.teleport(
				p,
				destinationPort.location(),
				travelCost,
				destinationTown.getAccount(),
				Msg.tr("port.travel.reason.teleport")
		);

		sendRouteSummary(p, routePlan, sign);
		startTeleportCountdown(p, warmupTicks);
		scheduleCooldownOnSuccessfulTeleport(p, destinationPort.location(), warmupTicks, destinationTown, routePlan);
		return true;
	}

	private static void startTeleportCountdown(@NotNull Player player, long warmupTicks) {
		for (int second = 3; second >= 1; second--) {
			int countdownSecond = second;
			long delay = warmupTicks - (second * 20L);
			if (delay < 0) continue;

			Bukkit.getScheduler().runTaskLater(PortsMain.instance, () -> {
				if (!player.isOnline()) return;
				player.sendActionBar(Component.text(Msg.tr(player, "port.travel.actionbar.countdown", countdownSecond), Msg.ACCENT));
			}, delay);
		}
	}

	private static void scheduleCooldownOnSuccessfulTeleport(@NotNull Player player, @NotNull Location destination, long warmupTicks,
														 @NotNull Town destinationTown,
														 @NotNull RoutePlan routePlan) {
		Bukkit.getScheduler().runTaskLater(PortsMain.instance, () -> {
			if (!player.isOnline()) return;

			Location current = player.getLocation();
			if (!current.getWorld().equals(destination.getWorld())) return;

			if (current.distanceSquared(destination) <= 4.0) {
				distributePortTrafficPayouts(destinationTown, routePlan);
				portsCooldownManager.setCooldown(player, Config.PORT_TRAVEL_COOLDOWN_IN_SECONDS.getLong());
			}
		}, warmupTicks + 5L);
	}

	private static void distributePortTrafficPayouts(@NotNull Town destinationTown, @NotNull RoutePlan routePlan) {
		if (routePlan.stops().size() <= 1) return;

		Object destinationAccount = destinationTown.getAccount();
		for (RouteStop stop : routePlan.stops()) {
			if (stop.town().equals(destinationTown)) continue;
			try {
				Object stopAccount = stop.town().getAccount();
				payBetweenAccounts(destinationAccount, stopAccount, stop.cost(), Msg.tr("port.travel.reason.routed-payout"));
			} catch (Exception ignored) {
				// Keep travel successful even if account redistribution fails for a specific stop.
			}
		}
	}

	private static void payBetweenAccounts(Object fromAccount, Object toAccount, double amount, String reason) throws Exception {
		for (Method method : fromAccount.getClass().getMethods()) {
			if (!method.getName().equals("payTo")) continue;
			Class<?>[] params = method.getParameterTypes();
			if (params.length != 3) continue;
			if (params[0] != double.class) continue;
			if (!params[1].isAssignableFrom(toAccount.getClass())) continue;
			if (params[2] != String.class) continue;
			method.invoke(fromAccount, amount, toAccount, reason);
			return;
		}
	}

	private static boolean hasSufficientBalance(@NotNull Player player, double amount) {
		var registration = Bukkit.getServicesManager().getRegistration(Economy.class);
		if (registration == null || registration.getProvider() == null) {
			return true;
		}

		return registration.getProvider().has(player, amount);
	}

	/**
	 * One compact follow-up message (no world name, styled X/Y/Z) with click-to-copy coords + quick /t port here.
	 */
	private static void sendOriginPortHintIfPossible(Player p) {
		Resident res = TownyAPI.getInstance().getResident(p);
		Town town = (res != null) ? res.getTownOrNull() : null;
		if (town == null) return;

		Port originPort = PortDAO.getPort(town);
		if (originPort == null) return;

		var l = originPort.location();
		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();

		// This is what we "copy" into chat: "x y z"
		Component coords = Msg.styledCoords(p, x, y, z);

		Msg.send(p, Component.text()
				.append(Msg.prefix(p))
				.append(Component.text(Msg.tr(p, "port.hint.label.port-spawn"), Msg.MUTED))
				.append(coords)
				.append(Component.text(Msg.tr(p, "general.separator.dot"), Msg.MUTED))
				.append(Msg.runCmd(Msg.tr(p, "port.hint.check-here.label"), "/t port here",
						Msg.tr(p, "port.hint.check-here.hover")))
				.build()
		);
	}

	/**
	 * Checks all prerequisite conditions required before a player may port-travel.
	 * Throws TownyException with a player-facing message if any condition is not met.
	 */
	private static void checkTeleportEligibility(Player player, @NotNull Town destinationTown) throws TownyException {

		TownyAPI townyAPI = TownyAPI.getInstance();

		Resident playerResident = townyAPI.getResident(player);
		if (playerResident == null) {
			throw new TownyException("port.travel.error.failed-resident-contact-staff");
		}

		Town playerTown = playerResident.getTownOrNull();
		if (playerTown == null) {
			throw new TownyException("common.error.no-town");
		}

		if (!playerTown.hasNation()) {
			throw new TownyException("port.travel.error.no-nation");
		}

		Nation playerNation = playerTown.getNationOrNull();

		// Wilderness restriction
		if (!player.hasPermission("townyports.bypass.wilderness") && townyAPI.isWilderness(player.getLocation())) {
			throw new TownyException("port.travel.error.wilderness-denied");
		}

		// Enemy restriction
		Nation destinationNation = destinationTown.getNationOrNull();
		if (destinationNation != null &&
				playerNation != null &&
				destinationNation.hasEnemy(playerNation) &&
				Config.PORT_TRAVEL_DENIES_FOR_ENEMIES.getBool()) {
			throw new TownyException("port.travel.error.enemy-denied");
		}

		// Outlaw restriction
		if (destinationTown.hasOutlaw(playerResident)) {
			throw new TownyException("port.travel.error.outlaw-denied");
		}

		// Must start in a port chunk
		if (!isPlayerStandingInPortChunk(player)) {
			throw new TownyException(MUST_STAND_PORT_CHUNK_KEY);
		}
	}

	private static RoutePlan buildRoutePlan(@NotNull Player player, @NotNull Port destinationPort) {
		TownyAPI townyAPI = TownyAPI.getInstance();
		Town originTown = townyAPI.getTownOrNull(townyAPI.getTownBlock(WorldCoord.parseWorldCoord(player.getLocation())));
		if (originTown == null) return null;

		Port originPort = PortDAO.getPort(originTown);
		if (originPort == null) return null;

		if (originTown.equals(destinationPort.town())) {
			return new RoutePlan(List.of(new RouteStop(destinationPort.town(), destinationPort.portPrice())), destinationPort.portPrice());
		}

		List<Port> allPorts = PortDAO.getAllPorts();
		Map<Town, Port> byTown = new HashMap<>();
		for (Port port : allPorts) {
			byTown.put(port.town(), port);
		}

		Port dest = byTown.get(destinationPort.town());
		Port start = byTown.get(originTown);
		if (start == null || dest == null) return null;

		int maxDistance = Config.MAXIMUM_PORT_DISTANCE_IN_CHUNKS.getInt();
		Nation playerNation = getPlayerNation(player);
		Map<Town, Double> bestCost = new HashMap<>();
		Map<Town, Town> previousTown = new HashMap<>();
		PriorityQueue<RouteNode> queue = new PriorityQueue<>(Comparator.comparingDouble(RouteNode::cost));

		bestCost.put(originTown, 0.0);
		queue.add(new RouteNode(start, 0.0));

		while (!queue.isEmpty()) {
			RouteNode current = queue.poll();
			if (current.port().town().equals(destinationPort.town())) break;

			double knownBest = bestCost.getOrDefault(current.port().town(), Double.MAX_VALUE);
			if (current.cost() > knownBest) continue;

			for (Port next : allPorts) {
				if (next.town().equals(current.port().town())) continue;
				if (!canUsePort(playerNation, next.town())) continue;

				double hopDistanceInChunks = getChunkDistanceBetweenPorts(current.port(), next);
				if (hopDistanceInChunks > maxDistance) {
					continue;
				}

				double nextCost = current.cost() + next.portPrice();
				double existing = bestCost.getOrDefault(next.town(), Double.MAX_VALUE);
				if (nextCost < existing) {
					bestCost.put(next.town(), nextCost);
					previousTown.put(next.town(), current.port().town());
					queue.add(new RouteNode(next, nextCost));
				}
			}
		}

		if (!bestCost.containsKey(destinationPort.town())) {
			return null;
		}

		List<RouteStop> reversedStops = new ArrayList<>();
		Town stepTown = destinationPort.town();
		while (!stepTown.equals(originTown)) {
			Port stepPort = byTown.get(stepTown);
			if (stepPort == null) return null;
			reversedStops.add(new RouteStop(stepTown, stepPort.portPrice()));
			stepTown = previousTown.get(stepTown);
			if (stepTown == null) return null;
		}

		Collections.reverse(reversedStops);

		if (reversedStops.size() == 1) {
			double directHopDistance = getChunkDistanceBetweenPorts(start, dest);
			if (directHopDistance > maxDistance) {
				return null;
			}
		}

		int minimumIntermediateStops = getMinimumIntermediateStopsRequired(start, dest, maxDistance);
		RoutePlan routePlan = new RoutePlan(reversedStops, bestCost.get(destinationPort.town()));
		if (routePlan.intermediateStopCount() < minimumIntermediateStops) {
			return null;
		}

		return routePlan;
	}

	private static void sendRouteSummary(@NotNull Player player, @NotNull RoutePlan routePlan, @NotNull String sign) {
		if (routePlan.stops().isEmpty()) return;

		StringBuilder stopList = new StringBuilder();
		for (int i = 0; i < routePlan.stops().size() - 1; i++) {
			if (i > 0) stopList.append(", ");
			stopList.append(routePlan.stops().get(i).town().getName());
		}

		RouteStop finalStop = routePlan.stops().get(routePlan.stops().size() - 1);
		Component message = Component.text()
				.append(Msg.prefix(player))
				.append(Component.text(Msg.tr(player, "port.route.label.route"), Msg.MUTED))
				.append(Component.text(finalStop.town().getName(), Msg.ACCENT))
				.append(Component.text(Msg.tr(player, "port.route.part.cost", routePlan.totalCost(), sign), Msg.GOOD))
				.append(Component.text(Msg.tr(player, "port.route.part.via-stops", routePlan.intermediateStopCount()), NamedTextColor.WHITE))
				.build();
		Msg.send(player, message);

		if (stopList.length() > 0) {
			Msg.send(player, Component.text()
					.append(Msg.prefix(player))
					.append(Component.text(Msg.tr(player, "port.route.label.stops"), Msg.MUTED))
					.append(Component.text(stopList.toString(), Msg.ACCENT))
					.build());
		}

		StringBuilder feeList = new StringBuilder();
		for (RouteStop stop : routePlan.stops()) {
			if (!feeList.isEmpty()) feeList.append(", ");
			feeList.append(stop.town().getName()).append(" ").append(stop.cost()).append(" ").append(sign);
		}

		Msg.send(player, Component.text()
				.append(Msg.prefix(player))
				.append(Component.text(Msg.tr(player, "port.route.label.fees"), Msg.MUTED))
				.append(Component.text(feeList.toString(), Msg.GOOD))
				.build());
	}


	private static int getMinimumIntermediateStopsRequired(@NotNull Port start, @NotNull Port destination, int maxDistancePerHopInChunks) {
		if (maxDistancePerHopInChunks <= 0) {
			return Integer.MAX_VALUE;
		}

		double directDistance = getChunkDistanceBetweenPorts(start, destination);
		if (directDistance == Double.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}

		int requiredHopCount = (int) Math.ceil(directDistance / maxDistancePerHopInChunks);
		return Math.max(0, requiredHopCount - 1);
	}

	private static double getChunkDistanceBetweenPorts(@NotNull Port a, @NotNull Port b) {
		Location aLocation = a.location();
		Location bLocation = b.location();

		if (aLocation.getWorld() == null || bLocation.getWorld() == null) {
			return Double.MAX_VALUE;
		}

		if (!aLocation.getWorld().equals(bLocation.getWorld())) {
			return Double.MAX_VALUE;
		}

		int aChunkX = aLocation.getBlockX() >> 4;
		int aChunkZ = aLocation.getBlockZ() >> 4;
		int bChunkX = bLocation.getBlockX() >> 4;
		int bChunkZ = bLocation.getBlockZ() >> 4;

		double deltaChunkX = aChunkX - bChunkX;
		double deltaChunkZ = aChunkZ - bChunkZ;
		return Math.sqrt((deltaChunkX * deltaChunkX) + (deltaChunkZ * deltaChunkZ));
	}

	private static Nation getPlayerNation(@NotNull Player player) {
		Resident resident = TownyAPI.getInstance().getResident(player);
		if (resident == null) return null;
		Town town = resident.getTownOrNull();
		if (town == null) return null;
		return town.getNationOrNull();
	}

	private static boolean canUsePort(Nation playerNation, @NotNull Town destinationTown) {
		Nation destinationNation = destinationTown.getNationOrNull();
		if (destinationNation == null || playerNation == null) return true;
		if (!Config.PORT_TRAVEL_DENIES_FOR_ENEMIES.getBool()) return true;
		return !destinationNation.hasEnemy(playerNation);
	}

	private record RouteNode(Port port, double cost) {}

	private record RouteStop(Town town, double cost) {}

	private record RoutePlan(List<RouteStop> stops, double totalCost) {
		int intermediateStopCount() {
			return Math.max(0, stops.size() - 1);
		}
	}

	public static boolean isPlayerStandingInPortChunk(Player p) {
		TownyAPI townyAPI = TownyAPI.getInstance();
		WorldCoord playerWorldCoord = WorldCoord.parseWorldCoord(p.getLocation());

		Town townPlayerIsStandingIn = townyAPI.getTownOrNull(townyAPI.getTownBlock(playerWorldCoord));
		if (townPlayerIsStandingIn == null) return false;

		Port townPort = PortDAO.getPort(townPlayerIsStandingIn);
		if (townPort == null) return false;

		WorldCoord portWorldCoord = WorldCoord.parseWorldCoord(townPort.location());
		return playerWorldCoord.equals(portWorldCoord);
	}
}
