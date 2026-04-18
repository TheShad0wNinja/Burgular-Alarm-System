/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bas.phone;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

/**
 *
 * @author lenovo
 */
public class PhoneController {

    private ConcurrentLinkedQueue<CallRequest> callQueue = new ConcurrentLinkedQueue<>();
    private VoiceSynthesiser voiceSynth = new VoiceSynthesiser();
    private Phone phone = new Phone();

    public PhoneController() {
        Thread callHandler = new Thread(this::handleCallQueue);
        callHandler.setDaemon(true); // stops automatically when program ends
        callHandler.start();
    }

    void callService(String room, String sensor) {
        performCall(CallType.SERVICE, "Service alert in room " + room + ". Sensor: " + sensor);
    }

    // Called by Esper when a POLICE alert fires (room only)
    public void callPolice(String room) {
        performCall(CallType.POLICE, "Burglar detected in room " + room);
    }

    public synchronized void performCall(CallType type, String msg) {
        CallRequest callReq = new CallRequest();
        callReq.setType(type);
        callReq.setCallMsg(msg);
        callQueue.add(callReq);
        System.out.println("Queued: " + type + " — " + msg);
    }

    public void handleCallQueue() {
        // handle queue 
        while (true) {
            if (!callQueue.isEmpty()) {
                CallRequest curCall = callQueue.poll();
                boolean connected = false;

                // Try ringing up to 5 times
                for (int i = 0; i < 5; i++) {
                    connected = phone.ringPhone(curCall.getType());
                    if (connected) {
                        break;
                    }

                    try {
                        System.out.println("No answer, waiting before retry " + (i + 2) + "...");
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                // If connected, play the message
                if (connected) {
                    Audio audio = voiceSynth.CreateAudio(curCall.getCallMsg());
                    phone.beginCall(audio);
                    phone.endCall();
                } else {
                    System.out.println("Failed to connect after 5 attempts.");
                }

                updateCallLogs(curCall, connected);

            } else {
                // Queue is empty — wait a bit before checking again
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void updateCallLogs(CallRequest req, boolean connected) {
        System.out.println("LOG: Called " + req.getType()
                + " | Message: " + req.getCallMsg()
                + " | Connected: " + connected);
    }
}
