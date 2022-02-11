import org.eclipse.paho.client.mqttv3.MqttException;

public class PahoThreadsIssue {


    public static void main(String[] args) throws MqttException {
        new PahoCustomMQTTClient().startMQTT();
    }


}


