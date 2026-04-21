/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bas.console;

/**
 * ConsoleAuth
 *
 * Simple PIN-based authentication for console inputs.
 * Both PanicButton and ClearAlarmButton call this before doing anything,
 * so a random person can't just walk up and cancel the alarm or trigger a police call.
 *
 * The PIN is "1234". In a real product it would be stored securely elsewhere,
 * but for this prototype a hard coded value is fine.
 *
 * All methods are static — no need to create an instance.
 */
public class ConsoleAuth {

    private static final String CORRECT_PIN = "1234";

    // Prevent instantiation
    private ConsoleAuth() {}

    /**
     * Check whether the supplied PIN is correct.
     *
     * @param enteredPin the PIN typed by the operator
     * @return true if correct, false otherwise
     */
    public static boolean authenticate(String enteredPin) {
        if (enteredPin == null) {
            System.out.println("[AUTH] No PIN entered. Action blocked.");
            return false;
        }
        boolean ok = CORRECT_PIN.equals(enteredPin.trim());
        if (!ok) {
            System.out.println("[AUTH] Incorrect PIN. Action blocked.");
        }
        return ok;
    }
}