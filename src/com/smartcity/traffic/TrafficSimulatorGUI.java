package com.smartcity.traffic;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

/**
 * TrafficSimulatorGUI – Main GUI for the Smart City Traffic Simulator.
 * Implements the visual interface with:
 * 1. 4-way intersection display with direction arrows
 * 2. Traffic lights with yellow phase and countdown timers
 * 3. Vehicle animation (Car, Bus, Motorcycle, Ambulance)
 * 4. Interactive control panel (Pause/Resume, Speed slider, per-lane counts)
 *
 * @author Anupam
 */
public class TrafficSimulatorGUI extends JFrame {
    private static final long serialVersionUID = 1L;
    // Responsive control panel width
    private final int controlPanelWidth;

    // ── GUI Components ────────────────────────────────────────────────────────
    private IntersectionPanel intersectionPanel;
    private JPanel northWestControls;
    private JPanel northEastControls;
    private JPanel southWestControls;
    private JPanel southEastControls;
    private JLabel currentLightLabel;
    private JLabel cycleTimerLabel;
    private JLabel smartModeLabel;
    private JLabel statsLabel;
    private JLabel densityLabel;
    private JLabel realtimeLabel;
    private JLabel timeBankLabel;
    private JLabel[] laneCountLabels; // per-lane vehicle counts
    private JLabel emergencyLabel;
    private JButton toggleSmartModeButton;
    private JButton[] spawnTrafficButtons;
    private JButton spawnAmbulanceButton;
    private JButton clearTrafficButton;
    private JButton pauseResumeButton;
    private JSlider speedSlider;

    // ── Simulation Components ─────────────────────────────────────────────────
    private TrafficController trafficController;
    private ArrayList<Vehicle> vehicles;
    private Timer simulationTimer;
    private Timer updateTimer;

    // ── Statistics ────────────────────────────────────────────────────────────
    private int totalVehiclesPassed;
    // Removed totalAmbulances; no longer tracked

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
    private static final int WINDOW_WIDTH = (int) (SCREEN_SIZE.width * 0.85);
    private static final int WINDOW_HEIGHT = (int) (SCREEN_SIZE.height * 0.85);
    private static final int INTERSECTION_SIZE = (int) (Math.min(WINDOW_WIDTH, WINDOW_HEIGHT) * 0.7);
    private static final int BASE_TIMER_DELAY = 50; // ms – base animation refresh
    private static final int UPDATE_DELAY = 1000; // ms – signal update rate
    private static final int ROAD_HALF_WIDTH = 70; // half of the 140px road width

    // Curated car colour palette for visual variety
    private static final Color[] CAR_COLORS = {
            new Color(100, 150, 200), // steel blue
            new Color(72, 201, 176), // teal
            new Color(155, 89, 182), // purple
            new Color(52, 152, 219), // dodger blue
            new Color(26, 188, 156), // green-sea
            new Color(241, 196, 15), // sunflower
            new Color(230, 126, 34), // carrot
            new Color(231, 76, 60), // alizarin
            new Color(46, 204, 113), // emerald
            new Color(149, 165, 166), // concrete
    };

    // ── Constructor ───────────────────────────────────────────────────────────

    public TrafficSimulatorGUI() {
        this.controlPanelWidth = (int) (WINDOW_WIDTH * 0.23);
        super("Smart City Traffic Simulator v1.1");

        vehicles = new ArrayList<>();
        trafficController = new TrafficController();
        trafficController.setVehicles(vehicles);

        // Configure Vehicle geometry to match the actual rendered panel size.
        // Both the collision/approach logic in Vehicle and the spawn coordinates
        // below derive from this single source of truth.
        int panelCenter = INTERSECTION_SIZE / 2;
        Vehicle.setIntersectionParams(panelCenter, panelCenter, ROAD_HALF_WIDTH);
        // ── Per-lane offsets (px from road centre) – tune each direction independently
        // ──
        Vehicle.NORTH_LANE_OFFSET = 35;
        Vehicle.SOUTH_LANE_OFFSET = 35;
        Vehicle.EAST_LANE_OFFSET = 35;
        Vehicle.WEST_LANE_OFFSET = 35;

        totalVehiclesPassed = 0;

        setupFrame();
        createControlPanel();
        createIntersectionPanel();
        startSimulation();
    }

    // ── Frame setup ───────────────────────────────────────────────────────────

    private void setupFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── Control panel ─────────────────────────────────────────────────────────

    private void createControlPanel() {
        int overlayWidth = controlPanelWidth;

        // Title
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(30, 30, 33));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        titlePanel.setMaximumSize(new Dimension(overlayWidth, 60));
        JLabel titleLabel = new JLabel("🚦 TRAFFIC CONTROL", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(100, 200, 255));
        titlePanel.add(titleLabel);

        // ── Status panel ──────────────────────────────────────────────────────
        JPanel statusPanel = createStyledPanel("STATUS");

        currentLightLabel = createStyledLabel("Current: NORTH (20s)", Color.GREEN);
        statusPanel.add(currentLightLabel);

        cycleTimerLabel = createStyledLabel("Cycle: 0/80s", new Color(100, 200, 255));
        statusPanel.add(cycleTimerLabel);

        smartModeLabel = createStyledLabel("Smart Mode: ON", new Color(255, 220, 100));
        statusPanel.add(smartModeLabel);

        realtimeLabel = createStyledLabel("⚡ Real-time: Active", new Color(100, 255, 100));
        statusPanel.add(realtimeLabel);

        timeBankLabel = createStyledLabel("💰 Time Bank: 0s", new Color(255, 215, 0));
        statusPanel.add(timeBankLabel);

        // ── Per-lane counts panel ─────────────────────────────────────────────
        JPanel lanePanel = createStyledPanel("LANE VEHICLES");
        String[] lanes = TrafficController.getLaneSequence();
        // Swap NS and EW for display
        String[] laneIcons = { "⬇ S", "⬅ W", "⬆ N", "➡ E" };
        laneCountLabels = new JLabel[lanes.length];
        for (int i = 0; i < lanes.length; i++) {
            laneCountLabels[i] = createStyledLabel(laneIcons[i] + ": 0", new Color(180, 220, 255));
            lanePanel.add(laneCountLabels[i]);
        }

