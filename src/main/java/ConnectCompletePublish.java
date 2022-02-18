import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.concurrent.Semaphore;

public class ConnectCompletePublish implements Runnable {

    private final String statusTopicTemplate = "client/%client_id%/status";

    private final MqttClient mqttClient;
    private final Semaphore lock;
    private final String statusTopic;


    public ConnectCompletePublish(final MqttClient mqttClient, final Semaphore lock) {
        this.mqttClient = mqttClient;
        this.lock = lock;
        statusTopic = statusTopicTemplate.replace("%client_id%", mqttClient.getClientId());
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        try {
            Thread.sleep(10000);
//            If we put this at the end, we risk failing to publish, waiting for 10 seconds, and not reconnecting

            System.out.println("Publishing status to " + statusTopic);
            mqttClient.publish(statusTopic, "UP".getBytes(), 1, true);
            System.out.println("Status published");
        } catch (MqttException | InterruptedException e) {
            System.out.println("Failed to publish status topic, retrying in 10 seconds (Auto Reconnect?)");

        } finally {
            System.out.println("Lock released");
            lock.release();
        }

    }
}
