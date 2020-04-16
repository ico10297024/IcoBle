package ico.ico.ble.demo.db;

import android.os.Parcel;
import android.os.Parcelable;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Unique;

/**
 * Created by root on 18-2-1.
 */

@Entity
public class Device implements Parcelable {

    public static final Parcelable.Creator<Device> CREATOR = new Parcelable.Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel source) {
            return new Device(source);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };
    @Id(autoincrement = true)
    private Long id;
    private String name;
    @Unique
    @NotNull
    private String serial;
    /**
     * 设备类型,0ble 1dm
     */
    @NotNull
    private int type;

    @Generated(hash = 644977552)
    public Device(Long id, String name, @NotNull String serial, int type) {
        this.id = id;
        this.name = name;
        this.serial = serial;
        this.type = type;
    }

    public Device(int type) {
        this.type = type;
    }

    @Generated(hash = 1469582394)
    public Device() {
    }

    protected Device(Parcel in) {
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.name = in.readString();
        this.serial = in.readString();
        this.type = in.readInt();
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSerial() {
        return this.serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.id);
        dest.writeString(this.name);
        dest.writeString(this.serial);
        dest.writeInt(this.type);
    }
}
