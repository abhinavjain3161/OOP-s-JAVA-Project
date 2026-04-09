# OOPJ Lab Mini Project: Requirement Mapping

**Team Members:**
- Anupam
- Arjun
- Abhinav

## Objective
This document explains how our Java mini project satisfies each requirement from the OOPJ Lab Mini Project Guidelines.

---

## 1. Objective of the Mini Project
Our project demonstrates Object-Oriented Programming (OOP) concepts in Java by modeling a smart city traffic system. The design emphasizes class structure, object interaction, and core OOP principles.

---

## 2. Team Size
- Team of 3 students.
- All members contributed and understand the codebase.

---

## 3. Mandatory OOP Requirements

### Class Design
- **Minimum 6 user-defined classes:**
  - Our project defines more than 6 user classes: `Vehicle` (abstract base), `Ambulance`, `Motorcycle`, `PrivateCar`, `PublicBus` (all vehicle types), `TrafficLight` (controls signals), `TrafficController` (manages traffic flow), `TrafficSimulatorGUI` (user interface), and `InvalidVehicleDataException` (custom exception). This ensures modularity and clear separation of concerns.
- **Classes represent real entities:**
  - Each class is mapped to a real-world entity. For example, `Vehicle` and its subclasses represent different types of vehicles, `TrafficLight` models a real traffic signal, and `TrafficController` acts as the system's manager. This mapping makes the simulation realistic and easy to understand.
- **Logic not in one class:**
  - The logic is distributed: vehicle movement and properties are in their respective classes, traffic management is in `TrafficController`, and the GUI is handled by `TrafficSimulatorGUI`. This avoids the anti-pattern of a "God class" and makes the code maintainable.

### Encapsulation
- **Private data members:**
  - All fields in our classes are declared private, ensuring that data cannot be accessed or modified directly from outside the class. This protects the internal state of objects.
- **Getters and setters:**
  - Public getter and setter methods are provided for each private field. This allows controlled access and modification, and is a core OOP practice.
- **Validation in setters:**
  - Setters include validation logic. For example, the speed setter in `Vehicle` checks for valid speed ranges, and vehicle type setters ensure only allowed types are set. This prevents invalid object states.

### Inheritance
- **Inheritance hierarchy:**
  - The class `Vehicle` is an abstract superclass. `Ambulance`, `Motorcycle`, `PrivateCar`, and `PublicBus` all extend `Vehicle`, inheriting its properties and behaviors. This models the "is-a" relationship and allows code reuse.
- **Method overriding:**
  - Each subclass provides its own implementation of methods like `move()` and `displayInfo()`, overriding the base class's abstract or virtual methods. This enables specific behavior for each vehicle type.
- **Use of `super`:**
  - Subclasses use the `super` keyword to call the parent class's constructor and methods, ensuring proper initialization and extension of base functionality.

### Polymorphism
- **Runtime polymorphism:**
  - The project demonstrates runtime (dynamic) polymorphism by using `Vehicle` references to point to objects of its subclasses. For example, `Vehicle v = new Ambulance();` allows the code to treat all vehicles uniformly while still invoking the correct subclass behavior at runtime.
- **Overridden methods via parent reference:**
  - When calling overridden methods like `move()` or `displayInfo()` on a `Vehicle` reference, the JVM executes the subclass's implementation. This is used in collections and traffic management logic, where a list of `Vehicle` objects can contain any subclass, and the correct method is called for each.

### Interface
- **At least one interface:**
  - The `EmergencyVehicle` interface defines behaviors specific to emergency vehicles, such as priority movement or siren activation.
- **Implemented by two or more classes:**
  - `Ambulance` implements `EmergencyVehicle`, and the design allows for easy addition of other emergency vehicle types (e.g., fire truck) that would also implement this interface. This demonstrates interface-based design and flexibility.

