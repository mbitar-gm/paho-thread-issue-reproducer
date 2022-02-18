import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.*;

public class PahoCustomMQTTClient implements MqttCallbackExtended {

    private static MqttClient mqttClient;
    private static final String clientId = "username";
    private static final String brokerUrl = "ws://127.0.0.1:8888";

    private static final int TIMEOUT_SECONDS = 10;

    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    private final ExecutorService onConnectCompleteExecutor = Executors.newSingleThreadExecutor();
    private final Semaphore onConnectCompleteLock = new Semaphore(1);

    public static void main(String[] args) throws MqttException {
        new PahoCustomMQTTClient();
    }

    public PahoCustomMQTTClient() throws MqttException {
        scheduledExecutorService = Executors.newScheduledThreadPool(4);
        mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence(), scheduledExecutorService);
        mqttClient.setCallback(this);
        mqttClient.setTimeToWait((TIMEOUT_SECONDS + 1) * 1000);
        final MqttConnectOptions connectOptions = getConnectionOptions();
        System.out.println("Connecting client...");
        mqttClient.connect(connectOptions);
        System.out.println("Connected");
    }


    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        System.out.println("Connection complete");
        subscribePublishInBackground();
    }

    private void subscribePublishInBackground() {

        final int locksAcquired = onConnectCompleteLock.drainPermits();
        if (locksAcquired > 0) {
            System.out.println("Submitting on connect complete");

            onConnectCompleteExecutor.submit(new ConnectCompletePublish(mqttClient, onConnectCompleteLock));

        } else {
            System.out.println("On connect complete already in progress");
        }
    }


    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Connection lost");
    }


    @Override
    public void messageArrived(String topic, MqttMessage message) {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    private static MqttConnectOptions getConnectionOptions() {

        final MqttConnectOptions mqttConnectionOptions = new MqttConnectOptions();
        mqttConnectionOptions.setCleanSession(true);
        mqttConnectionOptions.setAutomaticReconnect(true);
        mqttConnectionOptions.setMaxReconnectDelay(10 * 1000);
        mqttConnectionOptions.setUserName(clientId);
        mqttConnectionOptions.setConnectionTimeout(TIMEOUT_SECONDS);

        return mqttConnectionOptions;
    }

}

