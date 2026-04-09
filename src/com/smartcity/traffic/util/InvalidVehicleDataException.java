package com.smartcity.traffic.util;

/**
 * Custom exception for invalid vehicle data.
 */
public class InvalidVehicleDataException extends Exception {
    public InvalidVehicleDataException(String message) {
        super(message);
    }
}
