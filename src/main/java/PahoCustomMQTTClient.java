import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PahoCustomMQTTClient implements MqttCallbackExtended {

    private static MqttClient mqttClient;

    private static final String clientId = "username";
    private static final String brokerUrl = "ws://127.0.0.1:8888";
    private static final String statusTopic = "client/" + clientId + "/status";

    private static final int TIMEOUT_SECONDS = 10;

    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);


    public static void main(String[] args) throws MqttException, InterruptedException {
        final PahoCustomMQTTClient client = new PahoCustomMQTTClient();
//        Thread.sleep(10000);
//        System.out.println("Stopping client");
//        client.stopClient();

//        Thread.sleep(3000);
//        final PahoCustomMQTTClient client2 = new PahoCustomMQTTClient();
//        Thread.sleep(10000);
//        System.out.println("Stopping client2");
//        client2.stopClient();
//
//        Thread.sleep(3000);
//        final PahoCustomMQTTClient client3 = new PahoCustomMQTTClient();
//        Thread.sleep(10000);
//        System.out.println("Stopping client3");
//        client3.stopClient();
//
//        Thread.sleep(50000);

//        System.out.println("Client WSAITGAs");
//        Thread.sleep(20000);
//        System.out.println("Stopping client");
//        client.stopClient();


    }

    public PahoCustomMQTTClient() throws MqttException {
        scheduledExecutorService = Executors.newScheduledThreadPool(4);
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
//        connectCompleteLogic();
        try {
            System.out.println("Publishing status to " + statusTopic);
            mqttClient.publish(statusTopic, "UP".getBytes(), 1, true);
            System.out.println("Status published");
        } catch (MqttException e) {
            System.out.println("Failed to publish status topic, retrying in 10 seconds (Auto Reconnect?)");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }

        }
    }

    private void connectCompleteLogic() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Publishing status to " + statusTopic);
                    mqttClient.publish(statusTopic, "UP".getBytes(), 1, true);
                    System.out.println("Status published");
                } catch (MqttException e) {
                    System.out.println("Failed to publish status topic, retrying in 10 seconds (Auto Reconnect?)");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }

                }
            }
        };
        new Thread(runnable).start();
    }


    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Connection lost");
        try {
            Thread.sleep(500);
            System.out.println("RECONNECTING");
            mqttClient.reconnect();
        } catch (Exception e) {
            System.out.println("FAILED TO RECONNECT");
            e.printStackTrace();
        }
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
        mqttConnectionOptions.setAutomaticReconnect(false);
//        mqttConnectionOptions.setMaxReconnectDelay(10 * 1000);
        mqttConnectionOptions.setUserName(clientId);
        mqttConnectionOptions.setConnectionTimeout(TIMEOUT_SECONDS);

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
                        System.out.println("This will fail");
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
        mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence(), scheduledExecutorService);
        mqttClient.setCallback(this);
        mqttClient.setTimeToWait((TIMEOUT_SECONDS + 1) * 1000);
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

        System.out.println("Stopping executor service");
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

