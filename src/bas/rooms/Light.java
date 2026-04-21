/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bas.rooms;

/**
 *
 * @author ahmed
 */

/**
 * Light
 *
 * One light in one room.
 * Can be turned on or off. Tracks its own state so you can always ask isOn().
 *
 * Turned ON  by Room.switchLightOn()  when an alarm is triggered in this room.
 * Turned OFF by Room.switchLightOff() when clearAlarms() is called.
 */
public class Light {

    private boolean isOn = false;

    /** Switch the light on. Does nothing and prints nothing if already on. */
    public void turnOn() {
        if (!isOn) {
            isOn = true;
            System.out.println("[Light] Turned ON");
        }
    }

    /** Switch the light off. Does nothing and prints nothing if already off. */
    public void turnOff() {
        if (isOn) {
            isOn = false;
            System.out.println("[Light] Turned OFF");
        }
    }

    /**
     * @return true if the light is currently on
     */
    public boolean isOn() {
        return isOn;
    }
}
