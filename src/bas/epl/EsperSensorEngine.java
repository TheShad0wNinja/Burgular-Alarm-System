package bas.epl;

import bas.sensors.Sensor;
import bas.sensors.SensorTriggredEvent;
import bas.sensors.SensorFailureEvent;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;

public class EsperSensorEngine {

    private final EPServiceProvider engine;

    public EsperSensorEngine(SensorTriggeredCallback onTrigger, SensorFailureCallback onFailure, RepeatedIntrusionCallback onRepeatedIntrusion) {

        engine = EPServiceProviderManager.getDefaultProvider();
         engine.getEPAdministrator().getConfiguration().addEventType(SensorTriggredEvent.class);
         engine.getEPAdministrator().getConfiguration().addEventType(SensorFailureEvent.class);

        deploySingleTriggeredStatement(onTrigger);
        deployFailureStatement(onFailure);
        deployRepeatedIntrusionStatement(onRepeatedIntrusion);

        System.out.println("[EsperEngine] Engine ready - 3 EPL statements deployed");
    }

    private void deploySingleTriggeredStatement(SensorTriggeredCallback callback) {

         String epl = "select * from SensorTriggeredEvent";

         EPStatement statement = engine.getEPAdministrator().createEPL(epl, "SensorTriggered");
         statement.addListener((newEvents, oldEvents) -> {
             if (newEvents == null) {
                 return;
             }
             for (EventBean eb : newEvents) {
                 SensorTriggredEvent event = (SensorTriggredEvent) eb.getUnderlying();
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

    public void sendTriggeredEvent(Sensor sensor) {

         engine.getEPRuntime().sendEvent(new SensorTriggredEvent(sensor));

     }

     public void sendFailureEvent(Sensor sensor) {

         engine.getEPRuntime().sendEvent(new SensorFailureEvent(sensor));

     }

     private void deployRepeatedIntrusionStatement(RepeatedIntrusionCallback callback) {
         String epl = "select count(*) as sensorCount from SensorTriggredEvent.win:time(5000)";
         EPStatement statement = engine.getEPAdministrator().createEPL(epl, "RepeatedIntrusion");
         statement.addListener((newEvents, oldEvents) -> {
             if (newEvents == null) return;
             for (EventBean eb : newEvents) {
                 Long sensorCount = (Long) eb.get("sensorCount");
                 if (sensorCount > 1) {
                     System.out.printf("[Esper: RepeatedIntrusion] Multiple intrusions detected: %d%n", sensorCount);
                     callback.onRepeatedIntrusion(sensorCount);
                 }
             }
         });
     }

    @FunctionalInterface
    public interface SensorTriggeredCallback {
        void onTriggerd(Sensor sensor);
    }

    @FunctionalInterface
    public interface SensorFailureCallback {
        void onFailure(Sensor sensor);
    }

    @FunctionalInterface
    public interface RepeatedIntrusionCallback {
        void onRepeatedIntrusion(long sensorCount);
    }
}