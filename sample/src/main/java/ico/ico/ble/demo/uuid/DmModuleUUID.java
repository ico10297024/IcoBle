package ico.ico.ble.demo.uuid;

import ico.ico.ble.BleSocket;

/**
 * Created by root on 18-4-23.
 */

public class DmModuleUUID implements BleSocket.BLeUUIDI {

    private static DmModuleUUID instance;

    public static DmModuleUUID getInstance() {
        if (instance == null) {
            instance = new DmModuleUUID();
        }
        return instance;
    }

    @Override
    public String getBleName() {
        return "DM Module";
    }

    @Override
    public String getEnableNotificationUUID() {
        return "00002902-0000-1000-8000-00805f9b34fb";
    }

    @Override
    public String getWriteUUID() {
        return "0000ff02-0000-1000-8000-00805f9b34fb";
    }

    @Override
    public String getReadUUID() {
        return "0000ff01-0000-1000-8000-00805f9b34fb";
    }
}
