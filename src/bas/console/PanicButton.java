/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bas.console;

/**
 *
 * @author ahmed
 */

import bas.rooms.Room;

/**
 * PanicButton
 *
 * The physical panic button on the console.
 * It is linked to one specific room (wherever the console is located).
 *
 * Pressing it triggers the alarm, turns on lights near the console,
 * and calls the police — but ONLY after the operator enters the correct PIN.
 * This stops an intruder from pressing it and then cancelling everything.
 *
 * How BASController wires it up:
 *   PanicButton panicBtn = new PanicButton(consoleRoom);
 *   panicBtn.setOnPress(() -> basController.panic(panicBtn.getRoom()));
 *
 * How to simulate a press in tests:
 *   panicBtn.press("1234");   // correct PIN — fires
 *   panicBtn.press("9999");   // wrong PIN  — blocked
 */
public class PanicButton extends ConsoleInput {

    private Room room;

    /**
     * @param room the room this panic button is located in
     */
    public PanicButton(Room room) {
        this.room = room;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    /**
     * Press with PIN authentication.
     * This is the method that should always be used.
     *
     * @param pin the PIN entered by the operator
     */
    public void press(String pin) {
        if (!ConsoleAuth.authenticate(pin)) {
            return; // wrong PIN — do nothing, alarm is NOT triggered
        }
        System.out.println("[PanicButton] PIN accepted. Panic triggered in: "
                + (room != null ? room.getName() : "Unknown Room"));
        super.press(); // runs the onPress Runnable set by BASController
    }

    /**
     * No-PIN version — blocked with a warning.
     * Always use press(String pin) instead.
     */
    @Override
    public void press() {
        System.out.println("[PanicButton] WARNING: Called without PIN. Use press(String pin).");
    }
}