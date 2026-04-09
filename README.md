

# Smart City Traffic Simulator

An interactive Java Swing application demonstrating smart traffic control logic and advanced OOP principles.

## Project Overview

This project simulates a smart city intersection with a GUI, dynamic signal management, and multiple vehicle types. It is designed to showcase correct OOP design, encapsulation, inheritance, polymorphism, interface/abstract class usage, exception handling, file handling, and package organization.

## Features

- GUI-based simulation (Java Swing)
- 4-way intersection (North, East, South, West)
- Vehicle types: Private Car, Public Bus, Motorcycle, Ambulance
- Emergency override: Ambulance gets priority
- Smart mode for adaptive signal timing
- Real-time statistics and lane counts
- File handling for saving/loading state (extendable)
- Custom exception handling
- Organized into multiple packages (`traffic`, `traffic.util`)

## OOP Concepts Demonstrated

- **Encapsulation:** All data members are private/protected, with getters/setters and validation
- **Inheritance:** Vehicle hierarchy with method overriding and use of `super`
- **Polymorphism:** Base class references for all vehicles, runtime method dispatch
- **Interface:** `EmergencyVehicle` interface implemented by Ambulance
- **Abstract Class:** `Vehicle` is abstract with abstract methods
- **Constructor Overloading:** Multiple constructors in vehicle classes
- **Method Overloading:** Demonstrated in vehicle classes
- **Collections Framework:** Uses `ArrayList`, `HashMap` for vehicles and signals
- **Exception Handling:** Try-catch blocks and a user-defined exception (`InvalidVehicleDataException`)
- **File Handling:** (Extendable) for saving/loading traffic state
- **Packages:** At least two custom packages (`com.smartcity.traffic`, `com.smartcity.traffic.util`)

## Project Structure

```
JAVAProj/
├── src/
│   └── com/
│       └── smartcity/
│           └── traffic/
│               ├── Main.java                # Application entry point
│               ├── TrafficSimulatorGUI.java # Main GUI
│               ├── TrafficController.java   # Core logic
│               ├── TrafficLight.java        # Signal management
│               ├── Vehicle.java             # Abstract base class
│               ├── PrivateCar.java          # Car vehicle
│               ├── PublicBus.java           # Bus vehicle
│               ├── Motorcycle.java          # Motorcycle vehicle
│               ├── Ambulance.java           # Emergency vehicle
│               ├── EmergencyVehicle.java    # Interface
│               └── util/
│                   └── InvalidVehicleDataException.java # Custom exception
└── README.md
```

## How to Compile and Run

### Prerequisites
- Java Development Kit (JDK) 8 or higher

### Compilation

Open a terminal and run:
```bash
cd "/Users/anupamkanoongo/Documents/Developer's Drive/JAVAProj/src"
javac com/smartcity/traffic/*.java com/smartcity/traffic/util/*.java
```

### Execution
```bash
cd "/Users/anupamkanoongo/Documents/Developer's Drive/JAVAProj/src"
java com.smartcity.traffic.Main
```

## Usage

1. Launch the application. The GUI will open.
2. Use the control panel to:
   - Spawn vehicles (Car, Bus, Motorcycle, Ambulance)
   - Toggle smart mode
   - Pause/resume simulation
   - Clear all traffic
   - Adjust simulation speed
3. Observe real-time updates, statistics, and emergency overrides in the GUI.

## Documentation

- **OOP Concepts Used:** See code comments and class structure
- **Class Diagram:** (See report)
- **Package Structure:** See above
- **Sample Outputs:** Provided via GUI

## Future Enhancements
- Add persistent file save/load for simulation state
- More vehicle types and behaviors
- Enhanced statistics and reporting
- Improved smart signal algorithms

---
**Project Date**: March 24, 2026  
**Java Version**: Compatible with JDK 8+  
**License**: Educational Project
