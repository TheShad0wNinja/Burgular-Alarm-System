/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bas.rooms;

/**
 *
 * @author ahmed
 */

import bas.sensors.Sensor;
import java.util.ArrayList;
import java.util.List;

/**
 * Room
 *
 * Represents one physical room (or corridor, or ground floor area) in the building.
 *
 * Each room has:
 *   - a name        (e.g. "Living Room", "Corridor", "Ground Floor")
 *   - a list of sensors installed in it
 *   - exactly one Light
 *
 * BASController uses getLight() to turn the light on when an alarm fires here,
 * and turns it off again when clearAlarms() is called.
 *
 * isIntruded() is used by RoomRepository.getIntrudedRooms() to find all rooms
 * that currently have a triggered sensor.
 */
public class Room {

    private String name;
    private final List<Sensor> sensors;
    private final Light light;

    /**
     * @param name human-readable name shown on the console display
     */
    public Room(String name) {
        this.name    = name;
        this.sensors = new ArrayList<>();
        this.light   = new Light();
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public Light getLight() {
        return light;
    }

    // -------------------------------------------------------------------------
    // Sensor management
    // -------------------------------------------------------------------------

    /**
     * Register a sensor as belonging to this room.
     * Called during system setup (in Main or BASController constructor).
     */
    public void addSensor(Sensor sensor) {
        sensors.add(sensor);
    }

    // -------------------------------------------------------------------------
    // Light control  (called by BASController)
    // -------------------------------------------------------------------------

    /** Turn this room's light on (alarm triggered here). */
    public void switchLightOn() {
        light.turnOn();
    }

    /** Turn this room's light off (alarms cleared). */
    public void switchLightOff() {
        light.turnOff();
    }

    // -------------------------------------------------------------------------
    // Status
    // -------------------------------------------------------------------------

    /**
     * A room is "intruded" if it's alarm lights are on
     *
     * @return true if it was triggered through the BAS
     */
    public boolean isIntruded() {
        return light.isOn();
//        for (Sensor s : sensors) {
//            if (s.getIsTriggred()) {   // matches their spelling: getIsTriggred()
//                return true;
//            }
//        }
//        return false;
    }
}
