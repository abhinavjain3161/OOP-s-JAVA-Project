package com.smartcity.traffic;

import com.smartcity.traffic.util.InvalidVehicleDataException;

import java.awt.Color;

/**
 * Ambulance class – represents an emergency ambulance in the traffic system.
 * Ambulances have priority and trigger emergency override in traffic signals.
 * Demonstrates inheritance and polymorphism.
 *
 * @author Arjun
 */
public class Ambulance extends Vehicle implements EmergencyVehicle {
    private boolean emergencyMode;
    private boolean usingOppositeLane;
    private boolean bypassRequested;

    /**
     * Constructor for Ambulance
     * @param x      Initial x-coordinate
     * @param y      Initial y-coordinate
     * @param laneID Lane identifier
     */
    // Default constructor – emergency mode ON by default
    public Ambulance() {
        super(0, 0, 4, new Color(255, 0, 0), "NORTH");
        this.emergencyMode = true;
        this.usingOppositeLane = false;
        this.bypassRequested = false;
    }

    // Parameterized constructor – emergency mode ON by default
    public Ambulance(int x, int y, String laneID) {
        super(x, y, 4, new Color(255, 0, 0), laneID);
        this.emergencyMode = true;
        this.usingOppositeLane = false;
        this.bypassRequested = false;
    }

    // Overloaded constructor with emergency flag
    public Ambulance(int x, int y, String laneID, boolean emergencyMode) {
        super(x, y, 4, new Color(255, 0, 0), laneID);
        this.emergencyMode = emergencyMode;
        this.usingOppositeLane = false;
        this.bypassRequested = false;
    }

    /**
     * Polymorphic implementation of move() for Ambulance.
     * Ambulances always clear the intersection if inside.
     * At a red light they slow down but never fully stop (emergency mode).
     * @param signalState Current traffic signal state
     */
    @Override
    public void move(String signalState) {
        if (!emergencyMode) {
            if (shouldStop(signalState)) {
                isMoving = false;
                return;
            }
            isMoving = true;
            moveInLaneDirection(speed);
            return;
        }

        // If already in intersection, always clear it at full speed
        if (isInIntersection()) {
            isMoving = true;
            moveInLaneDirection(speed);
            return;
        }

        // User-requested behavior: bypass only when there is a vehicle ahead.
        usingOppositeLane = bypassRequested;

        int targetLateral = usingOppositeLane ? getOppositeLaneCoordinate() : getBaseLaneCoordinate();
        moveLaterallyToward(targetLateral, 3);

        if ("RED".equals(signalState) && isApproachingIntersection() && !usingOppositeLane) {
            isMoving = true;
            moveInLaneDirection(Math.max(1, speed / 2));
            return;
        }

        isMoving = true;
        moveInLaneDirection(speed);
    }

    private int getBaseLaneCoordinate() {
        switch (laneID) {
            case "NORTH":
                return INTERSECTION_CENTER_X + NORTH_LANE_OFFSET;
            case "SOUTH":
                return INTERSECTION_CENTER_X - SOUTH_LANE_OFFSET;
            case "EAST":
                return INTERSECTION_CENTER_Y + EAST_LANE_OFFSET;
            case "WEST":
                return INTERSECTION_CENTER_Y - WEST_LANE_OFFSET;
            default:
                return 0;
        }
    }

    private int getOppositeLaneCoordinate() {
        switch (laneID) {
            case "NORTH":
                return INTERSECTION_CENTER_X - SOUTH_LANE_OFFSET;
            case "SOUTH":
                return INTERSECTION_CENTER_X + NORTH_LANE_OFFSET;
            case "EAST":
                return INTERSECTION_CENTER_Y - WEST_LANE_OFFSET;
            case "WEST":
                return INTERSECTION_CENTER_Y + EAST_LANE_OFFSET;
            default:
                return getBaseLaneCoordinate();
        }
    }

    private void moveLaterallyToward(int target, int step) {
        if ("NORTH".equals(laneID) || "SOUTH".equals(laneID)) {
            if (x < target) {
                x = Math.min(target, x + step);
            } else if (x > target) {
                x = Math.max(target, x - step);
            }
        } else {
            if (y < target) {
                y = Math.min(target, y + step);
            } else if (y > target) {
                y = Math.max(target, y - step);
            }
        }
    }

    // EmergencyVehicle interface implementation
    @Override
    public boolean isOnEmergency() { return emergencyMode; }

    @Override
    public void setOnEmergency(boolean status) { this.emergencyMode = status; }

    public void setBypassRequested(boolean bypassRequested) { this.bypassRequested = bypassRequested; }

    public boolean isUsingOppositeLane() { return usingOppositeLane; }

    /** @deprecated Use {@link #isOnEmergency()} */
    @Deprecated
    public boolean isEmergencyMode() { return emergencyMode; }
    /** @deprecated Use {@link #setOnEmergency(boolean)} */
    @Deprecated
    public void setEmergencyMode(boolean emergencyMode) { this.emergencyMode = emergencyMode; }

    @Override
    public String getVehicleType() { return "Ambulance"; }
    
    // Example of validation using custom exception
    public void setSpeed(int speed) throws InvalidVehicleDataException {
        if (speed < 1 || speed > 10) {
            throw new InvalidVehicleDataException("Ambulance speed must be between 1 and 10.");
        }
        this.speed = speed;
    }
}