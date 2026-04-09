package com.smartcity.traffic;

/**
 * TrafficLight class – represents a single traffic signal at the intersection.
 * Manages signal states and timing.
 *
 * @author Abhinav
 */
public class TrafficLight {
    private String laneID;       // "NORTH", "EAST", "SOUTH", "WEST"
    private String currentState; // "GREEN", "RED", "YELLOW"
    private int greenTime;       // Time allocated for green signal (in seconds)
    private int timeRemaining;   // Time remaining in current state

    private static final int YELLOW_TIME = 3; // Yellow light duration

    /**
     * Constructor for TrafficLight
     * @param laneID         Lane identifier
     * @param initialGreenTime Initial green light duration
     */
    public TrafficLight(String laneID, int initialGreenTime) {
        this.laneID = laneID;
        this.greenTime = initialGreenTime;
        this.currentState = "RED";
        this.timeRemaining = 0;
    }

    /** Activate green light for this traffic signal. */
    public void activateGreen(int duration) {
        this.currentState = "GREEN";
        this.greenTime = duration;
        this.timeRemaining = duration;
    }

    /** Transition to yellow light. */
    public void activateYellow() {
        this.currentState = "YELLOW";
        this.timeRemaining = YELLOW_TIME;
    }

    /** Transition to red light. */
    public void activateRed() {
        this.currentState = "RED";
        this.timeRemaining = 0;
    }

    /** Decrement time remaining by 1 second. */
    public void tick() {
        if (timeRemaining > 0) {
            timeRemaining--;
        }
    }

    // ── Convenience state checkers ───────────────────────────────────────────

    public boolean isGreen()  { return "GREEN".equals(currentState); }
    public boolean isYellow() { return "YELLOW".equals(currentState); }
    public boolean isRed()    { return "RED".equals(currentState); }

    // ── Getters and Setters ──────────────────────────────────────────────────

    public String getLaneID()       { return laneID; }
    public String getCurrentState() { return currentState; }
    public int getTimeRemaining()   { return timeRemaining; }
    public int getGreenTime()       { return greenTime; }

    public void setGreenTime(int greenTime)         { this.greenTime = greenTime; }
    public void setTimeRemaining(int timeRemaining) { this.timeRemaining = timeRemaining; }

    public static int getYellowTime() { return YELLOW_TIME; }
}
