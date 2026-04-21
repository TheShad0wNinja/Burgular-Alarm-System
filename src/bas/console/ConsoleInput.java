/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bas.console;

/**
 * ConsoleInput
 *
 * Base class for every physical button on the operator console.
 * Holds a Runnable called onPress — whatever you assign to it runs when
 * the button is pressed.
 *
 * BASController sets onPress during startup, like this:
 *   clearBtn.setOnPress(() -> basController.clearAlarms());
 *   panicBtn.setOnPress(() -> basController.panic(room));
 *
 * Subclasses (PanicButton, ClearAlarmButton) override press() to add PIN
 * authentication before the action fires.
 *
 * The field is 'protected' so subclasses can call super.press() to run it.
 */
public class ConsoleInput {

    // The action that runs when this button is pressed.
    // Set by BASController via setOnPress().
    protected Runnable onPress;

    /**
     * Assign an action to this button.
     * @param onPress the action to run on press
     */
    public void setOnPress(Runnable onPress) {
        this.onPress = onPress;
    }

    /**
     * Fire the button's action.
     * Subclasses override this to add authentication first.
     */
    public void press() {
        if (onPress != null) {
            onPress.run();
        } else {
            System.out.println("[ConsoleInput] Button pressed but no action has been set.");
        }
    }
}