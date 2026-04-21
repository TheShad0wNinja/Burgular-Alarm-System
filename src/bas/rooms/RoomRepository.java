/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bas.rooms;

/**
 *
 * @author ahmed
 *
 */

import bas.sensors.Sensor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RoomRepository
 *
 * Stores and manages every room in the building.
 * BASController holds one of these and uses it to:
 *   - find which room a triggered sensor belongs to (getRoom(sensor))
 *   - get every room that currently has an active intrusion (getIntrudedRooms())
 *   - clear lights room by room during clearAlarms()
 *
 * The internal list is a synchronizedList so it is safe to read/write from
 * multiple threads (sensor thread, power thread, etc.) without locking manually.
 */
public class RoomRepository {

    // synchronizedList keeps add/remove thread-safe
    private final List<Room> rooms = Collections.synchronizedList(new ArrayList<>());

    // -------------------------------------------------------------------------
    // Add / Remove
    // -------------------------------------------------------------------------

    /**
     * Register a room with the system.
     * Called once during setup for each room in the building.
     *
     * @param room the room to add (must not be null)
     */
    public void addRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("Room must not be null");
        }
        rooms.add(room);
        System.out.println("[RoomRepository] Added room: " + room.getName());
    }

    /**
     * Remove a room from the system.
     *
     * @param room the room to remove
     * @return true if it was found and removed, false if it was not in the list
     */
    public boolean removeRoom(Room room) {
        boolean removed = rooms.remove(room);
        if (removed) {
            System.out.println("[RoomRepository] Removed room: " + room.getName());
        }
        return removed;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * @return a snapshot copy of all rooms (safe to iterate without holding a lock)
     */
    public List<Room> getRooms() {
        synchronized (rooms) {
            return new ArrayList<>(rooms);
        }
    }

    /**
     * Returns every room that currently has at least one triggered sensor.
     * Used by BASController.clearAlarms() to know which lights to switch off.
     *
     * @return list of intruded rooms (empty list if none)
     */
    public List<Room> getIntrudedRooms() {
        List<Room> intruded = new ArrayList<>();
        synchronized (rooms) {
            for (Room r : rooms) {
                if (r.isIntruded()) {
                    intruded.add(r);
                }
            }
        }
        return intruded;
    }

    /**
     * Find which room a given sensor belongs to.
     * Used by BASController when a sensor fires so it knows which room to light up.
     *
     * @param sensor the sensor to look up
     * @return the Room that contains this sensor, or null if it is not registered to any room
     */
    public Room getRoom(Sensor sensor) {
        synchronized (rooms) {
            for (Room r : rooms) {
                if (r.getSensors().contains(sensor)) {
                    return r;
                }
            }
        }
        return null; // sensor not associated with any room
    }

    /**
     * @return the total number of rooms registered
     */
    public int size() {
        return rooms.size();
    }
}
