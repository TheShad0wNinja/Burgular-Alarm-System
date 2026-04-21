/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bas.phone;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lenovo
 */
public class Phone {

    private boolean isInCall;
    private String status = "Idle";

    public boolean getIsInCall() {
        return isInCall;
    }

    public void beginCall(Audio audio) {
        this.isInCall = true;
        this.status = "Speaking: " + audio.getValue();
        System.out.println("Playing message: " + audio);
        try {
            Thread.sleep(5000); // Simulate Call duration
        } catch (InterruptedException ex) {
            Logger.getLogger(Phone.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void endCall() {
        this.isInCall = false;
        this.status = "Call Ended.";
        System.out.println("Call ended.");

    }

    public boolean ringPhone(CallType type) {
        this.status = "Ringing " + type + "...";
        System.out.println("Ringing " + type + "...");
        boolean connected = Math.random() > 0.5;

        if (connected) {
            System.out.println("Connected to " + type + "!");
        } else {
            this.status = "Idle";
            System.out.println("No answer from " + type);
        }

        return connected;
    }
}
