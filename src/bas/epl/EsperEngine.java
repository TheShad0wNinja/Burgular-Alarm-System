package bas.epl;

import bas.sensors.Sensor;
import bas.sensors.SensorTriggeredEvent;
import bas.sensors.SensorFailureEvent;
import bas.power.VoltageChangeEvent;
import bas.power.PowerFailureEvent;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import jdk.jfr.Event;

public class EsperEngine {

    private final EPServiceProvider engine;

    public EsperEngine(SensorTriggeredCallback onTrigger, SensorFailureCallback onFailure) {

         engine = EPServiceProviderManager.getDefaultProvider();
         engine.getEPAdministrator().getConfiguration().addEventType(SensorTriggeredEvent.class);
         engine.getEPAdministrator().getConfiguration().addEventType(SensorFailureEvent.class);

        deploySingleTriggeredStatement(onTrigger);
        deployFailureStatement(onFailure);
//        deployRepeatedIntrusionStatement(onRepeatedIntrusion);

        System.out.println("[EsperEngine] Engine ready - 3 EPL statements deployed");
    }

    public EsperEngine(VoltageMinorDropCallback onMinorDrop, VoltageMajorDropCallback onMajorDrop, VoltageRecoveredCallback onVoltageRecovered, PowerFailureCallback onPowerFailure){

        engine = EPServiceProviderManager.getDefaultProvider();
        engine.getEPAdministrator().getConfiguration().addEventType(VoltageChangeEvent.class);
        engine.getEPAdministrator().getConfiguration().addEventType(PowerFailureEvent.class);

        deployMinorDropRule(onMinorDrop);
        deployMajorDropRule(onMajorDrop);
        deployRecoveredRule(onVoltageRecovered);
        deployPowerFailureRule(onPowerFailure);

        System.out.println("[EsperEngine] 4 power EPL rules deployed");
    }

    private void deploySingleTriggeredStatement(SensorTriggeredCallback callback) {

         String epl = "select * from SensorTriggeredEvent";

         EPStatement statement = engine.getEPAdministrator().createEPL(epl, "SensorTriggered");
         statement.addListener((newEvents, oldEvents) -> {
             if (newEvents == null) {
                 return;
             }
             for (EventBean eb : newEvents) {
                 SensorTriggeredEvent event = (SensorTriggeredEvent) eb.getUnderlying();
                 long callbackStart = System.currentTimeMillis();

                 System.out.printf("[Esper: SensorTriggered] -> triggerAlarm %s%n", event.getSensor());
                 callback.onTriggerd(event.getSensor());

                 long cbElapsed = System.currentTimeMillis() - callbackStart;
                 if (cbElapsed > RTSConstraints.SENSOR_TRIGGERED_RESPONSE_MS) {
                     System.out.printf("[RT WARNING][ESPER] triggerAlarm callback took %dms" + "(deadline = %dms)%n", cbElapsed, RTSConstraints.SENSOR_TRIGGERED_RESPONSE_MS);

                 }
             }
         });
     }

    private void deployFailureStatement(SensorFailureCallback callback) {

        String epl = "select * from SensorFailureEvent";

        EPStatement statement = engine.getEPAdministrator().createEPL(epl, "SensorFailure");

        statement.addListener((newEvents, oldEvents) -> {
             if (newEvents == null) return;
             for (EventBean eb : newEvents) {
                 SensorFailureEvent event = (SensorFailureEvent) eb.getUnderlying();
                 long callbackStart = System.currentTimeMillis();

                 System.out.printf("[Esper: SensorFailure] -> onFailure for %s%n", event.getSensor());
                 callback.onFailure(event.getSensor());

                 long cbElapsed = System.currentTimeMillis() - callbackStart;
                 if (cbElapsed > RTSConstraints.FAILURE_NOTIFY_RESPONSE_MS) {

                     System.out.printf("[RT Warning][Esper] onFailure callback took %dms " + "(deadline = %dms)%n", cbElapsed, RTSConstraints.FAILURE_NOTIFY_RESPONSE_MS);
                 }
             }
         });
    }

    private void deployMinorDropRule(VoltageMinorDropCallback cb) {

        String epl = "select * from VoltageChangeEvent " +
                "where dropPercent >= 10 and dropPercent <= 20 and backupEnabled = false";

        EPStatement statement = engine.getEPAdministrator().createEPL(epl, "VoltageDropMinor");

        statement.addListener((newEvents, oldEvents) -> {
            if (newEvents == null){
                return;
            }
            for (EventBean eb : newEvents) {
                VoltageChangeEvent event = (VoltageChangeEvent) eb.getUnderlying();
                long t0 = System.currentTimeMillis();

                System.out.printf("[Esper: VoltageDropMinor] drop = %.1f%% -> enable battery%n",
                        event.getDropPercent());
                cb.onMinorDrop(event);

                checkDeadline("VoltageDropMinor",
                        System.currentTimeMillis() - t0,
                        RTSConstraints.POWER_DROP_MINOR_RESPONSE_MS);
            }
        });
    }

