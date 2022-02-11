import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PahoCustomMQTTClient implements MqttCallbackExtended {

    private final static String clientId = "username";
    private final static String brokerUrl = "ws://127.0.0.1:8888";
    private final static String statusTopic = "client/" + clientId + "/status";
    private static MqttClient mqttClient;
    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(4);
    private static MemoryPersistence memoryPersistence = new MemoryPersistence();


    public void startMQTT() throws MqttException {
        mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence(), scheduledExecutorService);
        mqttClient.setCallback(this);
        mqttClient.setTimeToWait(4 * 1000);
        final MqttConnectOptions connectOptions = getConnectionOptions();
        System.out.println("Connecting client...");
        mqttClient.connect(connectOptions);
        System.out.println("Connected");

    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        System.out.println("Connection complete");
        try {
            System.out.println("Publishing status to " + statusTopic);
            mqttClient.publish(statusTopic, "UP".getBytes(), 1, true);
            System.out.println("Status published");
        } catch (MqttException e) {
            System.out.println("Failed to publish status topic, retrying in 10 seconds");
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            restartClient();
        }
    }

    private void restartClient() {
        try {
            stopClient();
            connectClient();
        } catch (Throwable t) {
            System.out.println("Error restarting client");
            t.printStackTrace();
        }
    }

    private void connectClient() {
        try {
            instantiateClient();
        } catch (MqttException e) {
            System.out.println("Error instantiating MQTT Client");
            e.printStackTrace();
            System.exit(1);
        }
        connectMQTT();
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
        mqttConnectionOptions.setConnectionTimeout(3);

        return mqttConnectionOptions;
    }

    private void disconnectForcibly() {
        try {
            mqttClient.disconnectForcibly(1000, 1000, false);
        } catch (MqttException e) {
            System.out.println("Could not disconnect client");
            e.printStackTrace();
        }
    }

    private void connectMQTT() {
        long connectionTries = 0;
        System.out.println("Connecting to broker");
        try {
            do {
                connectionTries++;
                try {
                    final MqttConnectOptions connectOptions = getConnectionOptions();
                    mqttClient.connect(connectOptions);
                } catch (MqttException e) {
                    if (connectionTries == 1) {
                        System.out.println("Error connecting to broker. Will keep re-trying every 10 seconds");
                        e.printStackTrace();
                    }
                    System.out.println("Error connecting to broker after " + connectionTries + " tries. Retrying in 10 seconds.");
                    Thread.sleep(10 * 1000);
                    if (e.getMessage().contains("Connect already in progress")) {
                        restartClient();
                    }
                }
            } while (!mqttClient.isConnected());
            System.out.println("Finished connecting to broker");
        } catch (InterruptedException e) {
            System.out.println("Error instantiating MQTT Client");
            e.printStackTrace();
            System.exit(1);
        }

    }

    private void instantiateClient() throws MqttException {
        scheduledExecutorService = Executors.newScheduledThreadPool(4);
        memoryPersistence = new MemoryPersistence();
        mqttClient = new MqttClient(brokerUrl, clientId, memoryPersistence, scheduledExecutorService);
        mqttClient.setCallback(this);
        mqttClient.setTimeToWait(4 * 1000);
    }

    private void stopClient() {
        try {
            System.out.println("Disconnecting client");
            disconnectForcibly();
            System.out.println("Closing client");
            mqttClient.close(true);
            System.out.println("Client closed");
        } catch (MqttException e) {
            System.out.println("Failed to close client");
            e.printStackTrace();
        }
        
        try {
            memoryPersistence.clear();
            memoryPersistence.close();
        } catch (MqttPersistenceException e) {
            System.out.println("Failed to clear MemoryPersistence");
            e.printStackTrace();

        }
        memoryPersistence = null;
        
        scheduledExecutorService.shutdown();
        scheduledExecutorService.shutdownNow();
        try {
            scheduledExecutorService.awaitTermination(5000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("Cannot stop executor service");
            e.printStackTrace();
        }
        mqttClient = null;
    }

}

