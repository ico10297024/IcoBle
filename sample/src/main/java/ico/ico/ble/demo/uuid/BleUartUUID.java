package ico.ico.ble.demo.uuid;

import ico.ico.ble.BleSocket;

/**
 * Created by root on 18-4-23.
 */

public class BleUartUUID implements BleSocket.BLeUUIDI {

    private static BleUartUUID instance;

    public static BleUartUUID getInstance() {
        if (instance == null) {
            instance = new BleUartUUID();
        }
        return instance;
    }

    @Override
    public String getBleName() {
        return "BLE-UART";
    }

    @Override
    public String getEnableNotificationUUID() {
        return "00002902-0000-1000-8000-00805f9b34fb";
    }

    @Override
    public String getWriteUUID() {
        return "BB8A27E0-C37C-11E3-B953-0228AC012A70";
    }

    @Override
    public String getReadUUID() {
        return "B34AE89E-C37C-11E3-940E-0228AC012A70";
    }
}
