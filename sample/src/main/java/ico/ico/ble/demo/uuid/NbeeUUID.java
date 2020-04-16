package ico.ico.ble.demo.uuid;

import ico.ico.ble.BleSocket;

/**
 * Created by root on 18-4-23.
 */

public class NbeeUUID implements BleSocket.BLeUUIDI {

    private static  volatile NbeeUUID instance;

    public static NbeeUUID getInstance() {
        if (instance == null) {
            synchronized (NbeeUUID.class) {
                if (instance == null) {
                    instance = new NbeeUUID();
                }
            }
        }
        return instance;
    }

    @Override
    public String getBleName() {
        return "NBee";
    }

    @Override
    public String getEnableNotificationUUID() {
        return "00002902-0000-1000-8000-00805f9b34fb";
    }

    @Override
    public String getWriteUUID() {
        return "0000fff2-0000-1000-8000-00805f9b34fb";
    }

    @Override
    public String getReadUUID() {
        return "0000fff1-0000-1000-8000-00805f9b34fb";
    }
}
