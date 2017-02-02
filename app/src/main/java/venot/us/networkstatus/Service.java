package venot.us.networkstatus;

import android.app.IntentService;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Service extends IntentService {

    private int FOREGROUND_ID = 2489;
    private ServerSocket serverSocket;

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public Service() {
        super("Service");
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        System.out.println("IM ALIVE!!!");

//        AndroidPhoneStateListener phoneListener = new AndroidPhoneStateListener();
//        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
//        telephonyManager.listen(phoneListener, AndroidPhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        ServiceReceiver sr = new ServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TELEPHONY_SERVICE);
        registerReceiver(sr, filter);

        Intent intent1 = new Intent(TELEPHONY_SERVICE);
        sendBroadcast(intent1);

        System.out.println("Telephony listener setup done!" + AndroidPhoneStateListener.signalStrengthValue);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Network status")
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentText("TCP server is up and running!")
                        .setPriority(Notification.PRIORITY_MIN);

        startForeground(FOREGROUND_ID, mBuilder.build());

        Thread serverThread = new Thread(new ServerThread());
        serverThread.start();

//        ServerSocket server = null;
//        try {
//            server = new ServerSocket(4321);
//        } catch (IOException e) {
//            System.out.println("Could not listen on port 4321");
//        }
//
//        Socket client = null;
//        Boolean quit = false;
//        while (!quit) {
//            try {
//                if (server != null) client = server.accept();
//                if (client != null) {
//                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
//                    PrintWriter out = new PrintWriter(client.getOutputStream(), true);
//                    while (true) {
//                        String line = in.readLine();
//                        if (line != null) {
//                            if (line.equals("status")) {
//                                int level;
//                                String conn;
//                                if (getWifiStatus().equals("COMPLETED")) {
//                                    level = getWifiLevel();
//                                    conn = "WIFI";
//                                } else {
//                                    level = AndroidPhoneStateListener.signalStrengthValue;
//                                    conn = getNetworkClass(getApplicationContext());
//                                }
//                                out.println(conn + " " + level);
//                            }
//                            if (line.equals("quit")) {
//                                quit = true;
//                                break;
//                            }
//                        }
//                    }
//                } else {
//                    System.out.println("CLIENT IS NULL");
//                }
//            } catch (IOException e) {
//                System.out.println("Read failed");
//            }
//        }
        try {
            serverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("EXITING SERVICE!!!");
        unregisterReceiver(sr);
        ServiceReceiver.telephony.listen(ServiceReceiver.phoneListener, PhoneStateListener.LISTEN_NONE);
        stopForeground(true);
    }


    public String getWifiStatus() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getSupplicantState().toString();
    }

    public int getWifiLevel() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        int numberOfLevels = 4;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);
    }

    public String getNetworkClass(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "Edge";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "LTE";
            default:
                return "Unknown";
        }
    }

    class ServerThread implements Runnable {

        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(4321);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {

                try {

                    socket = serverSocket.accept();

                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;
        private PrintWriter output;

        public CommunicationThread(Socket clientSocket) {

            this.clientSocket = clientSocket;

            try {

                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
                this.output = new PrintWriter(clientSocket.getOutputStream(), true);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = input.readLine();
                    if (read != null) {
                        if (read.equals("status")) {
                                int level;
                                String conn;
                                if (getWifiStatus().equals("COMPLETED")) {
                                    level = getWifiLevel();
                                    conn = "WiFi";
                                } else {
                                    level = AndroidPhoneStateListener.signalStrengthValue;
                                    conn = getNetworkClass(getApplicationContext());
                                }
                                output.println(conn + " " + level);
                            }
                    } else {
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}