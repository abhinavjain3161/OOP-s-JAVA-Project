
package com.smartcity.traffic;

import java.awt.Color;

/**
 * PrivateCar class – represents a private car in the traffic system.
 * Demonstrates inheritance and polymorphism.
 *
 * @author Arjun
 */
public class PrivateCar extends Vehicle {
    /** Default constructor – classic light-blue car. */
    public PrivateCar() {
        super(0, 0, 3, new Color(100, 150, 200), "NORTH");
    }

    /** Parameterized constructor – classic light-blue car. */
    public PrivateCar(int x, int y, String laneID) {
        super(x, y, 3, new Color(100, 150, 200), laneID); // Speed: 3, Light blue
    }

    /** Overloaded constructor – allows a custom colour for visual variety. */
    public PrivateCar(int x, int y, String laneID, Color color) {
        super(x, y, 3, color, laneID);
    }

    /**
     * Polymorphic implementation of move() for PrivateCar.
     * Private cars stop at red lights BEFORE the intersection,
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
    public String getVehicleType() { return "Private Car"; }
}
