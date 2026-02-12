# TownyPorts

TownyPorts is a Towny addon that lets players travel between town-owned ports using `/t port` commands.

It is designed for long-distance worlds where overland travel can be slow, while still keeping travel balanced through:
- economy cost,
- warmup + cooldown,
- nation/enemy restrictions,
- and per-hop distance limits.

---

## 1) What TownyPorts does (high-level)

TownyPorts adds two Towny command branches:
- `/t port ...` for travel and information,
- `/t set port ...` for configuring your town’s port.

A **port** is data attached to a Town (not a separate block/entity):
- port spawn location,
- port fee.

When a player travels, TownyPorts validates eligibility (town/nation/restrictions), calculates a valid route, charges the player, warms up the teleport, and applies cooldown after successful arrival.

---

## 2) Runtime requirements

### Required
- **Paper/Spigot-compatible server** (plugin targets Paper API 1.21.x).
- **Towny** (hard dependency).

### Optional
- **Vault + economy plugin** (soft dependency). If Vault economy is present, TownyPorts checks player affordability using Vault before teleport.

### Java
- Java **17** build/runtime target.

---

## 3) Installation

1. Build the plugin JAR:
   ```bash
   mvn clean package
   ```
2. Put the built JAR in your server `plugins/` folder.
3. Start/restart server.
4. Ensure Towny is loaded first.

---

## 4) Command system and ownership model

TownyPorts registers subcommands under Towny’s addon API:
- `/t port`
- `/t set port`

### Port ownership
A port belongs to a **Town**. The plugin stores metadata directly on the Town:
- `townyPorts_portLocation`
- `townyPorts_portPrice`

If a town has location metadata but no price metadata, the port uses `default-port-fee` from config.

This means data persists with Towny objects and can be queried globally from all towns.

---

## 5) Travel flow (technical deep dive)

When a player runs `/t port <destinationTown>`:

1. **Destination validation**
   - Destination town must exist.
   - Destination town must have a configured port.

2. **Eligibility checks**
   - Sender must be a player.
   - Player must belong to a town.
   - Player’s town must belong to a nation.
   - Player cannot start in wilderness unless they have `townyports.bypass.wilderness`.
   - If enemy denial is enabled, player cannot travel to enemy nation ports.
   - Player must stand in a valid port chunk (same chunk as that town’s configured port).

3. **Vehicle checks**
   - If mounted, only certain vehicle classes are allowed (horse-like, boats, pigs, striders, etc.).

4. **Route planning**
   - TownyPorts builds a route from origin town port to destination using all known ports.
   - Hops are only allowed if the chunk-distance between two port chunks is within `maximum-port-distance-in-chunks`.
   - Route cost is cumulative sum of stop fees.
   - If no legal route exists, travel is denied.

5. **Cost + affordability**
   - Total route cost is computed.
   - If Vault economy is available, balance check is performed before teleport.

6. **Warmup teleport**
   - Teleport starts with configurable warmup (`port-travel-warmup-in-ticks`).
   - Movement during warmup cancels teleport (configured in teleporter behavior).
   - Destination safety checks are enabled.

7. **Arrival and cooldown**
   - On successful arrival, cooldown is applied (`port-travel-cooldown-in-seconds`).
   - If route had intermediary stops, destination town account redistributes per-stop payouts to intermediary towns.

### Notes on route economics
- The pre-teleport transaction charges total route cost.
- Intermediary payouts are attempted after successful arrival.
- Redistribution failures are intentionally non-fatal to avoid cancelling successful player travel.

---

## 6) All commands

## Player/General commands (`/t port`)
- `/t port <town>` — travel to a town’s port.
- `/t port list [page]` — list active ports.
- `/t port price <town>` — show fee for a town port.
- `/t port info [town]` — show your town port info or another town.
- `/t port here` — verify whether your current chunk is a town’s port chunk.
- `/t port reload` — reload TownyPorts config (permission/op required).
- `/t port help` — show clickable help menu.

### Town-management commands (`/t set port`)
- `/t set port spawn` — set your town’s port spawn to current location.
- `/t set port price <amount>` — set your town’s fee.
- `/t set port remove` — remove your town’s port metadata.

---

## 7) Permissions

Defined in `plugin.yml`:
- `townyports.bypass.wilderness`
- `townyports.set.spawn`
- `townyports.reload`

