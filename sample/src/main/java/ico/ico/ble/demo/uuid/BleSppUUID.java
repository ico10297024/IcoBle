package ico.ico.ble.demo.uuid;

import ico.ico.ble.BleSocket;

/**
 * Created by root on 18-4-23.
 */

public class BleSppUUID implements BleSocket.BLeUUIDI {

    private static BleSppUUID instance;

    public static BleSppUUID getInstance() {
        if (instance == null) {
            instance = new BleSppUUID();
        }
        return instance;
    }

    @Override
    public String getEnableNotificationUUID() {
        return "00002902-0000-1000-8000-00805f9b34fb";
    }

    @Override
    public String getWriteUUID() {
        return "0000fee2-0000-1000-8000-00805f9b34fb";
    }

    @Override
    public String getReadUUID() {
        return "0000fee1-0000-1000-8000-00805f9b34fb";
    }
}
