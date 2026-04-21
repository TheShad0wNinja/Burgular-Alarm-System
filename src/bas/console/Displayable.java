package bas.console;

/**
 * Displayable interface.
 *
 * Any class that wants to act as a display just implements this one method.
 * The class diagram notes "Implementation will depend on testing environment" —
 * so in real use we pass in ConsoleDisplayable, and in tests we can pass in
 * a mock that captures the output instead of printing it.
 */
public interface Displayable {

    /**
     * Show a message on whatever output this is connected to.
     *
     * @param message the text to display
     */
    void display(String message);
}
