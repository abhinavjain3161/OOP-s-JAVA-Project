package com.smartcity.traffic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * TrafficController – Manages the circular smart traffic light system.
 * Implements the core logic for:
 * 1. Fixed 80-second cycle
 * 2. Circular rotation (N → E → S → W → N)
 * 3. Yellow phase between green and red
 * 4. Smart redistribution when lanes are empty
 * 5. Emergency override for ambulances (gives ambulance lane green ASAP)
 *
 * @author Abhinav
 */
public class TrafficController {
    private static final int TOTAL_CYCLE_TIME = 80; // Fixed cycle time in seconds
    private static final int DEFAULT_GREEN_TIME = 20; // Default 20 seconds per lane (80/4)
    private static final int TIME_BANK_RESET_SECONDS = 1000; // Expire bank after 1000s
    private static final String[] LANE_SEQUENCE = { "NORTH", "EAST", "SOUTH", "WEST" };

    private Map<String, TrafficLight> trafficLights;
    private int currentLaneIndex;
    private boolean smartModeEnabled;
    private int cycleTimeElapsed;
    private int savedTimeBank; // Accumulated time saved from empty lanes
    private int timeBankAgeSeconds; // How long current bank has existed

    private ArrayList<Vehicle> vehicles;

    // Pause support
    private boolean paused = false;

    // Emergency override tracking (for GUI counter)
    private int emergencyOverrideCount = 0;

    /**
     * Constructor for TrafficController
     */
    public TrafficController() {
        this.trafficLights = new HashMap<>();
        this.currentLaneIndex = 0;
        this.smartModeEnabled = true;
        this.cycleTimeElapsed = 0;
        this.savedTimeBank = 0;
        this.timeBankAgeSeconds = 0;
        this.vehicles = new ArrayList<>();

        // Initialise traffic lights for each lane
        for (String lane : LANE_SEQUENCE) {
            trafficLights.put(lane, new TrafficLight(lane, DEFAULT_GREEN_TIME));
        }

        // Start with first lane green
        activateGreenLight(getCurrentLane(), DEFAULT_GREEN_TIME);
    }

    // ── Lane helpers ─────────────────────────────────────────────────────────

    /** Get the current active lane. */
    public String getCurrentLane() {
        return LANE_SEQUENCE[currentLaneIndex];
    }

    private String getNextLane() {
        return LANE_SEQUENCE[(currentLaneIndex + 1) % LANE_SEQUENCE.length];
    }

    private void moveToNextLane() {
        currentLaneIndex = (currentLaneIndex + 1) % LANE_SEQUENCE.length;
    }

    // ── Signal activation ────────────────────────────────────────────────────

    /**
     * Set all lights to red, then activate green for the specified lane.
     */
    private void activateGreenLight(String laneID, int duration) {
        for (TrafficLight light : trafficLights.values()) {
            light.activateRed();
        }
        trafficLights.get(laneID).activateGreen(duration);
    }

    // ── Main update loop ─────────────────────────────────────────────────────

    /**
     * Update traffic signals – called every second.
     * Order: tick → emergency check → transition check → smart check.
     */
    public void update() {
        if (paused)
            return;

        cycleTimeElapsed++;
        if (cycleTimeElapsed >= TOTAL_CYCLE_TIME) {
            cycleTimeElapsed = 0;
        }

        updateTimeBankExpiry();

        TrafficLight currentLight = trafficLights.get(getCurrentLane());

        // 1. Tick the current light first
        currentLight.tick();

        // 2. Emergency override (highest priority)
        checkEmergencyOverride();

        // 3. Handle yellow → red transition
        if (currentLight.isYellow() && currentLight.getTimeRemaining() <= 0) {
            // Yellow phase finished – move to next lane
            advanceToNextLane();
            return;
        }

        // 4. Handle green → yellow transition
        if (currentLight.isGreen() && currentLight.getTimeRemaining() <= 0) {
            currentLight.activateYellow();
            return;
        }

        // 5. Smart redistribution (only during green phase)
        if (currentLight.isGreen() && smartModeEnabled) {
            checkSmartRedistribution();
        }
    }

    /** Reset the time bank if it has existed for too long. */
    private void updateTimeBankExpiry() {
        if (savedTimeBank <= 0) {
            timeBankAgeSeconds = 0;
            return;
        }

        timeBankAgeSeconds++;
        if (timeBankAgeSeconds >= TIME_BANK_RESET_SECONDS) {
            System.out.println("⌛ Time Bank Expired: Resetting after " + TIME_BANK_RESET_SECONDS + "s");
            savedTimeBank = 0;
            timeBankAgeSeconds = 0;
        }
    }

    // ── Lane transition ──────────────────────────────────────────────────────