    private void deployMajorDropRule(VoltageMajorDropCallback cb) {

        String epl = "select * from VoltageChangeEvent " + "where dropPercent > 20 and backupEnabled = false";

        EPStatement statement = engine.getEPAdministrator().createEPL(epl, "VoltageDropMajor");

        statement.addListener((newEvents, oldEvents) -> {
            if (newEvents == null){
                return;
            }
            for (EventBean eb : newEvents) {
                VoltageChangeEvent event = (VoltageChangeEvent) eb.getUnderlying();
                long t0 = System.currentTimeMillis();

                System.out.printf("[Esper: VoltageDropMajor] drop = %.1f%% -> enable battery%n",
                        event.getDropPercent());
                cb.onMajorDrop(event);

                checkDeadline("VoltageDropMajor",
                        System.currentTimeMillis() - t0,
                        RTSConstraints.POWER_DROP_MAJOR_RESPONSE_MS);
            }
        });
    }

    private void deployRecoveredRule(VoltageRecoveredCallback cb) {
        String epl = "select * from VoltageChangeEvent " + "where dropPercent < 10 and backupEnabled = true";

        EPStatement statement = engine.getEPAdministrator().createEPL(epl, "VoltageRecovered");
        statement.addListener((newEvents, oldEvents) -> {
            if (newEvents == null) {
                return;
            }
            for (EventBean eb : newEvents) {
                VoltageChangeEvent event = (VoltageChangeEvent) eb.getUnderlying();
                System.out.printf("[Esper : VoltageRecovered] drop = %.1f%% -> disable battery%n",
                        event.getDropPercent());
                cb.onRecovered(event);
            }
        });
    }

    public void deployPowerFailureRule(PowerFailureCallback cb) {
        String epl = "select * from PowerFailureEvent";
        EPStatement statement = engine.getEPAdministrator().createEPL(epl, "PowerFailure");

        statement.addListener((newEvents, oldEvents) -> {
            if (newEvents == null) {
                return;
            }
            for (EventBean eb : newEvents) {
                PowerFailureEvent event = (PowerFailureEvent) eb.getUnderlying();
                long t0 = System.currentTimeMillis();

                System.out.printf("[Esper: PowerFailure] source = %s -> call technician%n",
                        event.getFailureSource());
                cb.onPowerFailure(event);

                checkDeadline("PowerFailure" ,
                        System.currentTimeMillis() - t0,
                        RTSConstraints.FAILURE_NOTIFY_RESPONSE_MS);
            }
        });
    }

    public void sendTriggeredEvent(Sensor sensor) {

         engine.getEPRuntime().sendEvent(new SensorTriggeredEvent(sensor));

     }

     public void sendFailureEvent(Sensor sensor) {

         engine.getEPRuntime().sendEvent(new SensorFailureEvent(sensor));

     }

//     private void deployRepeatedIntrusionStatement(RepeatedIntrusionCallback callback) {
//         String epl = "select count(*) as sensorCount from SensorTriggredEvent.win:time(5000)";
//         EPStatement statement = engine.getEPAdministrator().createEPL(epl, "RepeatedIntrusion");
//         statement.addListener((newEvents, oldEvents) -> {
//             if (newEvents == null) return;
//             for (EventBean eb : newEvents) {
//                 Long sensorCount = (Long) eb.get("sensorCount");
//                 if (sensorCount > 1) {
//                     System.out.printf("[Esper: RepeatedIntrusion] Multiple intrusions detected: %d%n", sensorCount);
//                     callback.onRepeatedIntrusion(sensorCount);
//                 }
//             }
//         });
//     }

     public void sendVoltageChangeEvent(float currentVoltage, float originalVoltage, boolean backupEnabled) {
        engine.getEPRuntime().sendEvent(new VoltageChangeEvent(currentVoltage, originalVoltage, backupEnabled));
     }

     public void sendPowerFailureEvent(String source) {
        engine.getEPRuntime().sendEvent(new PowerFailureEvent(source));
     }

     private void checkDeadline(String ruleName, long elapsed, long deadline) {
        if (elapsed > deadline) {
            System.out.printf("[RT Warning][Esper: %s] callback took %dms (deadline = %dms)%n",
                    ruleName, elapsed, deadline);
        }
     }


    @FunctionalInterface
    public interface SensorTriggeredCallback {
        void onTriggerd(Sensor sensor);
    }

    @FunctionalInterface
    public interface SensorFailureCallback {
        void onFailure(Sensor sensor);
    }

//    @FunctionalInterface
//    public interface RepeatedIntrusionCallback {
//        void onRepeatedIntrusion(long sensorCount);
//    }

    @FunctionalInterface
    public interface VoltageMinorDropCallback {
        void onMinorDrop(VoltageChangeEvent event);
    }

    @FunctionalInterface
    public interface VoltageMajorDropCallback {
        void onMajorDrop(VoltageChangeEvent event);
    }

    @FunctionalInterface
    public interface VoltageRecoveredCallback {
        void onRecovered(VoltageChangeEvent event);
    }

    @FunctionalInterface
    public interface PowerFailureCallback {
        void onPowerFailure(PowerFailureEvent event);
    }

}