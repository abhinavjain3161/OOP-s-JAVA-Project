package com.smartcity.traffic;

import javax.swing.SwingUtilities;

/**
 * Main class to launch the Smart City Traffic Simulator application.
 *
 * Project Contributors:
 * - Arjun : Vehicle Hierarchy and Polymorphism
 * - Abhinav: Circular Smart Logic and Traffic Controller
 * - Anupam : GUI and Interactivity
 *
 * Features:
 * 1. Vehicle Hierarchy with Polymorphism (Abstract Vehicle class)
 * 2. Fixed 80-second cycle with circular rotation (N → E → S → W)
 * 3. Yellow phase between green and red
 * 4. Smart redistribution when lanes are empty
 * 5. Emergency override for ambulances (gives ambulance lane green ASAP)
 * 6. Motorcycle vehicle type (speed 5, treats yellow as green)
 * 7. Interactive GUI: Pause/Resume, Speed slider, per-lane counts
 *
 * @version 1.1
 * @since 2026-02-08
 */
public class Main {
    public static void main(String[] args) {
        printWelcomeMessage();

        SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.UIManager.setLookAndFeel(
                        javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Could not set system look and feel: " + e.getMessage());
            }

            TrafficSimulatorGUI gui = new TrafficSimulatorGUI();
            gui.setVisible(true);

            System.out.println("\n✓ Smart City Traffic Simulator v1.1 is now running!");
            System.out.println("✓ Use the control panel to interact with the simulation.");
        });
    }

    private static void printWelcomeMessage() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║    SMART CITY TRAFFIC SIMULATOR v1.1 - OOPS PROJECT       ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║                                                            ║");
        System.out.println("║  Contributors:                                             ║");
        System.out.println("║    • Arjun   : Vehicle Hierarchy & Polymorphism            ║");
        System.out.println("║    • Abhinav : Circular Smart Traffic Logic                ║");
        System.out.println("║    • Anupam  : GUI & Interactivity                         ║");
        System.out.println("║                                                            ║");
        System.out.println("║  Key Features:                                             ║");
        System.out.println("║    ✓ Fixed 80-second traffic cycle                         ║");
        System.out.println("║    ✓ Circular signal rotation (N→E→S→W)                    ║");
        System.out.println("║    ✓ Yellow phase between green and red                    ║");
        System.out.println("║    ✓ Smart redistribution for empty lanes                  ║");
        System.out.println("║    ✓ Emergency override for ambulances                     ║");
        System.out.println("║    ✓ Motorcycle vehicle type (speed 5, runs yellow)        ║");
        System.out.println("║    ✓ Pause/Resume & simulation speed control               ║");
        System.out.println("║    ✓ Per-lane vehicle count display                        ║");
        System.out.println("║                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println("\nInitialising simulation...\n");
    }
}
