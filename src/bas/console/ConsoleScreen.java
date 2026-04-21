package bas.console;

import bas.rooms.Room;
import bas.sensors.Sensor;

/**
 * ConsoleScreen
 *
 * The operator's screen. Shows intrusions, power events, and service alerts.
 * Every method is synchronized so multiple threads (sensor thread, power thread, etc.)
 * can call it at the same time without garbling the output.
 *
 * It delegates the actual printing to a Displayable, meaning we can swap in
 * a test mock without touching this class.
 *
 * Wiring example (done in Main or BASController):
 *   ConsoleScreen screen = new ConsoleScreen(new ConsoleDisplayable());
 */
public class ConsoleScreen {

    private final Displayable display;

    public ConsoleScreen(Displayable display) {
        this.display = display;
    }

    // -------------------------------------------------------------------------
    // Display methods (called by BASController / EsperEngine callbacks)
    // -------------------------------------------------------------------------

    /**
     * Show an intrusion alert.
     * Called as soon as a sensor is triggered.
     *
     * @param room   the room where the intrusion was detected (can be null if unknown)
     * @param sensor the sensor that fired (can be null if unknown)
     */
    public synchronized void displayIntrusion(Room room, Sensor sensor) {
        String roomName   = (room   != null) ? room.getName()                    : "Unknown Room";
        String sensorName = (sensor != null) ? sensor.getClass().getSimpleName() : "Unknown Sensor";
        display.display("*** INTRUSION DETECTED ***  Room: " + roomName
                + "  |  Sensor: " + sensorName);
    }

    /**
     * Clear the intrusion messages from the screen.
     * Called when the operator presses the clear-alarms button.
     */
    public synchronized void clearAllDisplayedIntrusions() {
        display.display("--- All alarms cleared. Display reset. ---");
    }

    /**
     * Show whether the battery backup has been switched on or off.
     *
     * @param isUsed true  = battery is now powering the system (main power lost)
     *               false = battery has been switched off (main power restored)
     */
    public synchronized void displayBatteryUsed(boolean isUsed) {
        if (isUsed) {
            display.display("[POWER] Battery backup ENABLED  -  main power lost.");
        } else {
            display.display("[POWER] Battery backup DISABLED  -  main power restored.");
        }
    }

    /**
     * Show a service-needed alert.
     * Called when a sensor or the power supply fails.
     *
     * Both parameters are optional (can be null) because a power failure has
     * no specific room or sensor.
     *
     * @param room   the room where the failure occurred, or null for system-wide failures
     * @param sensor the failed sensor, or null for power failures
     */
    public synchronized void displayServiceNeeded(Room room, Sensor sensor) {
        String location    = (room   != null) ? "Room: " + room.getName()
                                              : "System (no specific room)";
        String sensorInfo  = (sensor != null) ? "  |  Sensor: " + sensor.getSensorId()
                                              : "";
        display.display("[SERVICE NEEDED]  " + location + sensorInfo
                + "  - A technician is being contacted.");
    }
}