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
    }

    // Parameterized constructor – emergency mode ON by default
    public Ambulance(int x, int y, String laneID) {
        super(x, y, 4, new Color(255, 0, 0), laneID);
        this.emergencyMode = true;
    }

    // Overloaded constructor with emergency flag
    public Ambulance(int x, int y, String laneID, boolean emergencyMode) {
        super(x, y, 4, new Color(255, 0, 0), laneID);
        this.emergencyMode = emergencyMode;
    }

    /**
     * Polymorphic implementation of move() for Ambulance.
     * Ambulances always clear the intersection if inside.
     * At a red light they slow down but never fully stop (emergency mode).
     * @param signalState Current traffic signal state
     */
    @Override
    public void move(String signalState) {
        // If already in intersection, always clear it at full speed
        if (isInIntersection()) {
            isMoving = true;
            moveInLaneDirection(speed);
            return;
        }

        // Not in intersection – check if approaching a red light
        boolean approachingRed = "RED".equals(signalState) && isApproachingIntersection();

        if (approachingRed && emergencyMode) {
            // Ambulances slow to half speed at red but never stop
            isMoving = true;
            moveInLaneDirection(speed / 2);
        } else if (approachingRed) {
            // Emergency mode off – behave like a normal vehicle
            isMoving = false;
        } else {
            // Green or yellow – move at full speed
            isMoving = true;
            moveInLaneDirection(speed);
        }
    }

    // EmergencyVehicle interface implementation
    @Override
    public boolean isOnEmergency() { return emergencyMode; }

    @Override
    public void setOnEmergency(boolean status) { this.emergencyMode = status; }

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
