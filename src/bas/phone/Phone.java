/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bas.phone;

/**
 *
 * @author lenovo
 */
public class Phone {

    private boolean isInCall;

  

    public void setIsInCall(boolean isInCall) {
        this.isInCall = isInCall;
    }

    public boolean getIsInCall() {
        return isInCall;
    }

    public void beginCall(Audio audio) {
        setIsInCall(true);
        System.out.println("Playing message: " + audio);

    }

    public void endCall() {
        setIsInCall(false);
        System.out.println("Call ended.");

    }

    public boolean ringPhone(CallType type) {
        System.out.println("Ringing " + type + "...");
        boolean connected = Math.random() > 0.5;

        if (connected) {
            System.out.println("Connected to " + type + "!");
        } else {
            System.out.println("No answer from " + type);
        }

        return connected;
    }
}