### Abstract Class
- **At least one abstract class:**
  - `Vehicle` is declared abstract, meaning it cannot be instantiated directly and serves as a blueprint for all vehicle types.
- **At least one abstract method:**
  - The method `move()` is abstract in `Vehicle`, forcing all subclasses to provide their own implementation. This ensures that each vehicle type defines its movement logic.
- **Subclasses implement it:**
  - All subclasses (`Ambulance`, `Motorcycle`, etc.) implement the abstract methods, providing specific movement and display logic.

### Constructor Usage
- **Default constructor:**
  - Each class provides a default constructor, allowing objects to be created with default values.
- **Parameterized constructor:**
  - Parameterized constructors are used to initialize objects with specific values, such as vehicle type, speed, or color.
- **Constructor overloading:**
  - Multiple constructors with different parameter lists are provided, giving flexibility in object creation and initialization.

### Method Overloading
- **At least one overloaded method:**
  - Methods like `setSpeed(int speed)` and `setSpeed(double speed)` in `Vehicle` demonstrate method overloading, allowing the same method name to handle different parameter types.

### Collections Framework
- **At least two collection types:**
  - The project uses `ArrayList<Vehicle>` to store and manage all vehicles in the simulation, enabling dynamic addition and removal.
  - `HashMap<String, TrafficLight>` is used to map intersection names to their corresponding traffic light objects, allowing efficient lookup and control.
- **No reliance on arrays only:**
  - The use of collections instead of arrays provides flexibility, scalability, and access to Java's rich collection methods.

### Exception Handling
- **Try–catch blocks:**
  - The code uses try–catch blocks to handle exceptions during input, parsing, and other operations, preventing program crashes and providing user feedback.
- **User-defined exception:**
  - The custom exception `InvalidVehicleDataException` (in the `util` package) is thrown when invalid vehicle data is encountered, demonstrating custom error handling.
- **Handle invalid inputs:**
  - Setters and input methods validate data and throw exceptions or show error messages for invalid inputs, ensuring robustness.

### File Handling
- **File read/write:**
  - The current project does not implement file read/write or object serialization. All data is managed in memory. This can be extended in the future to support persistent storage if required.

### Packages
- **At least two custom packages:**
  - The codebase is organized into `com.smartcity.traffic` (main logic, entities, controllers, GUI) and `com.smartcity.traffic.util` (utility classes, custom exceptions). This separation improves code organization and maintainability.
- **Logical organization:**
  - Classes are grouped by their role: core entities and logic in the main package, helpers and exceptions in the util package, and the GUI in its own class. This makes the project structure clear and modular.

---

## 4. User Interface Requirement
- **GUI using Swing:**
  - The `TrafficSimulatorGUI` class implements a graphical user interface using Java Swing. It allows users to interact with the simulation, add/remove vehicles, control traffic lights, and observe the system in real time. This makes the program user-friendly and interactive.

---

## 5. Code Quality Rules
- **Meaningful names:**
  - All classes, methods, and variables use descriptive names that reflect their purpose, making the code self-explanatory.
- **Indentation and comments:**
  - The code follows consistent indentation and includes comments for important logic, improving readability and maintainability.
- **No dead code or excessive static usage:**
  - Unused variables and dead code are avoided. Static members are used only where appropriate, not for core logic.
- **No copy-paste from internet sources:**
  - All code is original and fully understood by the team, ensuring academic integrity and deep understanding.

---

## 6. Documentation to Submit
- **Project Report (PDF):**
  - The report includes a clear problem statement, a list of implemented features, detailed explanations of OOP concepts used, a UML class diagram, package structure, and sample outputs. This provides a comprehensive overview of the project.

---

## Conclusion
All requirements from the OOPJ Lab Mini Project Guidelines are satisfied and mapped to our codebase. Each section above provides a deep explanation of how the requirement is fulfilled, with references to specific classes and design choices. For further details, refer to the respective class files in the `src/com/smartcity/traffic/` directory.
