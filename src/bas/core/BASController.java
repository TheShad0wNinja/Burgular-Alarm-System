package bas.core;

import bas.console.BASMonitorUI;
import bas.console.ClearAlarmButton;
import bas.console.ConsoleScreen;
import bas.console.PanicButton;
import bas.epl.EsperEngine;
import bas.phone.PhoneController;
import bas.power.BackupBattery;
import bas.power.PowerController;
import bas.power.PowerFailureEvent;
import bas.power.VoltageChangeEvent;
import bas.rooms.Buzzer;
import bas.rooms.Room;
import bas.rooms.RoomRepository;
import bas.sensors.DoorSensor;
import bas.sensors.MovementSensor;
import bas.sensors.Sensor;
import bas.sensors.SensorController;
import bas.sensors.SensorRepositroy;
import bas.sensors.WindowSensor;

import javax.swing.SwingUtilities;
import java.util.List;

public class BASController {

    // ── Repositories ─────────────────────────────────────────────────────────
    private RoomRepository   roomRepository;
    private SensorRepositroy sensorRepository;

    // ── Controllers ───────────────────────────────────────────────────────────
    private PhoneController  phoneController;
    private PowerController  powerController;
    private SensorController sensorController;

    // ── Esper engines ─────────────────────────────────────────────────────────
    private EsperEngine sensorEngine;
    private EsperEngine powerEngine;

    // ── Hardware / actuators ──────────────────────────────────────────────────
    private Buzzer        buzzer;
    private BackupBattery battery;

    // ── Console inputs (Ahmed's classes) ──────────────────────────────────────
    private PanicButton      panicButton;
    private ClearAlarmButton clearAlarmButton;

    // ── Display (Ahmed's classes) ─────────────────────────────────────────────
    private ConsoleScreen screen;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean systemIsOn     = false;
    private boolean alarmTriggered = false;

    // =========================================================================
    // Getters used by DebugUI / BASMonitorUI
    // =========================================================================

    public boolean        isAlarmTriggered()  { return alarmTriggered; }
    public boolean        isSystemOn()        { return systemIsOn; }
    public RoomRepository getRoomRepository() { return roomRepository; }
    public BackupBattery  getBattery()        { return battery; }

    // =========================================================================
    // Constructor — boot sequence
    // =========================================================================

    public BASController() {
        systemIsOn = true;

        registerSensors();   // creates rooms, sensors, repositories, buzzer, battery
        registerConsole();   // creates panic + clear buttons
        registerEvents();    // creates Esper engines with callbacks
        registerControllers(); // creates phone + sensor controller

        // Start 500ms sensor polling loop
        sensorController.beginPollCycle();

        // Open the UI on the Swing thread
        SwingUtilities.invokeLater(this::launchUI);
    }

    // =========================================================================
    // Setup helpers
    // =========================================================================

    private void registerSensors() {
        this.buzzer          = new Buzzer();
        this.battery         = new BackupBattery();
        this.sensorRepository = new SensorRepositroy();
        this.roomRepository   = new RoomRepository();

        // ── Rooms + their sensors ─────────────────────────────────────────
        Room livingRoom = new Room("Living Room");
        livingRoom.addSensor(new DoorSensor("FrontDoor"));
        livingRoom.addSensor(new WindowSensor("LivingRoomWindow1"));
        livingRoom.addSensor(new MovementSensor("LivingRoomMotion"));

        Room kitchen = new Room("Kitchen");
        kitchen.addSensor(new WindowSensor("KitchenWindow1"));
        kitchen.addSensor(new DoorSensor("BackDoor"));

        Room bedroom = new Room("Master Bedroom");
        bedroom.addSensor(new WindowSensor("BedroomWindow1"));
        bedroom.addSensor(new WindowSensor("BedroomWindow2"));
        bedroom.addSensor(new MovementSensor("BedroomMotion"));

        Room hallway = new Room("Hallway");
        hallway.addSensor(new MovementSensor("HallwayMotion"));

        roomRepository.addRoom(livingRoom);
        roomRepository.addRoom(kitchen);
        roomRepository.addRoom(bedroom);
        roomRepository.addRoom(hallway);

        // Register every sensor in every room into the flat sensor repository
        for (Room r : roomRepository.getRooms()) {
            for (Sensor s : r.getSensors()) {
                sensorRepository.addSensor(s);
            }
        }
    }

    /**
     * Create PanicButton and ClearAlarmButton, then wire their onPress actions.
     * Called after registerSensors() so roomRepository already exists.
     * Panic button is placed in Living Room (first room = console location).
     */
    private void registerConsole() {
        Room consoleRoom = roomRepository.getRooms().get(0); // Living Room

        panicButton      = new PanicButton(consoleRoom);
        clearAlarmButton = new ClearAlarmButton();

        // Wire the actions — these run after PIN is accepted
        panicButton.setOnPress(()      -> panicRoom(panicButton.getRoom()));
        clearAlarmButton.setOnPress(() -> clearAlarms());
    }

    private void registerControllers() {
        phoneController  = new PhoneController();
        sensorController = new SensorController(this.sensorRepository, this.sensorEngine);
    }

