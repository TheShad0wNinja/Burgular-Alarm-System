package bas.console;

import bas.power.BackupBattery;
import bas.rooms.Room;
import bas.rooms.RoomRepository;
import bas.sensors.Sensor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BASMonitorUI
 *
 * A modern dark-themed GUI for the Burglar Alarm System.
 * Implements Displayable — pass it directly to ConsoleScreen instead of ConsoleDisplayable.
 *
 * Shows in real-time:
 *   - All event log messages (colour-coded by type)
 *   - Room cards: which rooms are intruded, which lights are on, sensor states
 *   - System status: main power vs battery, battery charge bar, alarm count
 *   - Panic + Clear Alarms buttons with PIN authentication
 *
 * How to wire it up in Main.java — see bottom of this file.
 */
public class BASMonitorUI implements Displayable {

    // =========================================================================
    // Colour palette  (dark "security panel" theme)
    // =========================================================================
    private static final Color C_BG          = new Color(13,  17,  23);   // page background
    private static final Color C_PANEL       = new Color(22,  27,  34);   // panel background
    private static final Color C_CARD        = new Color(30,  36,  44);   // room card bg
    private static final Color C_BORDER      = new Color(48,  54,  61);   // subtle borders
    private static final Color C_TEXT        = new Color(230, 237, 243);  // primary text
    private static final Color C_DIM         = new Color(110, 118, 129);  // dimmed text
    private static final Color C_RED         = new Color(248,  81,  73);  // alarm / intrusion
    private static final Color C_GREEN       = new Color(63,  185, 105);  // clear / ok
    private static final Color C_ORANGE      = new Color(210, 153,  34);  // power / battery
    private static final Color C_BLUE        = new Color(88,  166, 255);  // info
    private static final Color C_PURPLE      = new Color(188, 140, 255);  // auth / pin
    private static final Color C_YELLOW      = new Color(230, 220,  60);  // service needed
    private static final Color C_BTN_PANIC   = new Color(180,  30,  30);  // panic button
    private static final Color C_BTN_CLEAR   = new Color(30,  120,  60);  // clear button

    private static final Font FONT_TITLE  = new Font("SansSerif", Font.BOLD,  15);
    private static final Font FONT_LABEL  = new Font("SansSerif", Font.BOLD,  12);
    private static final Font FONT_SMALL  = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font FONT_MONO   = new Font("Monospaced", Font.PLAIN, 12);
    private static final Font FONT_BTN    = new Font("SansSerif", Font.BOLD,  13);

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // =========================================================================
    // Data references  (read-only — UI never writes to these)
    // =========================================================================
    private final RoomRepository  roomRepo;
    private final BackupBattery   battery;
    private final PanicButton     panicButton;
    private final ClearAlarmButton clearButton;

    // =========================================================================
    // Swing components that are updated by refresh() or display()
    // =========================================================================
    private JFrame      frame;
    private JTextPane   logPane;
    private StyledDocument logDoc;

    // Header status badge
    private JLabel headerStatus;

    // System status panel labels
    private JLabel lblPowerSource;
    private JProgressBar batteryBar;
    private JLabel lblBatteryPct;
    private JLabel lblRoomCount;
    private JLabel lblSensorCount;
    private JLabel lblAlarmCount;

    // Room cards — one panel per room, rebuilt on refresh
    private JPanel roomsContainer;

    // PIN input field in footer
    private JPasswordField pinField;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * @param roomRepo    all rooms in the building
     * @param battery     backup battery (for charge level + power source)
     * @param panicButton the panic button object (already has its room + onPress wired)
     * @param clearButton the clear-alarms button (already has its onPress wired)
     */
    public BASMonitorUI(RoomRepository roomRepo,
                        BackupBattery battery,
                        PanicButton panicButton,
                        ClearAlarmButton clearButton) {
        this.roomRepo    = roomRepo;
        this.battery     = battery;
        this.panicButton = panicButton;
        this.clearButton = clearButton;

        // All Swing work must be on the Event Dispatch Thread
        try {
            SwingUtilities.invokeAndWait(this::buildWindow);
        } catch (Exception e) {
            throw new RuntimeException("[BASMonitorUI] Failed to create window", e);
        }

        // Refresh room cards + system status every 500 ms (same as sensor poll rate)
        Timer refreshTimer = new Timer(500, e -> SwingUtilities.invokeLater(this::refresh));
        refreshTimer.start();
    }

