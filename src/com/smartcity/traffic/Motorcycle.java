package com.smartcity.traffic;

import java.awt.Color;

/**
 * Motorcycle class – represents a motorcycle in the traffic system.
 * Motorcycles are the fastest vehicle type and treat yellow lights as green
 * (they accelerate through yellow lights rather than slowing down).
 *
 * Demonstrates OOP extensibility: adding a new vehicle type requires only
 * a new subclass – no changes to the controller or GUI drawing pipeline.
 *
 * @author Arjun
 */
public class Motorcycle extends Vehicle {

    /**
     * Constructor for Motorcycle
     * 
     * @param x      Initial x-coordinate
     * @param y      Initial y-coordinate
     * @param laneID Lane identifier
     */
    public Motorcycle(int x, int y, String laneID) {
        super(x, y, 5, new Color(255, 220, 50), laneID); // Speed: 5 (fastest), Yellow
    }

    /**
     * Polymorphic implementation of move() for Motorcycle.
     * Motorcycles stop at red lights before the intersection (like cars),
     * but treat YELLOW as green and accelerate through it.
     * They always clear the intersection if already inside.
     * 
     * @param signalState Current traffic signal state
     */
    @Override
    public void move(String signalState) {
        // Always clear the intersection if already inside
        if (isInIntersection()) {
            isMoving = true;
            moveInLaneDirection(speed);
            return;
        }

        // Stop only at RED, not at YELLOW (motorcycles push through yellow)
        if ("RED".equals(signalState) && isApproachingIntersection()) {
            isMoving = false;
            return;
        }

        isMoving = true;
        moveInLaneDirection(speed);
    }

    @Override
    public String getVehicleType() {
        return "Motorcycle";
    }
}