        // ── Statistics panel ──────────────────────────────────────────────────
        JPanel statisticsPanel = createStyledPanel("STATISTICS");

        statsLabel = createStyledLabel("Active: 0 | Passed: 0", new Color(150, 255, 150));
        statisticsPanel.add(statsLabel);

        densityLabel = createStyledLabel("Density: Low", new Color(200, 200, 200));
        statisticsPanel.add(densityLabel);

        emergencyLabel = createStyledLabel("🚨 Overrides: 0", new Color(255, 150, 150));
        statisticsPanel.add(emergencyLabel);

        // ── Controls panel ────────────────────────────────────────────────────
        JPanel controlsPanel = createStyledPanel("CONTROLS");

        toggleSmartModeButton = createStyledButton("⚡ Toggle Smart Mode", new Color(255, 193, 7));
        toggleSmartModeButton.addActionListener(e -> toggleSmartMode());
        controlsPanel.add(toggleSmartModeButton);
        controlsPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        pauseResumeButton = createStyledButton("⏸ Pause", new Color(52, 152, 219));
        pauseResumeButton.addActionListener(e -> togglePause());
        controlsPanel.add(pauseResumeButton);
        controlsPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        clearTrafficButton = createStyledButton("🗑 Clear All Traffic", new Color(220, 53, 69));
        clearTrafficButton.addActionListener(e -> clearAllTraffic());
        controlsPanel.add(clearTrafficButton);
        controlsPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Speed slider
        JLabel speedLabel = createStyledLabel("Speed:", new Color(200, 200, 200));
        controlsPanel.add(speedLabel);
        speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 4, 2);
        speedSlider.setMajorTickSpacing(1);
        speedSlider.setPaintTicks(true);
        speedSlider.setSnapToTicks(true);
        speedSlider.setBackground(new Color(30, 30, 33));
        speedSlider.setForeground(new Color(200, 200, 200));
        speedSlider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        speedSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel[] speedLabels = {
                new JLabel("0.5×"), new JLabel("1×"), new JLabel("2×"), new JLabel("4×")
        };
        Hashtable<Integer, JLabel> speedLabelTable = new Hashtable<>();
        for (JLabel sl : speedLabels) {
            sl.setForeground(new Color(150, 150, 150));
            sl.setFont(new Font("Arial", Font.PLAIN, 9));
        }
        speedLabelTable.put(1, speedLabels[0]);
        speedLabelTable.put(2, speedLabels[1]);
        speedLabelTable.put(3, speedLabels[2]);
        speedLabelTable.put(4, speedLabels[3]);
        speedSlider.setLabelTable(speedLabelTable);
        speedSlider.setPaintLabels(true);
        speedSlider.addChangeListener(e -> applySpeedSetting(speedSlider.getValue()));
        controlsPanel.add(speedSlider);

        // ── Spawn panel ───────────────────────────────────────────────────────
        JPanel spawnPanel = createStyledPanel("SPAWN VEHICLES");

        spawnTrafficButtons = new JButton[4];
        String[] laneNames = TrafficController.getLaneSequence();
        // Interchange icons and colors for North<->South and East<->West
        // Arrows now match the 'From' direction (where vehicles originate)
        String[] icons = { "⬆️", "➡️", "⬇️", "⬅️" };
        Color[] btnColors = {
            new Color(155, 89, 182), // South (was North)
            new Color(241, 196, 15), // West (was East)
            new Color(52, 152, 219), // North (was South)
            new Color(46, 204, 113)  // East (was West)
        };

        for (int i = 0; i < laneNames.length; i++) {
            final String lane = laneNames[i];
            // Interchange button labels for North<->South and East<->West
            String displayLane;
            switch (lane) {
                case "NORTH": displayLane = "South"; break;
                case "SOUTH": displayLane = "North"; break;
                case "EAST":  displayLane = "West";  break;
                case "WEST":  displayLane = "East";  break;
                default: displayLane = lane.charAt(0) + lane.substring(1).toLowerCase();
            }
            String label = icons[i] + " From " + displayLane;
            spawnTrafficButtons[i] = createStyledButton(label, btnColors[i]);
            spawnTrafficButtons[i].addActionListener(e -> spawnHeavyTraffic(lane));
            spawnPanel.add(spawnTrafficButtons[i]);
            if (i < 3)
                spawnPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        // ── Emergency panel ───────────────────────────────────────────────────
        JPanel emergencyPanel = createStyledPanel("EMERGENCY");

        spawnAmbulanceButton = createStyledButton("🚑 Spawn Ambulance", new Color(231, 76, 60));
        spawnAmbulanceButton.setFont(new Font("Arial", Font.BOLD, 13));
        spawnAmbulanceButton.addActionListener(e -> spawnAmbulance());
        emergencyPanel.add(spawnAmbulanceButton);

        // ── Legend panel (moved into NW control box for visual uniformity) ──
        JPanel legendPanel = createStyledPanel("LEGEND");
        legendPanel.add(createLegendItem("Private Car", "CAR"));
        legendPanel.add(createLegendItem("Public Bus", "BUS"));
        legendPanel.add(createLegendItem("Motorcycle", "MOTORCYCLE"));
        legendPanel.add(createLegendItem("Ambulance", "AMBULANCE"));

        // Scatter controls across four grass quadrants.
        northWestControls = createQuadrantContainer();
        northWestControls.setLayout(new BorderLayout(0, 10));
        northWestControls.add(titlePanel, BorderLayout.NORTH);
        JPanel northWestContent = new JPanel();
        northWestContent.setOpaque(false);
        northWestContent.setLayout(new BoxLayout(northWestContent, BoxLayout.Y_AXIS));
        northWestContent.add(legendPanel);
        northWestContent.add(Box.createRigidArea(new Dimension(0, 10)));
        northWestContent.add(statusPanel);
        northWestControls.add(northWestContent, BorderLayout.CENTER);

        northEastControls = createQuadrantContainer();
        northEastControls.add(lanePanel);
        northEastControls.add(Box.createRigidArea(new Dimension(0, 10)));
        northEastControls.add(statisticsPanel);

        southWestControls = createQuadrantContainer();
        southWestControls.add(controlsPanel);

        southEastControls = createQuadrantContainer();
        southEastControls.add(spawnPanel);
        southEastControls.add(Box.createRigidArea(new Dimension(0, 10)));
        southEastControls.add(emergencyPanel);
    }

    // ── Intersection panel ────────────────────────────────────────────────────

    private void createIntersectionPanel() {
        intersectionPanel = new IntersectionPanel();
        add(intersectionPanel, BorderLayout.CENTER);

        if (northWestControls != null) {
            intersectionPanel.add(northWestControls);
            intersectionPanel.add(northEastControls);
            intersectionPanel.add(southWestControls);
            intersectionPanel.add(southEastControls);
            SwingUtilities.invokeLater(this::updateQuadrantControlBounds);
        }
    }

    private void updateQuadrantControlBounds() {
        if (intersectionPanel == null || northWestControls == null)
            return;

        int panelW = intersectionPanel.getWidth();
        int panelH = intersectionPanel.getHeight();
        int cx = panelW / 2;
        int cy = panelH / 2;
        int margin = 16;
        int centerClearance = ROAD_HALF_WIDTH + 50;

        int leftX = margin;
        int rightX = cx + centerClearance + margin;
        int topY = margin;
        int bottomY = cy + centerClearance + margin;

        int leftW = (cx - centerClearance) - (2 * margin);
        int rightW = panelW - rightX - margin;
        int topH = (cy - centerClearance) - (2 * margin);
        int bottomH = panelH - bottomY - margin;

        if (leftW <= 0 || rightW <= 0 || topH <= 0 || bottomH <= 0)
            return;

        // NW box starts at top for consistent block alignment with other quadrants.
        int nwY = topY;
        // Stretch NW panel down to just above the horizontal road with padding.
        int roadTopY = cy - ROAD_HALF_WIDTH;
        int nwBottomPadding = 12;
        int nwBottom = roadTopY - nwBottomPadding;
        int nwH = nwBottom - nwY;
        if (nwH <= 80) {
            nwY = topY;
            nwH = topH;
        }

        northWestControls.setBounds(leftX, nwY, leftW, nwH);
        northEastControls.setBounds(rightX, topY, rightW, topH);
        southWestControls.setBounds(leftX, bottomY, leftW, bottomH);

        // Keep the SE box closer to the road and taller for spawn/emergency controls.
        int seTopPaddingFromRoad = 8;
        int seTopY = cy + ROAD_HALF_WIDTH + seTopPaddingFromRoad;
        int seHeight = panelH - seTopY - margin;
        if (seHeight <= 80) {
            southEastControls.setBounds(rightX, bottomY, rightW, bottomH);
        } else {
            southEastControls.setBounds(rightX, seTopY, rightW, seHeight);
        }
    }

    // ── Simulation timers ─────────────────────────────────────────────────────

    private void startSimulation() {
        simulationTimer = new Timer(BASE_TIMER_DELAY, e -> {
            updateVehicles();
            intersectionPanel.repaint();
        });
        simulationTimer.start();

        updateTimer = new Timer(UPDATE_DELAY, e -> {
            trafficController.update();
            updateControlPanelLabels();
        });
        updateTimer.start();
    }

    // ── Vehicle update ────────────────────────────────────────────────────────

    private void updateVehicles() {
        if (trafficController.isPaused())
            return;

        for (int i = vehicles.size() - 1; i >= 0; i--) {
            Vehicle vehicle = vehicles.get(i);
            String laneID = vehicle.getLaneID();
            TrafficLight laneLight = trafficController.getTrafficLight(laneID);
            String signalState = laneLight != null ? laneLight.getCurrentState() : "RED";

            // Only move if the light for this vehicle's lane is GREEN or YELLOW
            // (Ambulance handles its own logic in move())
            vehicle.move(signalState);

            if (vehicle.hasPassedIntersection()) {
                vehicles.remove(i);
                totalVehiclesPassed++;
            }
        }
        updateStatistics();
    }

    // ── Control panel label updates ───────────────────────────────────────────

    private void updateControlPanelLabels() {
        String currentLane = trafficController.getCurrentLane();
        TrafficLight currentLight = trafficController.getTrafficLight(currentLane);

        String state = currentLight.getCurrentState();
        int timeRemaining = currentLight.getTimeRemaining();
        currentLightLabel.setText(String.format("%s: %s (%ds)", currentLane, state, timeRemaining));

        switch (state) {
            case "GREEN":
                currentLightLabel.setForeground(Color.GREEN);
                break;
            case "YELLOW":
                currentLightLabel.setForeground(Color.YELLOW);
                break;
            case "RED":
                currentLightLabel.setForeground(Color.RED);
                break;
        }

        int elapsed = trafficController.getCycleTimeElapsed();
        int total = trafficController.getTotalCycleTime();
        cycleTimerLabel.setText(String.format("Cycle: %d/%ds", elapsed, total));

        smartModeLabel.setText("Smart Mode: " + (trafficController.isSmartModeEnabled() ? "ON" : "OFF"));

        boolean hasVehiclesInCurrentLane = vehicles.stream()
                .anyMatch(v -> v.getLaneID().equals(currentLane) && !v.hasPassedIntersection());

        if (trafficController.isSmartModeEnabled() && !hasVehiclesInCurrentLane && timeRemaining > 2) {
            realtimeLabel.setText("⚡ Real-time: Switching Soon!");
            realtimeLabel.setForeground(new Color(255, 200, 0));
        } else if (trafficController.isSmartModeEnabled()) {
            realtimeLabel.setText("⚡ Real-time: Active");
            realtimeLabel.setForeground(new Color(100, 255, 100));
        } else {
            realtimeLabel.setText("⚡ Real-time: OFF");
            realtimeLabel.setForeground(new Color(150, 150, 150));
        }

        int timeBank = trafficController.getSavedTimeBank();
        timeBankLabel.setText("💰 Time Bank: " + timeBank + "s");
        timeBankLabel.setForeground(timeBank > 15 ? new Color(255, 215, 0)
                : timeBank > 0 ? new Color(200, 200, 100)
                        : new Color(150, 150, 150));

        // Per-lane counts
        String[] lanes = TrafficController.getLaneSequence();
        String[] laneIcons = { "⬆ N", "➡ E", "⬇ S", "⬅ W" };
        for (int i = 0; i < lanes.length; i++) {
            int cnt = trafficController.getVehicleCount(lanes[i]);
            laneCountLabels[i].setText(laneIcons[i] + ": " + cnt);
            laneCountLabels[i].setForeground(cnt == 0 ? new Color(120, 120, 120)
                    : cnt < 5 ? new Color(180, 220, 255)
                            : new Color(255, 150, 100));
        }

        // Emergency stats
        emergencyLabel.setText("🚨 Overrides: " + trafficController.getEmergencyOverrideCount());
    }

    // ── Button actions ────────────────────────────────────────────────────────

    private void toggleSmartMode() {
        trafficController.toggleSmartMode();
        updateControlPanelLabels();
    }

    private void togglePause() {
        if (trafficController.isPaused()) {
            trafficController.resume();
            pauseResumeButton.setText("⏸ Pause");
            pauseResumeButton.setBackground(new Color(52, 152, 219));
        } else {
            trafficController.pause();
            pauseResumeButton.setText("▶ Resume");
            pauseResumeButton.setBackground(new Color(46, 204, 113));
        }
    }

    private void applySpeedSetting(int sliderValue) {
        // sliderValue: 1=0.5×, 2=1×, 3=2×, 4=4×
        int[] delays = { 100, BASE_TIMER_DELAY, 25, 12 };
        simulationTimer.setDelay(delays[sliderValue - 1]);
    }

    private void clearAllTraffic() {
        vehicles.clear();
        System.out.println("All traffic cleared");
    }

    // ── Vehicle spawning ──────────────────────────────────────────────────────

    private void spawnHeavyTraffic(String laneID) {
        Random random = new Random();
        int count = 5 + random.nextInt(4); // 5–8 vehicles

        int cx = INTERSECTION_SIZE / 2;
        int cy = INTERSECTION_SIZE / 2;
        int spacing = 70;
        int startDistance = 150;

        for (int i = 0; i < count; i++) {
            int vehicleType = random.nextInt(3);
            Vehicle vehicle;

            switch (laneID) {
                case "NORTH":
                    vehicle = createVehicle(vehicleType,
                            cx + Vehicle.NORTH_LANE_OFFSET,
                            cy + ROAD_HALF_WIDTH + startDistance + (i * spacing),
                            laneID);
                    break;
                case "SOUTH":
                    vehicle = createVehicle(vehicleType,
                            cx - Vehicle.SOUTH_LANE_OFFSET,
                            cy - ROAD_HALF_WIDTH - startDistance - (i * spacing),
                            laneID);
                    break;
                case "EAST":
                    vehicle = createVehicle(vehicleType,
                            cx - ROAD_HALF_WIDTH - startDistance - (i * spacing),
                            cy + Vehicle.EAST_LANE_OFFSET,
                            laneID);
                    break;
                case "WEST":
                    vehicle = createVehicle(vehicleType,
                            cx + ROAD_HALF_WIDTH + startDistance + (i * spacing),
                            cy - Vehicle.WEST_LANE_OFFSET,
                            laneID);
                    break;
                default:
                    return;
            }
            vehicles.add(vehicle);
        }
        System.out.println("Spawned " + count + " vehicles to " + laneID + " lane");
    }

    /**
     * Create a vehicle of the given type.
     * 
     * @param type 0 = PrivateCar (random colour), 1 = PublicBus, 2 = Motorcycle
     */
    private Vehicle createVehicle(int type, int x, int y, String laneID) {
        Random random = new Random();
        switch (type) {
            case 0:
                return new PrivateCar(x, y, laneID, CAR_COLORS[random.nextInt(CAR_COLORS.length)]);
            case 1:
                return new PublicBus(x, y, laneID);
            case 2:
                return new Motorcycle(x, y, laneID);
            default:
                return new PrivateCar(x, y, laneID);
        }
    }

    private void spawnAmbulance() {
        Random random = new Random();
        String[] lanes = TrafficController.getLaneSequence();
        String randomLane = lanes[random.nextInt(lanes.length)];

        int cx = INTERSECTION_SIZE / 2;
        int cy = INTERSECTION_SIZE / 2;
        int spawnDistance = 200;

        Ambulance ambulance;
        switch (randomLane) {
            case "NORTH":
                ambulance = new Ambulance(cx + Vehicle.NORTH_LANE_OFFSET,
                        cy + ROAD_HALF_WIDTH + spawnDistance, randomLane);
                break;
            case "SOUTH":
                ambulance = new Ambulance(cx - Vehicle.SOUTH_LANE_OFFSET,
                        cy - ROAD_HALF_WIDTH - spawnDistance, randomLane);
                break;
            case "EAST":
                ambulance = new Ambulance(cx - ROAD_HALF_WIDTH - spawnDistance,
                        cy + Vehicle.EAST_LANE_OFFSET, randomLane);
                break;
            case "WEST":
                ambulance = new Ambulance(cx + ROAD_HALF_WIDTH + spawnDistance,
                        cy - Vehicle.WEST_LANE_OFFSET, randomLane);
                break;
            default:
                return;
        }
        vehicles.add(ambulance);
        System.out.println("🚑 Ambulance spawned on " + randomLane + " lane!");
    }

    // ── Statistics ────────────────────────────────────────────────────────────

    private void updateStatistics() {
        statsLabel.setText(String.format("Active: %d | Passed: %d", vehicles.size(), totalVehiclesPassed));

        String density;
        Color densityColor;
        if (vehicles.size() < 10) {
            density = "Low";
            densityColor = new Color(150, 255, 150);
        } else if (vehicles.size() < 20) {
            density = "Medium";
            densityColor = new Color(255, 220, 100);
        } else {
            density = "High";
            densityColor = new Color(255, 100, 100);
        }
        densityLabel.setText("Density: " + density);
        densityLabel.setForeground(densityColor);
    }

    // ── Styled component helpers ──────────────────────────────────────────────

    private JPanel createStyledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(30, 30, 33));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 85), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 11));
        titleLabel.setForeground(new Color(150, 150, 155));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        return panel;
    }

    private JLabel createStyledLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Consolas", Font.PLAIN, 12));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        return label;
    }

    private JPanel createLegendItem(String text, String type) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 1));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iconLabel = new JLabel(createLegendVehicleIcon(type));
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        textLabel.setForeground(new Color(220, 220, 220));

        row.add(iconLabel);
        row.add(textLabel);
        return row;
    }

    private Icon createLegendVehicleIcon(final String type) {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return 24;
            }

            @Override
            public int getIconHeight() {
                return 14;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                switch (type) {
                    case "CAR": {
                        Color carColor = new Color(100, 150, 200);
                        g2.setColor(carColor);
                        g2.fillRoundRect(x + 2, y + 4, 20, 8, 4, 4);
                        g2.setColor(new Color(160, 200, 240));
                        g2.fillRoundRect(x + 5, y + 5, 8, 3, 2, 2);
                        g2.setColor(carColor.darker());
                        g2.drawRoundRect(x + 2, y + 4, 20, 8, 4, 4);
                        break;
                    }
                    case "BUS": {
                        Color busColor = new Color(255, 165, 0);
                        g2.setColor(busColor);
                        g2.fillRoundRect(x + 1, y + 3, 22, 9, 3, 3);
                        g2.setColor(new Color(180, 220, 255));
                        g2.fillRect(x + 4, y + 5, 5, 3);
                        g2.fillRect(x + 10, y + 5, 5, 3);
                        g2.fillRect(x + 16, y + 5, 4, 3);
                        g2.setColor(busColor.darker());
                        g2.drawRoundRect(x + 1, y + 3, 22, 9, 3, 3);
                        break;
                    }
                    case "MOTORCYCLE": {
                        Color motoColor = new Color(255, 220, 50);
                        g2.setColor(Color.BLACK);
                        g2.fillOval(x + 4, y + 8, 4, 4);
                        g2.fillOval(x + 16, y + 8, 4, 4);
                        g2.setColor(motoColor);
                        g2.fillRoundRect(x + 6, y + 6, 12, 4, 3, 3);
                        g2.setColor(new Color(70, 70, 70));
                        g2.fillOval(x + 10, y + 3, 4, 3);
                        g2.setColor(motoColor.darker());
                        g2.drawRoundRect(x + 6, y + 6, 12, 4, 3, 3);
                        break;
                    }
                    case "AMBULANCE": {
                        Color ambColor = new Color(230, 70, 70);
                        g2.setColor(Color.WHITE);
                        g2.fillRoundRect(x + 1, y + 3, 22, 9, 3, 3);
                        g2.setColor(ambColor);
                        g2.fillRect(x + 1, y + 6, 22, 3);
                        g2.setStroke(new BasicStroke(2f));
                        g2.drawLine(x + 9, y + 7, x + 15, y + 7);
                        g2.drawLine(x + 12, y + 4, x + 12, y + 10);
                        g2.setColor(new Color(200, 200, 200));
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawRoundRect(x + 1, y + 3, 22, 9, 3, 3);
                        break;
                    }
                    default:
                        g2.setColor(new Color(180, 180, 180));
                        g2.fillRect(x + 4, y + 4, 14, 6);
                        break;
                }

                g2.dispose();
            }
        };
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 11));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMargin(new Insets(6, 12, 6, 12));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });
        return button;
    }

    private JPanel createQuadrantContainer() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(45, 45, 48, 220));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setOpaque(true);
        return panel;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Inner class: IntersectionPanel
    // ═════════════════════════════════════════════════════════════════════════

    private class IntersectionPanel extends JPanel {

        public IntersectionPanel() {
            setLayout(null);
            setPreferredSize(new Dimension(INTERSECTION_SIZE, INTERSECTION_SIZE));
            setMinimumSize(new Dimension(400, 400));
            setBackground(new Color(40, 167, 69)); // grass green

            addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    updateQuadrantControlBounds();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // ── Viewport Centering ──────────────────────────────────────────
            // Center the fixed-size 595x595 physical simulation inside the dynamically resizing panel.
            // This prevents the top-left alignment issue while keeping vehicle physics deterministic.
            int xOffset = (getWidth() - INTERSECTION_SIZE) / 2;
            int yOffset = (getHeight() - INTERSECTION_SIZE) / 2;
            g2d.translate(xOffset, yOffset);

            drawRoads(g2d);
            drawDirectionArrows(g2d);
            drawTrafficLights(g2d);
            drawVehicles(g2d);

            // Revert translation for absolute UI overlays
            g2d.translate(-xOffset, -yOffset);

            drawPauseOverlay(g2d);
        }

        // ── Road drawing ──────────────────────────────────────────────────────

        private void drawRoads(Graphics2D g2d) {
            int cx = INTERSECTION_SIZE / 2, cy = INTERSECTION_SIZE / 2;
            int roadWidth = 140;

            // Shadows
            g2d.setColor(new Color(0, 0, 0, 40));
            g2d.fillRect(cx - roadWidth / 2 + 5, -2000, roadWidth, 4000);
            g2d.fillRect(-2000, cy - roadWidth / 2 + 5, 4000, roadWidth);

            // Vertical road
            GradientPaint vg = new GradientPaint(cx - roadWidth / 2, 0, new Color(55, 55, 55),
                    cx + roadWidth / 2, 0, new Color(75, 75, 75));
            g2d.setPaint(vg);
            g2d.fillRect(cx - roadWidth / 2, -2000, roadWidth, 4000);

            // Horizontal road
            GradientPaint hg = new GradientPaint(0, cy - roadWidth / 2, new Color(55, 55, 55),
                    0, cy + roadWidth / 2, new Color(75, 75, 75));
            g2d.setPaint(hg);
            g2d.fillRect(-2000, cy - roadWidth / 2, 4000, roadWidth);

            // Road borders
            g2d.setColor(new Color(40, 40, 40));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(cx - roadWidth / 2, -2000, roadWidth, 4000);
            g2d.drawRect(-2000, cy - roadWidth / 2, 4000, roadWidth);

            // Dashed yellow lane dividers
            g2d.setColor(new Color(255, 220, 0));
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    0, new float[] { 15, 10 }, 0));
            g2d.drawLine(cx, -2000, cx, cy - roadWidth / 2);
            g2d.drawLine(cx, cy + roadWidth / 2, cx, 4000);
            g2d.drawLine(-2000, cy, cx - roadWidth / 2, cy);
            g2d.drawLine(cx + roadWidth / 2, cy, 4000, cy);

            // White road edges
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(4));
            g2d.drawLine(cx - roadWidth / 2, -2000, cx - roadWidth / 2, cy - roadWidth / 2);
            g2d.drawLine(cx - roadWidth / 2, cy + roadWidth / 2, cx - roadWidth / 2, 4000);
            g2d.drawLine(cx + roadWidth / 2, -2000, cx + roadWidth / 2, cy - roadWidth / 2);
            g2d.drawLine(cx + roadWidth / 2, cy + roadWidth / 2, cx + roadWidth / 2, 4000);
            g2d.drawLine(-2000, cy - roadWidth / 2, cx - roadWidth / 2, cy - roadWidth / 2);
            g2d.drawLine(cx + roadWidth / 2, cy - roadWidth / 2, 4000, cy - roadWidth / 2);
            g2d.drawLine(-2000, cy + roadWidth / 2, cx - roadWidth / 2, cy + roadWidth / 2);
            g2d.drawLine(cx + roadWidth / 2, cy + roadWidth / 2, 4000, cy + roadWidth / 2);

            // Intersection centre
            g2d.setColor(new Color(65, 65, 65));
            g2d.fillRect(cx - roadWidth / 2, cy - roadWidth / 2, roadWidth, roadWidth);

            // Crosswalks
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(6));
            int csp = 12;
            for (int i = 0; i < roadWidth; i += csp) {
                // North
                g2d.drawLine(cx - roadWidth / 2 + i, cy - roadWidth / 2 - 25,
                        cx - roadWidth / 2 + i, cy - roadWidth / 2 - 10);
                // South
                g2d.drawLine(cx - roadWidth / 2 + i, cy + roadWidth / 2 + 10,
                        cx - roadWidth / 2 + i, cy + roadWidth / 2 + 25);
                // East
                g2d.drawLine(cx + roadWidth / 2 + 10, cy - roadWidth / 2 + i,
                        cx + roadWidth / 2 + 25, cy - roadWidth / 2 + i);
                // West
                g2d.drawLine(cx - roadWidth / 2 - 25, cy - roadWidth / 2 + i,
                        cx - roadWidth / 2 - 10, cy - roadWidth / 2 + i);
            }

            // ── Per-lane stop lines ──────────────────────────────────────────
            // Thick white bar drawn at the intersection edge, only on the half
            // of the road that each direction occupies.
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));

            // NORTH (moves ↑): stops at SOUTH edge of intersection, RIGHT half of road
            g2d.drawLine(cx, cy + roadWidth / 2,
                    cx + roadWidth / 2, cy + roadWidth / 2);

            // SOUTH (moves ↓): stops at NORTH edge of intersection, LEFT half of road
            g2d.drawLine(cx - roadWidth / 2, cy - roadWidth / 2,
                    cx, cy - roadWidth / 2);

            // EAST (moves →): stops at WEST edge of intersection, BOTTOM half of road
            g2d.drawLine(cx - roadWidth / 2, cy,
                    cx - roadWidth / 2, cy + roadWidth / 2);

            // WEST (moves ←): stops at EAST edge of intersection, TOP half of road
            g2d.drawLine(cx + roadWidth / 2, cy - roadWidth / 2,
                    cx + roadWidth / 2, cy);

            g2d.setStroke(new BasicStroke(1));
        }

        // ── Direction arrows ──────────────────────────────────────────────────

        private void drawDirectionArrows(Graphics2D g2d) {
            int cx = INTERSECTION_SIZE / 2, cy = INTERSECTION_SIZE / 2;
            int roadWidth = 140;

            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g2d.getFontMetrics();

            // NORTH lane arrow (↑) – right side of vertical road
            String na = "↑";
            g2d.drawString(na, cx + Vehicle.NORTH_LANE_OFFSET - fm.stringWidth(na) / 2, cy - roadWidth / 2 - 40);
            g2d.drawString(na, cx + Vehicle.NORTH_LANE_OFFSET - fm.stringWidth(na) / 2, cy + roadWidth / 2 + 55);

            // SOUTH lane arrow (↓) – left side of vertical road
            String sa = "↓";
            g2d.drawString(sa, cx - Vehicle.SOUTH_LANE_OFFSET - fm.stringWidth(sa) / 2, cy - roadWidth / 2 - 40);
            g2d.drawString(sa, cx - Vehicle.SOUTH_LANE_OFFSET - fm.stringWidth(sa) / 2, cy + roadWidth / 2 + 55);

            // EAST lane arrow (→) – bottom half of horizontal road
            String ea = "→";
            g2d.drawString(ea, cx - roadWidth / 2 - 50, cy + Vehicle.EAST_LANE_OFFSET + fm.getAscent() / 2);
            g2d.drawString(ea, cx + roadWidth / 2 + 30, cy + Vehicle.EAST_LANE_OFFSET + fm.getAscent() / 2);

            // WEST lane arrow (←) – top half of horizontal road
            String wa = "←";
            g2d.drawString(wa, cx - roadWidth / 2 - 50, cy - Vehicle.WEST_LANE_OFFSET + fm.getAscent() / 2);
            g2d.drawString(wa, cx + roadWidth / 2 + 30, cy - Vehicle.WEST_LANE_OFFSET + fm.getAscent() / 2);
        }

        // ── Traffic lights ────────────────────────────────────────────────────

        private void drawTrafficLights(Graphics2D g2d) {
            int cx = INTERSECTION_SIZE / 2, cy = INTERSECTION_SIZE / 2;
            int offset = 60;
            // Swap NORTH and SOUTH light positions
            drawSingleTrafficLight(g2d, cx - offset, cy - 130, "SOUTH"); // Top now controls SOUTH
            drawSingleTrafficLight(g2d, cx + offset, cy + 130, "NORTH"); // Bottom now controls NORTH
            // Swap EAST and WEST light positions
            drawSingleTrafficLight(g2d, cx + 130, cy - offset, "WEST"); // Right now controls WEST
            drawSingleTrafficLight(g2d, cx - 130, cy + offset, "EAST"); // Left now controls EAST
        }

        private void drawSingleTrafficLight(Graphics2D g2d, int x, int y, String laneID) {
            TrafficLight light = trafficController.getTrafficLight(laneID);
            String state = light.getCurrentState();
            int timeRemaining = light.getTimeRemaining();

            // Pole shadow + pole
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillRect(x - 1, y + 2, 8, 35);
            GradientPaint pg = new GradientPaint(x - 4, 0, new Color(60, 60, 60),
                    x + 4, 0, new Color(90, 90, 90));
            g2d.setPaint(pg);
            g2d.fillRoundRect(x - 4, y, 8, 35, 4, 4);

            // Housing shadow + housing
            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.fillRoundRect(x - 18, y - 54, 36, 50, 12, 12);
            GradientPaint hg = new GradientPaint(x - 20, y - 55, new Color(30, 30, 30),
                    x + 20, y - 55, new Color(50, 50, 50));
            g2d.setPaint(hg);
            g2d.fillRoundRect(x - 20, y - 56, 36, 50, 12, 12);

            int redY = y - 48;
            int yellowY = y - 32;
            int greenY = y - 16;

            // Red
            if ("RED".equals(state)) {
                g2d.setColor(new Color(255, 0, 0, 60));
                g2d.fillOval(x - 12, redY - 2, 20, 20);
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(new Color(80, 0, 0));
            }
            g2d.fillOval(x - 10, redY, 16, 16);

            // Yellow
            if ("YELLOW".equals(state)) {
                g2d.setColor(new Color(255, 255, 0, 80));
                g2d.fillOval(x - 12, yellowY - 2, 20, 20);
                g2d.setColor(Color.YELLOW);
            } else {
                g2d.setColor(new Color(80, 80, 0));
            }
            g2d.fillOval(x - 10, yellowY, 16, 16);

            // Green
            if ("GREEN".equals(state)) {
                g2d.setColor(new Color(0, 255, 0, 60));
                g2d.fillOval(x - 12, greenY - 2, 20, 20);
                g2d.setColor(Color.GREEN);
            } else {
                g2d.setColor(new Color(0, 80, 0));
            }
            g2d.fillOval(x - 10, greenY, 16, 16);

            // Highlight
            g2d.setColor(new Color(255, 255, 255, 100));
            if ("RED".equals(state))
                g2d.fillOval(x - 7, redY + 3, 5, 5);
            if ("YELLOW".equals(state))
                g2d.fillOval(x - 7, yellowY + 3, 5, 5);
            if ("GREEN".equals(state))
                g2d.fillOval(x - 7, greenY + 3, 5, 5);

            // Lane label
            g2d.setColor(new Color(0, 0, 0, 120));
            g2d.fillRoundRect(x - 18, y + 38, 32, 20, 8, 8);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 11));
            FontMetrics fm = g2d.getFontMetrics();
            // Swap the label for N<->S and E<->W
            String label;
            switch (laneID) {
                case "NORTH": label = "S"; break;
                case "SOUTH": label = "N"; break;
                case "EAST":  label = "W"; break;
                case "WEST":  label = "E"; break;
                default: label = laneID.substring(0, 1);
            }
            g2d.drawString(label, x - fm.stringWidth(label) / 2, y + 52);

            // Countdown timer
            if ("GREEN".equals(state) || "YELLOW".equals(state)) {
                Font timerFont = new Font("Arial", Font.BOLD, 10);
                g2d.setFont(timerFont);
                FontMetrics timerFm = g2d.getFontMetrics(timerFont);
                String timer = String.valueOf(timeRemaining);
                int tx = x - timerFm.stringWidth(timer) / 2;
                int ty = y + 68;

                g2d.setColor(new Color(0, 0, 0, 140));
                g2d.fillRoundRect(tx - 4, ty - timerFm.getAscent(),
                        timerFm.stringWidth(timer) + 8, timerFm.getHeight(), 8, 8);

                g2d.setColor(new Color(255, 255, 255, 220));
                g2d.drawString(timer, tx, ty);
            }
        }

        // ── Vehicle drawing ───────────────────────────────────────────────────

        private void drawVehicles(Graphics2D g2d) {
            for (Vehicle vehicle : vehicles) {
                drawVehicle(g2d, vehicle);
            }
        }

        private void drawVehicle(Graphics2D g2d, Vehicle vehicle) {
            int x = vehicle.getX(), y = vehicle.getY();
            Color color = vehicle.getColor();
            String lane = vehicle.getLaneID();

            java.awt.geom.AffineTransform oldTransform = g2d.getTransform();

            double angle = 0;
            switch (lane) {
                case "EAST":
                    angle = 0;
                    break;
                case "SOUTH":
                    angle = Math.PI / 2;
                    break;
                case "WEST":
                    angle = Math.PI;
                    break;
                case "NORTH":
                    angle = -Math.PI / 2;
                    break;
            }
            g2d.rotate(angle, x, y);

            if (vehicle instanceof Ambulance)
                drawAmbulance(g2d, x, y, color);
            else if (vehicle instanceof PublicBus)
                drawBus(g2d, x, y, color);
            else if (vehicle instanceof Motorcycle)
                drawMotorcycle(g2d, x, y, color);
            else
                drawCar(g2d, x, y, color);

            g2d.setTransform(oldTransform);
        }

        private void drawCar(Graphics2D g2d, int x, int y, Color color) {
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillRoundRect(x - 10, y - 6, 24, 16, 6, 6);

            GradientPaint gp = new GradientPaint(x, y - 8, color.brighter(), x, y + 8, color.darker());
            g2d.setPaint(gp);
            g2d.fillRoundRect(x - 12, y - 8, 24, 16, 6, 6);

            g2d.setColor(new Color(100, 150, 200, 180));
            g2d.fillRoundRect(x - 8, y - 6, 8, 5, 3, 3);
            g2d.fillRoundRect(x + 2, y - 6, 8, 5, 3, 3);

            g2d.setColor(new Color(255, 255, 255, 80));
            g2d.fillRoundRect(x - 8, y - 6, 6, 3, 2, 2);

            g2d.setColor(color.darker().darker());
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(x - 12, y - 8, 24, 16, 6, 6);

            g2d.setColor(new Color(255, 255, 200, 200));
            g2d.fillOval(x + 10, y - 6, 3, 3);
            g2d.fillOval(x + 10, y + 3, 3, 3);
            g2d.setStroke(new BasicStroke(1));
        }

        private void drawBus(Graphics2D g2d, int x, int y, Color color) {
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillRoundRect(x - 12, y - 10, 30, 24, 6, 6);

            GradientPaint gp = new GradientPaint(x, y - 12, color.brighter(), x, y + 12, color.darker());
            g2d.setPaint(gp);
            g2d.fillRoundRect(x - 14, y - 12, 30, 24, 6, 6);

            g2d.setColor(new Color(100, 150, 200, 180));
            for (int i = 0; i < 3; i++)
                g2d.fillRect(x - 10 + i * 8, y - 9, 6, 7);

            g2d.setColor(color.darker().darker());
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawRoundRect(x - 14, y - 12, 30, 24, 6, 6);

            g2d.setColor(Color.BLACK);
            g2d.fillOval(x - 8, y + 10, 6, 6);
            g2d.fillOval(x + 8, y + 10, 6, 6);
            g2d.setStroke(new BasicStroke(1));
        }

        private void drawMotorcycle(Graphics2D g2d, int x, int y, Color color) {
            // Shadow
            g2d.setColor(new Color(0, 0, 0, 40));
            g2d.fillOval(x - 8, y - 3, 18, 8);

            // Body (slim)
            GradientPaint gp = new GradientPaint(x, y - 5, color.brighter(), x, y + 5, color.darker());
            g2d.setPaint(gp);
            g2d.fillRoundRect(x - 8, y - 5, 18, 10, 5, 5);

            // Rider helmet
            g2d.setColor(new Color(50, 50, 50));
            g2d.fillOval(x - 2, y - 8, 8, 7);
            g2d.setColor(new Color(150, 200, 255, 180));
            g2d.fillOval(x - 1, y - 7, 5, 4); // visor

            // Wheels
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x + 7, y + 3, 5, 5);
            g2d.fillOval(x - 10, y + 3, 5, 5);

            // Outline
            g2d.setColor(color.darker().darker());
            g2d.setStroke(new BasicStroke(1f));
            g2d.drawRoundRect(x - 8, y - 5, 18, 10, 5, 5);
            g2d.setStroke(new BasicStroke(1));
        }

        private void drawAmbulance(Graphics2D g2d, int x, int y, Color color) {
            // Flashing glow
            if (System.currentTimeMillis() % 500 < 250) {
                g2d.setColor(new Color(255, 0, 0, 100));
                g2d.fillOval(x - 20, y - 20, 40, 40);
            }

            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillRoundRect(x - 11, y - 9, 28, 22, 6, 6);

            GradientPaint gp = new GradientPaint(x, y - 10, Color.WHITE, x, y + 10, new Color(240, 240, 240));
            g2d.setPaint(gp);
            g2d.fillRoundRect(x - 13, y - 10, 28, 22, 6, 6);

            g2d.setColor(color);
            g2d.fillRect(x - 13, y - 2, 28, 6);

            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(x - 5, y + 1, x + 5, y + 1);
            g2d.drawLine(x, y - 4, x, y + 6);

            g2d.setColor(new Color(100, 150, 200, 180));
            g2d.fillRoundRect(x - 9, y - 8, 8, 6, 2, 2);
            g2d.fillRoundRect(x + 3, y - 8, 8, 6, 2, 2);

            g2d.setColor(color.darker());
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawRoundRect(x - 13, y - 10, 28, 22, 6, 6);

            // Emergency lights (alternating red/blue)
            g2d.setColor(System.currentTimeMillis() % 500 < 250 ? Color.RED : Color.BLUE);
            g2d.fillOval(x - 3, y - 13, 6, 6);
            g2d.setStroke(new BasicStroke(1));
        }

        // ── Pause overlay ─────────────────────────────────────────────────────

        private void drawPauseOverlay(Graphics2D g2d) {
            if (!trafficController.isPaused())
                return;
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(new Color(255, 255, 255, 220));
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            String msg = "⏸ PAUSED";
            g2d.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
        }
    }

}
