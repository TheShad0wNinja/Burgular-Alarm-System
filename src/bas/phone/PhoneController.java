/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bas.phone;

import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author lenovo
 */
public class PhoneController {

    private Queue<CallRequest> callQueue = new LinkedList<>();
    private VoiceSynthesiser voiceSynth = new VoiceSynthesiser();
    private Phone phone = new Phone();

    void callService(String room, String sensor) {
        performCall(CallType.SERVICE, "Service alert in room " + room + ". Sensor: " + sensor);
    }

    // Called by Esper when a POLICE alert fires (room only)
    public void callPolice(String room) {
        performCall(CallType.POLICE, "Burglar detected in room " + room);
    }

    public synchronized void performCall(CallType type, String msg) {

        // create the callrequest 
        CallRequest callReq = new CallRequest();
        callReq.setType(type);
        callReq.setCallMsg(msg);

        // add to queue
        callQueue.add(callReq);

        // handle queue 
        while (!callQueue.isEmpty()) {
            CallRequest curCall = callQueue.poll();
            boolean connected = false;
            for (int i = 0; i < 5; i++) {
                connected = phone.ringPhone(curCall.getType());
                if (connected) {
                    break;
                }
                //not connected - wait 1 sec then try again
                try {
                    System.out.println("Wwaiting before retry " + (i + 2) + "");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // if connected then play message
            if (connected) {
                Audio audio = voiceSynth.CreateAudio(curCall.getCallMsg());
                phone.beginCall(audio);
                phone.endCall();
            } else {
                System.out.println("Failed to connect after 5 attempts");
            }
            // log result
            updateCallLogs(curCall, connected);
        }

    }

    private void updateCallLogs(CallRequest req, boolean connected) {
        System.out.println("LOG: Called " + req.getType()
                + " | Message: " + req.getCallMsg()
                + " | Connected: " + connected);
    }
}
