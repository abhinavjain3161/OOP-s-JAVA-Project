package com.smartcity.traffic;

import java.awt.Color;

/**
 * PublicBus class – represents a public bus in the traffic system.
 * Buses are slower and larger than cars.
 * Demonstrates inheritance and polymorphism.
 *
 * @author Arjun
 */
public class PublicBus extends Vehicle {

    /**
     * Constructor for PublicBus
     * @param x      Initial x-coordinate
     * @param y      Initial y-coordinate
     * @param laneID Lane identifier
     */
    public PublicBus(int x, int y, String laneID) {
        super(x, y, 2, new Color(255, 165, 0), laneID); // Speed: 2, Orange
    }

    /**
     * Polymorphic implementation of move() for PublicBus.
     * Buses stop at red lights BEFORE the intersection,
     * but always clear it if already inside.
     * @param signalState Current traffic signal state
     */
    @Override
    public void move(String signalState) {
        if (shouldStop(signalState)) {
            isMoving = false;
            return;
        }
        isMoving = true;
        moveInLaneDirection(speed);
    }

    @Override
    public String getVehicleType() { return "Public Bus"; }
}