    private void registerEvents() {
        this.sensorEngine = new EsperEngine(
                this::handleSensorTriggered,
                this::handleSensorFailure
        );
        this.powerEngine = new EsperEngine(
                this::handlePowerMinorDrop,
                this::handlePowerMajorDrop,
                this::handlePowerVoltageRecovered,
                this::handlePowerFailure
        );
    }

    /** Open the monitor window. Called on the Swing EDT. */
    private void launchUI() {
        // BASMonitorUI implements Displayable — pass it straight to ConsoleScreen
        BASMonitorUI ui = new BASMonitorUI(
                roomRepository,
                battery,
                panicButton,
                clearAlarmButton
        );
        screen = new ConsoleScreen(ui);
        System.out.println("[BASController] UI launched.");
    }

    // =========================================================================
    // Public alarm actions  (called by buttons + Esper callbacks)
    // =========================================================================

    /**
     * Trigger an alarm caused by a sensor firing.
     * Turns on the buzzer, lights up the room, calls police, updates display.
     */
    public void triggerAlarm(Sensor sensor) {
        alarmTriggered = true;
        buzzer.switchOn();

        Room room = roomRepository.getRoom(sensor);
        if (room == null) return;

        room.switchLightOn();

        if (screen != null) {
            screen.displayIntrusion(room, sensor);
        }

        phoneController.callPolice(room.getName());
    }

    /**
     * Panic button was pressed (PIN already verified by PanicButton).
     * Same effect as triggerAlarm but room comes from the button, not a sensor.
     */
    public void panicRoom(Room room) {
        alarmTriggered = true;
        buzzer.switchOn();

        if (room != null) {
            room.switchLightOn();
        }

        if (screen != null) {
            // No specific sensor for a panic press — pass null
            screen.displayIntrusion(room, null);
        }

        String location = (room != null) ? room.getName() : "Console";
        phoneController.callPolice(location);
    }

    /**
     * Clear-alarms button was pressed (PIN already verified by ClearAlarmButton).
     * Switches off buzzer, turns off all alarm-triggered lights, resets display.
     */
    public void clearAlarms() {
        alarmTriggered = false;
        buzzer.switchOff();

        // Turn off lights in every room that was intruded
        List<Room> intruded = roomRepository.getIntrudedRooms();
        for (Room r : intruded) {
            r.switchLightOff();
        }

        if (screen != null) {
            screen.clearAllDisplayedIntrusions();
        }

        System.out.println("[BASController] All alarms cleared.");
    }

    public void switchOn() {
        systemIsOn = true;
        System.out.println("[BASController] System switched ON.");
    }

    public void switchOff() {
        systemIsOn = false;
        sensorController.stopPollCycle();
        buzzer.switchOff();
        System.out.println("[BASController] System switched OFF.");
    }

    // =========================================================================
    // Esper event handlers  (private — only called via lambda callbacks)
    // =========================================================================

    private void handleSensorTriggered(Sensor sensor) {
        triggerAlarm(sensor);
    }

    private void handleSensorFailure(Sensor sensor) {
        Room room = roomRepository.getRoom(sensor);

        // Tell a technician — phone call
        String roomName  = (room != null) ? room.getName() : "Unknown";
        String sensorId  = sensor.getSensorId();
        phoneController.callService(roomName, sensorId);

        // Also show it on the console screen
        if (screen != null) {
            screen.displayServiceNeeded(room, sensor);
        }
    }

    private void handlePowerMinorDrop(VoltageChangeEvent event) {
        // 10–20% drop: enable battery only
        battery.turnOn();
        if (screen != null) {
            screen.displayBatteryUsed(true);
        }
        System.out.printf("[BASController] Minor voltage drop (%.1f%%) — battery enabled.%n",
                event.getDropPercent());
    }

    private void handlePowerMajorDrop(VoltageChangeEvent event) {
        // >20% drop: enable battery + raise alarm + call police
        battery.turnOn();
        if (screen != null) {
            screen.displayBatteryUsed(true);
        }

        // Treat major power loss as intrusion (intruder cut the power)
        alarmTriggered = true;
        buzzer.switchOn();
        phoneController.callPolice("Power supply");

        if (screen != null) {
            screen.displayIntrusion(null, null);
        }

        System.out.printf("[BASController] Major voltage drop (%.1f%%) — battery + alarm.%n",
                event.getDropPercent());
    }

    private void handlePowerVoltageRecovered(VoltageChangeEvent event) {
        // Voltage back to normal: switch battery off
        battery.turnOff();
        if (screen != null) {
            screen.displayBatteryUsed(false);
        }
        System.out.println("[BASController] Voltage recovered — battery disabled.");
    }

    private void handlePowerFailure(PowerFailureEvent event) {
        // Power supply hardware failure — call service technician
        phoneController.callService("Power supply", event.getFailureSource());
        if (screen != null) {
            screen.displayServiceNeeded(null, null);
        }
        System.out.println("[BASController] Power failure — technician called.");
    }

    // =========================================================================
    // Entry point
    // =========================================================================

    public static void main(String[] args) {
        new BASController();
    }
}