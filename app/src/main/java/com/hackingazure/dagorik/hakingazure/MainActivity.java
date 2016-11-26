package com.hackingazure.dagorik.hakingazure;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.microsoft.azure.iothub.DeviceClient;
import com.microsoft.azure.iothub.IotHubClientProtocol;
import com.microsoft.azure.iothub.IotHubEventCallback;
import com.microsoft.azure.iothub.IotHubMessageResult;
import com.microsoft.azure.iothub.IotHubStatusCode;
import com.microsoft.azure.iothub.Message;
import com.microsoft.azure.iothub.MessageCallback;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Scanner;
public class MainActivity extends AppCompatActivity {

    //Mio
    String connString = "HostName=BastonInteligente.azure-devices.net;DeviceId=MyNewDevice;SharedAccessKey=idd7meI5BG33lyRWeGzhdgwBShjYRCOLdOWWurZx/Oo=";
      //String connString = "HostName=BastonInteligente.azure-devices.net;DeviceId=SampleDevice001_920;SharedAccessKey=c2GlMfs1x0WuDIRjEuRJFA==";
    //      HostName=[YourIoTHubName];SharedAccessKeyName=[YourAccessKeyName];SharedAccessKey=[YourAccessKey]

    //Vecino
    //String connString = "HostName=iothuertourbano.azure-devices.net;DeviceId=myFirstDevice;SharedAccessKey=nDBmOZhoWHa6HO28KwqMCHn8v01Da4G6qAStIG5hbM0=";





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            SendMessage();
        }
        catch(IOException e1)
        {
            System.out.println("Exception while opening IoTHub connection: " + e1.toString());
        }
        catch(Exception e2)
        {
            System.out.println("Exception while opening IoTHub connection: " + e2.toString());
        }

    }

    public void SendMessage() throws URISyntaxException, IOException
    {
        // Comment/uncomment from lines below to use HTTPS or MQTT protocol
        IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
      //  IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

        DeviceClient client = new DeviceClient(connString, protocol);

        try {
            client.open();
        }
        catch(IOException e1)
        {
            System.out.println("Exception while opening IoTHub connection: " + e1.toString());
        }
        catch(Exception e2)
        {
            System.out.println("Exception while opening IoTHub connection: " + e2.toString());
        }

        for (int i = 0; i < 5; ++i)
        {
            String msgStr = "Event Message " + Integer.toString(i);
            try
            {
                Message msg = new Message(msgStr);
                msg.setProperty("messageCount", Integer.toString(i));
                System.out.println(msgStr);
                EventCallback eventCallback = new EventCallback();
                client.sendEventAsync(msg, eventCallback, i);
            }
            catch (Exception e)
            {
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        client.close();
    }

    public void btnReceiveOnClick(View v) throws URISyntaxException, IOException {
        Button button = (Button) v;

        // Comment/uncomment from lines below to use HTTPS or MQTT protocol
        // IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

        DeviceClient client = new DeviceClient(connString, protocol);

        if (protocol == IotHubClientProtocol.MQTT)
        {
            MessageCallbackMqtt callback = new MessageCallbackMqtt();
            Counter counter = new Counter(0);
            client.setMessageCallback(callback, counter);
        }
        else
        {
            MessageCallback callback = new MessageCallback();
            Counter counter = new Counter(0);
            client.setMessageCallback(callback, counter);
        }

        try {
            client.open();
        }
        catch(IOException e1)
        {
            System.out.println("Exception while opening IoTHub connection: " + e1.toString());
        }
        catch(Exception e2)
        {
            System.out.println("Exception while opening IoTHub connection: " + e2.toString());
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        client.close();
    }

    // Our MQTT doesn't support abandon/reject, so we will only display the messaged received
    // from IoTHub and return COMPLETE
    public class MessageCallbackMqtt implements com.microsoft.azure.iothub.MessageCallback
    {
        String mensaje;
        String contenido;

        public IotHubMessageResult execute(Message msg, Object context)
        {
            Counter counter = (Counter) context;
             mensaje = "Received message " + counter.toString();
             contenido = " with content: " + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET);

            System.out.println(
                    "Received message " + counter.toString()
                            + " with content: " + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));

            Log.d("MyLog", mensaje+contenido);


            counter.increment();

            return IotHubMessageResult.COMPLETE;
        }

    }

    protected static class EventCallback implements IotHubEventCallback {
        public void execute(IotHubStatusCode status, Object context){
            Integer i = (Integer) context;
            System.out.println("IoT Hub responded to message "+i.toString()
                    + " with status " + status.name());
        }
    }

    protected static class MessageCallback implements com.microsoft.azure.iothub.MessageCallback
    {
        public IotHubMessageResult execute(Message msg, Object context)
        {
            Counter counter = (Counter) context;
            System.out.println(
                    "Received message " + counter.toString()
                            + " with content: " + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));

            int switchVal = counter.get() % 3;
            IotHubMessageResult res;
            switch (switchVal)
            {
                case 0:
                    res = IotHubMessageResult.COMPLETE;
                    break;
                case 1:
                    res = IotHubMessageResult.ABANDON;
                    break;
                case 2:
                    res = IotHubMessageResult.REJECT;
                    break;
                default:
                    // should never happen.
                    throw new IllegalStateException("Invalid message result specified.");
            }

            System.out.println("Responding to message " + counter.toString() + " with " + res.name());

            counter.increment();

            return res;
        }
    }

    /** Used as a counter in the message callback. */
    protected static class Counter
    {
        protected int num;

        public Counter(int num)
        {
            this.num = num;
        }

        public int get()
        {
            return this.num;
        }

        public void increment()
        {
            this.num++;
        }

        @Override
        public String toString()
        {
            return Integer.toString(this.num);
        }
    }
}