    // =========================================================================
    // Displayable — called by ConsoleScreen from any thread
    // =========================================================================

    @Override
    public void display(String message) {
        // invokeLater makes this safe to call from any thread
        SwingUtilities.invokeLater(() -> appendToLog(message));
    }

    // =========================================================================
    // Window construction
    // =========================================================================

    private void buildWindow() {
        frame = new JFrame("Burglar Alarm System — Monitor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 680);
        frame.setMinimumSize(new Dimension(900, 550));
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(C_BG);
        frame.setLayout(new BorderLayout(0, 0));

        frame.add(buildHeader(),   BorderLayout.NORTH);
        frame.add(buildCenter(),   BorderLayout.CENTER);
        frame.add(buildFooter(),   BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    // -------------------------------------------------------------------------
    // Header
    // -------------------------------------------------------------------------

    private JPanel buildHeader() {
        JPanel header = darkPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(12, 18, 12, 18));
        header.setBackground(new Color(18, 22, 30));

        JLabel title = new JLabel("  BURGLAR ALARM SYSTEM");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(C_TEXT);

        headerStatus = new JLabel("● SECURE");
        headerStatus.setFont(new Font("SansSerif", Font.BOLD, 14));
        headerStatus.setForeground(C_GREEN);
        headerStatus.setBorder(new EmptyBorder(4, 12, 4, 12));

        header.add(title,        BorderLayout.WEST);
        header.add(headerStatus, BorderLayout.EAST);
        return header;
    }

    // -------------------------------------------------------------------------
    // Centre — three columns
    // -------------------------------------------------------------------------

    private JPanel buildCenter() {
        JPanel center = darkPanel(new GridLayout(1, 3, 6, 0));
        center.setBorder(new EmptyBorder(6, 6, 6, 6));
        center.setBackground(C_BG);

        center.add(buildRoomsColumn());
        center.add(buildLogColumn());
        center.add(buildSystemColumn());
        return center;
    }

    // --- Left: Rooms ---

    private JPanel buildRoomsColumn() {
        JPanel col = darkPanel(new BorderLayout(0, 6));
        col.add(sectionLabel("ROOMS"), BorderLayout.NORTH);

        roomsContainer = darkPanel(null);
        roomsContainer.setLayout(new BoxLayout(roomsContainer, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(roomsContainer);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        scroll.getViewport().setBackground(C_BG);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        col.add(scroll, BorderLayout.CENTER);
        return col;
    }

    // --- Centre: Event log ---

    private JPanel buildLogColumn() {
        JPanel col = darkPanel(new BorderLayout(0, 6));
        col.add(sectionLabel("EVENT LOG"), BorderLayout.NORTH);

        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setBackground(new Color(10, 14, 20));
        logPane.setFont(FONT_MONO);
        logDoc = logPane.getStyledDocument();

        JScrollPane scroll = new JScrollPane(logPane);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        col.add(scroll, BorderLayout.CENTER);
        return col;
    }

    // --- Right: System status ---

    private JPanel buildSystemColumn() {
        JPanel col = darkPanel(new BorderLayout(0, 6));
        col.add(sectionLabel("SYSTEM STATUS"), BorderLayout.NORTH);

        JPanel content = darkPanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createLineBorder(C_BORDER));
        content.add(Box.createVerticalStrut(14));

        // Power source
        content.add(centredRow(dimLabel("POWER SOURCE")));
        lblPowerSource = new JLabel("⚡  MAIN POWER");
        lblPowerSource.setFont(FONT_LABEL);
        lblPowerSource.setForeground(C_GREEN);
        lblPowerSource.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(lblPowerSource);
        content.add(Box.createVerticalStrut(18));

        // Battery
        content.add(centredRow(dimLabel("BATTERY BACKUP")));
        batteryBar = new JProgressBar(0, 100);
        batteryBar.setValue(100);
        batteryBar.setStringPainted(false);
        batteryBar.setForeground(C_GREEN);
        batteryBar.setBackground(C_BORDER);
        batteryBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        batteryBar.setBorder(new EmptyBorder(0, 16, 0, 16));

        lblBatteryPct = new JLabel("100%");
        lblBatteryPct.setFont(FONT_SMALL);
        lblBatteryPct.setForeground(C_DIM);
        lblBatteryPct.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(batteryBar);
        content.add(Box.createVerticalStrut(4));
        content.add(lblBatteryPct);
        content.add(Box.createVerticalStrut(22));

        // Counters
        JSeparator sep = new JSeparator();
        sep.setForeground(C_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        content.add(sep);
        content.add(Box.createVerticalStrut(14));

        lblRoomCount   = statRow(content, "Rooms");
        lblSensorCount = statRow(content, "Sensors");
        lblAlarmCount  = statRow(content, "Active Alarms");

        content.add(Box.createVerticalGlue());
        col.add(content, BorderLayout.CENTER);
        return col;
    }

    // -------------------------------------------------------------------------
    // Footer — PIN + buttons
    // -------------------------------------------------------------------------

    private JPanel buildFooter() {
        JPanel footer = darkPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
        footer.setBackground(new Color(18, 22, 30));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

        // PIN label
        JLabel pinLabel = new JLabel("OPERATOR PIN:");
        pinLabel.setFont(FONT_LABEL);
        pinLabel.setForeground(C_DIM);

        // PIN field
        pinField = new JPasswordField(6);
        pinField.setFont(FONT_MONO);
        pinField.setBackground(C_CARD);
        pinField.setForeground(C_TEXT);
        pinField.setCaretColor(C_TEXT);
        pinField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                new EmptyBorder(4, 8, 4, 8)));
        pinField.setPreferredSize(new Dimension(90, 32));

        // Panic button
        JButton btnPanic = makeButton("  PANIC  ", C_BTN_PANIC);
        btnPanic.addActionListener(e -> {
            String pin = new String(pinField.getPassword());
            panicButton.press(pin);
            pinField.setText("");
        });

        // Clear alarms button
        JButton btnClear = makeButton("  CLEAR ALARMS  ", C_BTN_CLEAR);
        btnClear.addActionListener(e -> {
            String pin = new String(pinField.getPassword());
            clearButton.press(pin);
            pinField.setText("");
        });

        footer.add(pinLabel);
        footer.add(pinField);
        footer.add(Box.createHorizontalStrut(10));
        footer.add(btnPanic);
        footer.add(btnClear);
        return footer;
    }

    // =========================================================================
    // Refresh  (called every 500 ms by the Swing Timer)
    // =========================================================================

    private void refresh() {
        refreshRooms();
        refreshSystemStatus();
        refreshHeaderStatus();
    }

    // --- Rebuild room cards ---

    private void refreshRooms() {
        List<Room> rooms = roomRepo.getRooms();

        roomsContainer.removeAll();
        roomsContainer.add(Box.createVerticalStrut(6));

        for (Room room : rooms) {
            roomsContainer.add(buildRoomCard(room));
            roomsContainer.add(Box.createVerticalStrut(6));
        }

        roomsContainer.revalidate();
        roomsContainer.repaint();
    }

    private JPanel buildRoomCard(Room room) {
        boolean intruded = room.isIntruded();
        boolean lightOn  = room.getLight().isOn();

        // Card background changes red when intruded
        Color cardBg     = intruded ? new Color(60, 20, 20) : C_CARD;
        Color statusColor = intruded ? C_RED : C_GREEN;
        String statusText = intruded ? "● INTRUDED" : "● CLEAR";

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(cardBg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(intruded ? C_RED : C_BORDER),
                new EmptyBorder(8, 10, 8, 10)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Room name + status
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(cardBg);

        JLabel nameLabel = new JLabel(room.getName());
        nameLabel.setFont(FONT_LABEL);
        nameLabel.setForeground(C_TEXT);

        JLabel statusLabel = new JLabel(statusText);
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(statusColor);

        topRow.add(nameLabel,   BorderLayout.WEST);
        topRow.add(statusLabel, BorderLayout.EAST);
        card.add(topRow);

        // Light status
        card.add(Box.createVerticalStrut(4));
        JLabel lightLabel = new JLabel(lightOn ? "  💡  Light ON" : "     Light OFF");
        lightLabel.setFont(FONT_SMALL);
        lightLabel.setForeground(lightOn ? C_YELLOW : C_DIM);
        card.add(lightLabel);

        // Sensor rows
        List<Sensor> sensors = room.getSensors();
        if (!sensors.isEmpty()) {
            card.add(Box.createVerticalStrut(6));
            for (Sensor s : sensors) {
                card.add(buildSensorRow(s, cardBg));
            }
        }

        return card;
    }

    private JPanel buildSensorRow(Sensor sensor, Color bg) {
        boolean triggered = sensor.getIsTriggred();   // matches their spelling
        Color dotColor = triggered ? C_RED : C_GREEN;
        String type    = sensor.getClass().getSimpleName()
                               .replace("Sensor", ""); // "Door", "Window", "Movement"

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setBackground(bg);

        JLabel dot = new JLabel("●");
        dot.setFont(FONT_SMALL);
        dot.setForeground(dotColor);

        JLabel name = new JLabel(type);
        name.setFont(FONT_SMALL);
        name.setForeground(triggered ? C_RED : C_DIM);

        row.add(dot);
        row.add(name);
        return row;
    }

    // --- System status panel ---

    private void refreshSystemStatus() {
        // Power source
        boolean onBattery = battery.isPoweringSystem();
        if (onBattery) {
            lblPowerSource.setText("🔋  BATTERY BACKUP");
            lblPowerSource.setForeground(C_ORANGE);
        } else {
            lblPowerSource.setText("⚡  MAIN POWER");
            lblPowerSource.setForeground(C_GREEN);
        }

        // Battery bar
        int charge = Math.max(0, Math.min(100, (int) battery.getCurrentCharge()));
        batteryBar.setValue(charge);
        lblBatteryPct.setText(charge + "%");
        if (charge > 50) {
            batteryBar.setForeground(C_GREEN);
        } else if (charge > 20) {
            batteryBar.setForeground(C_ORANGE);
        } else {
            batteryBar.setForeground(C_RED);
        }

        // Counters
        List<Room> allRooms     = roomRepo.getRooms();
        List<Room> intrudedRooms = roomRepo.getIntrudedRooms();

        int totalSensors   = allRooms.stream().mapToInt(r -> r.getSensors().size()).sum();
        int triggeredCount = (int) allRooms.stream()
                .flatMap(r -> r.getSensors().stream())
                .filter(s -> s.getIsTriggred())
                .count();

        lblRoomCount.setText(String.valueOf(allRooms.size()));
        lblSensorCount.setText(triggeredCount + " triggered / " + totalSensors + " total");
        lblAlarmCount.setText(String.valueOf(intrudedRooms.size()));

        // Colour alarm count red if non-zero
        lblAlarmCount.setForeground(intrudedRooms.isEmpty() ? C_GREEN : C_RED);
    }

    // --- Header badge ---

    private void refreshHeaderStatus() {
        boolean anyAlarm = !roomRepo.getIntrudedRooms().isEmpty();
        boolean onBattery = battery.isPoweringSystem();

        if (anyAlarm) {
            headerStatus.setText("⚠  ALARM ACTIVE");
            headerStatus.setForeground(C_RED);
        } else if (onBattery) {
            headerStatus.setText("⚡  BATTERY MODE");
            headerStatus.setForeground(C_ORANGE);
        } else {
            headerStatus.setText("●  SECURE");
            headerStatus.setForeground(C_GREEN);
        }
    }

    // =========================================================================
    // Event log — appends a colour-coded line
    // =========================================================================

    private void appendToLog(String message) {
        String timestamp = "[" + LocalTime.now().format(TIME_FMT) + "] ";
        Color  colour    = pickLogColour(message);

        try {
            Style style = logPane.addStyle(null, null);
            StyleConstants.setForeground(style, C_DIM);
            logDoc.insertString(logDoc.getLength(), timestamp, style);

            StyleConstants.setForeground(style, colour);
            logDoc.insertString(logDoc.getLength(), message + "\n", style);
        } catch (BadLocationException e) {
            // Should never happen
        }

        // Always scroll to the bottom so the latest event is visible
        logPane.setCaretPosition(logDoc.getLength());
    }

    private Color pickLogColour(String msg) {
        String m = msg.toUpperCase();
        if (m.contains("INTRUSION") || m.contains("ALARM") || m.contains("PANIC"))
            return C_RED;
        if (m.contains("POWER") || m.contains("BATTERY") || m.contains("VOLTAGE"))
            return C_ORANGE;
        if (m.contains("SERVICE") || m.contains("TECHNICIAN"))
            return C_YELLOW;
        if (m.contains("CLEARED") || m.contains("RESET") || m.contains("SECURE"))
            return C_GREEN;
        if (m.contains("AUTH") || m.contains("PIN"))
            return C_PURPLE;
        if (m.contains("CALL") || m.contains("POLICE"))
            return C_BLUE;
        return C_TEXT;
    }

    // =========================================================================
    // Small helpers
    // =========================================================================

    /** Create a panel pre-set with the dark background colour. */
    private JPanel darkPanel(LayoutManager layout) {
        JPanel p = layout != null ? new JPanel(layout) : new JPanel();
        p.setBackground(C_BG);
        return p;
    }

    /** Section header label ("ROOMS", "EVENT LOG", etc.) */
    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(C_DIM);
        l.setBorder(new EmptyBorder(0, 0, 4, 0));
        return l;
    }

    /** Small dimmed label (used as sub-headings inside the system panel). */
    private JLabel dimLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(C_DIM);
        return l;
    }

    /** Row with content horizontally centred. */
    private JPanel centredRow(Component c) {
        JPanel p = darkPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
        p.setBackground(C_PANEL);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        p.add(c);
        return p;
    }

    /**
     * Add a labelled stat row to the system panel.
     * Returns the value JLabel so it can be updated by refresh().
     */
    private JLabel statRow(JPanel parent, String label) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(C_PANEL);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.setBorder(new EmptyBorder(2, 16, 2, 16));

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(C_DIM);

        JLabel val = new JLabel("—");
        val.setFont(FONT_LABEL);
        val.setForeground(C_TEXT);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        parent.add(row);
        parent.add(Box.createVerticalStrut(4));
        return val;
    }

    /** Styled button with custom background colour. */
    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BTN);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.brighter()),
                new EmptyBorder(6, 16, 6, 16)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Slightly lighter on hover
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(bg.brighter());
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }
}

/*
 * =============================================================================
 * HOW TO USE THIS IN Main.java
 * =============================================================================
 *
 * Replace wherever you currently create ConsoleScreen with this:
 *
 *   // 1. Create the UI (it opens the window immediately)
 *   BASMonitorUI ui = new BASMonitorUI(roomRepo, battery, panicBtn, clearBtn);
 *
 *   // 2. Pass it to ConsoleScreen — nothing else changes
 *   ConsoleScreen screen = new ConsoleScreen(ui);
 *
 * Make sure panicBtn and clearBtn already have their onPress actions set
 * by BASController BEFORE you create BASMonitorUI:
 *
 *   panicBtn.setOnPress(() -> basController.panic(panicBtn.getRoom()));
 *   clearBtn.setOnPress(() -> basController.clearAlarms());
 *
 * That's it. The UI refreshes itself every 500ms automatically.
 * =============================================================================
 */