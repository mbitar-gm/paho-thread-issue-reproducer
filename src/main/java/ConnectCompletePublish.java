import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectCompletePublish {

    private final String statusTopicTemplate = "client/%client_id%/status";

    private final MqttClient mqttClient;
    private final String statusTopic;

    private static final Semaphore lock = new Semaphore(1);
    private static final AtomicBoolean isComplete = new AtomicBoolean(false);

    public ConnectCompletePublish(final MqttClient mqttClient) {
        this.mqttClient = mqttClient;
        statusTopic = statusTopicTemplate.replace("%client_id%", mqttClient.getClientId());
        isComplete.set(false);
        run();
    }

    public void run() {
        final Runnable run = () -> {
            final boolean lockAcquired = lock.tryAcquire();
            if (lockAcquired) {
                System.out.println("Starting on connect complete loop");
                publishUntilSucceeded();
                lock.release();
                System.out.println("Lock released");
            } else {
                System.out.println("On connect complete already in progress");
            }
        };

        new Thread(run).start();

    }

    private void publishUntilSucceeded() {

        while (!isComplete.get()) {
            try {
                if (mqttClient.isConnected()) {
                    System.out.println("Publishing status to " + statusTopic);
                    mqttClient.publish(statusTopic, "UP".getBytes(), 1, true);
                    if (mqttClient.isConnected()) {
                        isComplete.set(true);
                    }
                    System.out.println("Status published");
                }
                Thread.sleep(10000);

            } catch (MqttException | InterruptedException e) {
                System.out.println("Failed to publish status topic, retrying in 10 seconds (Auto Reconnect?)");
            }
        }
    }
}