Also used in command checks:
- `townyports.port.list`
- `townyports.port.price`
- `townyports.set.price`

If your permission plugin setup is strict, explicitly grant the command permissions above to your intended staff/player groups.

---

## 8) Configuration reference

`config.yml` keys (reloadable):

- `uses-economy` (bool, default `true`)
  - Enables economy-dependent price setting behavior.

- `currency-sign` (string, default `Gold`)
  - Display text used in messages.

- `minimum-port-fee` (int, default `1`)
- `maximum-port-fee` (int, default `100`)
  - Bounds for `/t set port price`.

- `default-port-fee` (int, default `10`)
  - Used when a town has port location but no explicit stored fee.

- `maximum-port-distance-in-chunks` (int, default `2750`)
  - Maximum allowed distance per hop between ports in routing.

- `enable-port-arrival-alert` (bool, default `false`)
  - Config node exists; listener hook is currently a placeholder in main class.

- `port-travel-warmup-in-ticks` (long, default `200`)
  - Warmup before teleport (200 ticks = 10 seconds).

- `port-travel-cooldown-in-seconds` (long, default `60`)
  - Cooldown after successful travel.

- `port-travel-denies-for-enemies` (bool, default `true`)
  - Blocks travel to enemy nation ports.

After edits, reload with:
```bash
/t port reload
```

---

## 9) Operational tips for server admins

- **Set clear fee policy:** Keep min/max realistic for your economy.
- **Tune distance cap:** Larger map = larger `maximum-port-distance-in-chunks`.
- **Use cooldown to prevent spam:** Keep travel meaningful.
- **Train staff on `/t port here`:** Great for diagnosing “why can’t I travel?” cases.
- **Remember chunk rule:** Players must stand in the same chunk as a configured port spawn (unless your policy grants bypass behavior via permissions).

---

## 10) Troubleshooting

### “You must be standing in the same chunk as a port spawn to travel.”
- Player is not in a valid origin port chunk.
- Use `/t port here`.
- Confirm town has a port: `/t port info`.

### “That town does not have a port.”
- Destination town exists but no port metadata is set.
- Town staff should run `/t set port spawn`.

### “You cannot afford to travel to this port.”
- Vault economy is active and player balance is insufficient.

### “Economy is disabled.” when setting price
- `uses-economy` is false; price setting is blocked by validation logic.

### Reload failed
- Check console for config parsing errors.

---

## 11) Developer notes

### Project layout
- Main plugin bootstrap: `PortsMain`
- Command handlers:
  - `PortBaseCommand` (`/t port` subcommands)
  - `PortCommand` (actual travel)
  - `PortSetCommand` (`/t set port ...`)
- Data:
  - `Port` record
  - `PortDAO` metadata read/write
- Utility:
  - `Msg` component messaging helpers
  - `HelpMenu` clickable command help
  - `VehicleRegistry` mounted vehicle allowlist

### Build
```bash
mvn clean package
```

### Dependency context
- Towny API for towns/residents/nations + command addon registration.
- earthPolLib for config handler, cooldown manager, teleporter, and helpers.
- Vault API used for affordability checks when provider is present.

---

# Player Guide (Non-Developer)

This section is for regular players.

If you only care about using ports, read this part.

## What is a port?
A port is a town travel point. If your server has TownyPorts, you can fast-travel between towns with ports.

## How to travel
1. Stand in a valid port chunk (usually your town’s port area).
2. Run:
   ```
   /t port <townName>
   ```
3. Don’t move during warmup.
4. You arrive at that town’s port.

## Commands you will actually use
- `/t port list` → shows towns with ports.
- `/t port <town>` → travel there.
- `/t port price <town>` → see travel fee.
- `/t port info` → see your town port details.
- `/t port here` → checks if you are standing in a valid port chunk.

## Why travel sometimes fails
- You are not in a port chunk.
- Your town (or you) does not meet nation/travel rules.
- Destination has no port.
- You don’t have enough money.
- You are on cooldown.

## Fast fix checklist
- Run `/t port here`.
- Run `/t port list`.
- Check fee with `/t port price <town>`.
- Make sure you can pay.
- Try again after cooldown.

## If you manage a town
- Set port location where you stand:
  ```
  /t set port spawn
  ```
- Set fee:
  ```
  /t set port price <amount>
  ```

That’s it—you don’t need to know the backend details to use it effectively.
