package com.smartcity.traffic;

/**
 * EmergencyVehicle interface for vehicles with emergency privileges.
 */
public interface EmergencyVehicle {
    /**
     * Returns true if the vehicle is on an emergency call.
     */
    boolean isOnEmergency();

    /**
     * Set the emergency status of the vehicle.
     */
    void setOnEmergency(boolean status);
}
