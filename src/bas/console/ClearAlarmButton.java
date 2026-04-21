/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bas.console;

/**
 *
 * @author ahmed
 */
/**
 * ClearAlarmButton
 * The physical "clear alarms" button on the console.
 * Must be authenticated with a PIN — you don't want an intruder
 * to just walk up and cancel the alarm.
 */

/**
 * ClearAlarmButton
 *
 * The physical "clear alarms" button on the console.
 * Switches off the buzzer, turns off all lights that were activated by alarms,
 * and clears the display — but ONLY after the operator enters the correct PIN.
 *
 * You do NOT want an intruder to be able to walk up and silence the alarm,
 * so PIN authentication is required.
 *
 * How BASController wires it up:
 *   ClearAlarmButton clearBtn = new ClearAlarmButton();
 *   clearBtn.setOnPress(() -> basController.clearAlarms());
 *
 * How to simulate a press in tests:
 *   clearBtn.press("1234");   // correct PIN — clears alarms
 *   clearBtn.press("0000");   // wrong PIN  — blocked
 */
public class ClearAlarmButton extends ConsoleInput {

    /**
     * Press with PIN authentication.
     * This is the method that should always be used.
     *
     * @param pin the PIN entered by the operator
     */
    public void press(String pin) {
        if (!ConsoleAuth.authenticate(pin)) {
            return; // wrong PIN — alarms stay on
        }
        System.out.println("[ClearAlarmButton] PIN accepted. Clearing all alarms.");
        super.press(); // runs the onPress Runnable set by BASController
    }

    /**
     * No-PIN version — blocked with a warning.
     * Always use press(String pin) instead.
     */
    @Override
    public void press() {
        System.out.println("[ClearAlarmButton] WARNING: Called without PIN. Use press(String pin).");
    }
}