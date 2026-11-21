# Mango Goat Napper

Mango Goat Napper is a separate Minecraft Java 26.3 single-player mod and Windows companion. A completed TikTok LIVE Goat gift worth 200 coins triggers a flaming, screaming goat to appear roughly 30–50 blocks away and chase the player. It screams once, grabs the player, and carries them while the screen is black. The player wakes up lying down approximately 100–150 blocks from the starting point; press sneak to get up. A configurable cooldown starts when the goat spawns; the default is five minutes.

If the player remains within about three blocks for eight seconds and the goat cannot close the gap, the goat teleports to a safe nearby spot. If that is impossible, the player is moved to a safe spot near the goat. The goat screams, waits two seconds, and grabs the player. If neither spot is safe, the chase continues. Players who keep moving can escape; the chase ends after 60 seconds. The goat's flames are visual only and its attack damage is zero. After pickup and blackout, the mod moves the player directly to a checked landing spot, avoiding lava in and around it. It checks again immediately before moving the player. Unusual terrain and other mods can still affect safety. If no safe spot is found it ends the event. Gifts during an event or cooldown are dropped, not queued.

## Requirements

- Minecraft Java 26.3, single-player, Windows 11 x64.
- Fabric Loader 0.20.1 with Fabric API 0.166.2+26.3, or NeoForge 26.3.0.102. Install only the matching mod JAR.
- Mango Goat Napper Companion, kept open on the same computer. Gifted Pets is not required.

## Play

1. Install the appropriate JAR into the instance's `mods` folder. Fabric also requires Fabric API.
2. Launch the companion, enter the creator's TikTok username or LIVE URL, and click Connect. Set the cooldown in minutes and seconds and click Save; changes apply to the next goat.
3. Open a single-player world and wait for the companion to show Minecraft connected.
4. Click **Test goat (no gift needed)** to test without a TikTok gift. A real gift requires a LIVE connection and a completed Goat gift.
5. After relocation, release sneak if held, then press it to wake up. The mod also releases the pose after one minute as a fallback.

## Status and privacy

A flat-world Fabric test verified the unassisted chase. Synthetic tests exercise the four-block ledge, grabbing, relocation, lying pose, simulated sneak, and cooldown. Actual gift delivery on TikTok LIVE and full terrain coverage still require real-world verification. The companion uses the unofficial TikTokLive library and its external signing service; changes outside this project can interrupt connections.

The companion uses `127.0.0.1:18765` and a per-launch token in `%LOCALAPPDATA%\MangoGoatNapper\connection.json`. Do not share that file while the companion runs. It is separate from Gifted Pets and SteveCount.

## Build and test

Use Java 25 and run `gradlew.bat build` inside each of the `fabric` and `neoforge` directories. The companion's pinned Python dependencies are in `companion/requirements-lock.txt`. Run `python -m pytest tests` from the project root. `runClient -PmangogoatnapperSmoke` creates a separate synthetic test world; add `-PmangogoatnapperElevated` to run the four-block ledge case. These are development tests and do not contact TikTok.

The scream audio was provided by the user. Do not upload a substitute recording without the rights to use it.
