package bas.console;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * ConsoleDisplayable
 *
 * The real-world implementation of Displayable.
 * Every call to display() prints immediately to System.out with a timestamp,
 * so the operator sees events the moment they happen (real-time UI).
 *
 * Usage:
 *   ConsoleScreen screen = new ConsoleScreen(new ConsoleDisplayable());
 *
 * In unit tests we can instead pass in a simple lambda:
 *   ConsoleScreen screen = new ConsoleScreen(msg -> capturedMessages.add(msg));
 */
public class ConsoleDisplayable implements Displayable {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Override
    public void display(String message) {
        // System.out.println is synchronised internally, so this is thread-safe.
        String timestamp = "[" + LocalTime.now().format(TIME_FMT) + "]";
        System.out.println(timestamp + " " + message);
    }
}