    /**
     * Advance to the next lane in the circular sequence.
     * Uses the time bank to extend green for busy lanes.
     */
    private void advanceToNextLane() {
        moveToNextLane();
        String nextLane = getCurrentLane();

        int baseGreenTime = DEFAULT_GREEN_TIME;
        boolean nextLaneHasVehicles = hasVehiclesInLane(nextLane);

        int greenTime;
        if (nextLaneHasVehicles && savedTimeBank > 0) {
            int vehicleCount = countVehiclesInLane(nextLane);
            int bonusTime = Math.min(savedTimeBank, vehicleCount * 3); // max 3s per vehicle
            int maxGreen = 40; // cap at 40s to maintain cycle balance
            greenTime = Math.min(baseGreenTime + bonusTime, maxGreen);
            int usedTime = greenTime - baseGreenTime;
            savedTimeBank -= usedTime;

            System.out.println("💰 Time Bank Used: " + nextLane + " is busy (" + vehicleCount + " vehicles)");
            System.out.println("   Extended green: " + baseGreenTime + "s + " + usedTime + "s = " + greenTime + "s");
            System.out.println("   Remaining Bank: " + savedTimeBank + "s");
        } else {
            greenTime = baseGreenTime;
            if (!nextLaneHasVehicles && savedTimeBank > 0) {
                System.out.println("💤 " + nextLane + " empty, saving time bank for next busy lane");
            }
        }

        activateGreenLight(nextLane, greenTime);
    }

    // ── Smart redistribution ─────────────────────────────────────────────────

    /**
     * If the current green lane is empty, switch immediately and bank the saved
     * time.
     */
    private void checkSmartRedistribution() {
        String currentLane = getCurrentLane();
        TrafficLight currentLight = trafficLights.get(currentLane);

        boolean hasVehicles = hasVehiclesInLane(currentLane);
        int minimumGreenTime = 2; // avoid rapid switching
        int elapsedGreenTime = currentLight.getGreenTime() - currentLight.getTimeRemaining();

        if (!hasVehicles && elapsedGreenTime >= minimumGreenTime && currentLight.getTimeRemaining() > 1) {
            int remainingTime = currentLight.getTimeRemaining();
            savedTimeBank += remainingTime;

            System.out.println("⚡ Real-time Switch: " + currentLane + " empty, switching immediately!");
            System.out.println("   Saved " + remainingTime + "s | Time Bank: " + savedTimeBank + "s");

            // Force immediate yellow → then next lane
            currentLight.activateYellow();
        }
    }

    // ── Emergency override ───────────────────────────────────────────────────

    /**
     * If an ambulance is detected in a non-green lane, shorten the current green
     * to 3 seconds maximum so the cycle reaches the ambulance lane quickly.
     * If the ambulance lane is the very next lane, skip straight to yellow now.
     */
    private void checkEmergencyOverride() {
        for (Vehicle vehicle : vehicles) {
            if (vehicle instanceof Ambulance && !vehicle.hasPassedIntersection()) {
                String ambulanceLane = vehicle.getLaneID();

                if (!ambulanceLane.equals(getCurrentLane())) {
                    TrafficLight currentLight = trafficLights.get(getCurrentLane());

                    if (currentLight.isGreen() && currentLight.getTimeRemaining() > 3) {
                        System.out.println("🚨 Emergency Override: Ambulance in " +
                                ambulanceLane + " – shortening current green");
                        emergencyOverrideCount++;

                        // If ambulance lane is next, go to yellow immediately
                        if (ambulanceLane.equals(getNextLane())) {
                            currentLight.activateYellow();
                        } else {
                            currentLight.setTimeRemaining(3);
                        }
                    }
                }
                break; // Handle only the first ambulance found
            }
        }
    }

    // ── Vehicle helpers ──────────────────────────────────────────────────────

    private boolean hasVehiclesInLane(String laneID) {
        for (Vehicle v : vehicles) {
            if (v.getLaneID().equals(laneID) && !v.hasPassedIntersection())
                return true;
        }
        return false;
    }

    private int countVehiclesInLane(String laneID) {
        int count = 0;
        for (Vehicle v : vehicles) {
            if (v.getLaneID().equals(laneID) && !v.hasPassedIntersection())
                count++;
        }
        return count;
    }

    /** Public accessor so the GUI can display per-lane vehicle counts. */
    public int getVehicleCount(String laneID) {
        return countVehiclesInLane(laneID);
    }

    // ── Pause / Resume ───────────────────────────────────────────────────────

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }

    // ── Setters / Getters ────────────────────────────────────────────────────

    public void setVehicles(ArrayList<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public TrafficLight getTrafficLight(String laneID) {
        return trafficLights.get(laneID);
    }

    public static String[] getLaneSequence() {
        return LANE_SEQUENCE;
    }

    public void toggleSmartMode() {
        smartModeEnabled = !smartModeEnabled;
        System.out.println("Smart Mode: " + (smartModeEnabled ? "ENABLED" : "DISABLED"));
    }

    public boolean isSmartModeEnabled() {
        return smartModeEnabled;
    }

    public int getCycleTimeElapsed() {
        return cycleTimeElapsed;
    }

    public int getTotalCycleTime() {
        return TOTAL_CYCLE_TIME;
    }

    public int getRemainingCycleTime() {
        return TOTAL_CYCLE_TIME - cycleTimeElapsed;
    }

    public int getSavedTimeBank() {
        return savedTimeBank;
    }

    public int getEmergencyOverrideCount() {
        return emergencyOverrideCount;
    }
}
