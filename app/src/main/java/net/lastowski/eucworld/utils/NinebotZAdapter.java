package net.lastowski.eucworld.utils;

import net.lastowski.eucworld.BluetoothLeService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import timber.log.Timber;

public class NinebotZAdapter {
    private static NinebotZAdapter INSTANCE;
    private Timer keepAliveTimer;
	private boolean settingCommandReady = false;
	private static int updateStep = 0;
	private byte[] settingCommand;
	private static byte[] gamma = new byte[16];
    private static int stateCon = 0;
    private static int protocolVersion = 0;
    private static boolean protocolFound = false;

    NinebotUnpacker unpacker = new NinebotUnpacker();
    NinebotZUnpacker unpackerZ = new NinebotZUnpacker();

    public int getProtocolVersion() { return protocolVersion; }

    public void startKeepAliveTimer(final BluetoothLeService mBluetoothLeService, final String ninebotPassword) {
        Timber.i("Ninebot timer starting");
        updateStep = 0;
        stateCon = 0;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                boolean res;
                if (updateStep == 0) {
                    if (!protocolFound) {
                        protocolVersion += 1;
                        protocolVersion %= 2;
                    }
                    if (stateCon == 0) {
                        if (protocolVersion == 0)
                            // Ninebot Z
                            res = mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.startCommunication().writeBuffer());
                        else
                            // Ninebot One S2: Serial No.
                            res = mBluetoothLeService.writeBluetoothGattCharacteristic(new byte[] {(byte)0x55, (byte)0xaa, (byte)0x03, (byte)0x11, (byte)0x01, (byte)0x10, (byte)0x0e, (byte)0xcc, (byte)0xff});
                        if (!res) updateStep = 39;
                    } else if (stateCon == 1) {
                        if (protocolVersion == 0)
                            // Ninebot Z
                            res = mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.getKey().writeBuffer());
                        else
                            // Ninebot One S2: Controller Version
                            res = mBluetoothLeService.writeBluetoothGattCharacteristic(new byte[] {(byte)0x55, (byte)0xaa, (byte)0x03, (byte)0x11, (byte)0x01, (byte)0x1a, (byte)0x02, (byte)0xce, (byte)0xff});
                        if (!res) updateStep = 39;
                    } else if (stateCon == 2) {
                        if (protocolVersion == 0)
                            // Ninebot Z: BMS Versions
                            res = mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.getSerialNumber().writeBuffer());
                        else
                            // Ninebot One S2
                            res = mBluetoothLeService.writeBluetoothGattCharacteristic(new byte[] {(byte)0x55, (byte)0xaa, (byte)0x03, (byte)0x11, (byte)0x01, (byte)0x66, (byte)0x06, (byte)0x7e, (byte)0xff});
                        if (!res) updateStep = 39;
                    } else if (stateCon == 3) {
                        if (protocolVersion == 0)
                            // Ninebot Z
                            res = mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.getVersion().writeBuffer());
                        else
                            // Ninebot One S2: PIN code
                            res = mBluetoothLeService.writeBluetoothGattCharacteristic(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0x03, (byte) 0x11, (byte) 0x01, (byte) 0x17, (byte) 0x06, (byte) 0xcd, (byte) 0xff});
                        if (!res) updateStep = 39;
                    }
                    /*
                    } else if (settingCommandReady) {
    					if (mBluetoothLeService.writeBluetoothGattCharacteristic(settingCommand)) {
                            //needSlowData = true;
                            settingCommandReady = false;
                            Timber.i("Sent command message");
                        } else updateStep = 39; // after +1 and %10 = 0
    				}
    				*/
    				else {
                        if (protocolVersion == 0)
                            // Ninebot Z
                            res = mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.getLiveData().writeBuffer());
                        else
                            // Ninebot One S2: PIN code
                            res = mBluetoothLeService.writeBluetoothGattCharacteristic(new byte[] {(byte)0x55, (byte)0xaa, (byte)0x03, (byte)0x11, (byte)0x01, (byte)0xb0, (byte)0x20, (byte)0x1a, (byte)0xff});
                        if (!res) updateStep = 39;
                    }
				}
                updateStep += 1;
                updateStep %= 40;
                Timber.i("Step: %d", updateStep);
            }
        };
        Timber.i("Ninebot timer started");
        keepAliveTimer = new Timer();
        keepAliveTimer.scheduleAtFixedRate(timerTask, 0, 25);
    }


	public void resetConnection() {
        protocolFound = false;
        protocolVersion = 0;
        stateCon = 0;
        updateStep = 0;
        gamma = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        stopTimer();
	}

    public String getModel() {
        if (protocolVersion == 0)
            return "Ninebot Z";
        else
            return "Ninebot";
    }

    public static class Status {

        private final int speed;
        private final int voltage;
        private final int batt;
        private final int current;
        private final int power;
        private final int distance;
		private final int temperature;


        Status() {

            speed = 0;
            voltage = 0;
            batt = 0;
            current = 0;
            power = 0;
            distance = 0;
			temperature = 0;

        }

        Status(int speed, int voltage, int batt, int current, int power, int distance,  int temperature) {

            this.speed = speed;
            this.voltage = voltage;
            this.batt = batt;
            this.current = current;
            this.power = power;
            this.distance = distance;
			this.temperature = temperature;

        }

        public int getSpeed() {
            return speed;
        }

        public int getVoltage() {
            return voltage;
        }

        public int getBatt() {
            return batt;
        }

        public int getCurrent() {
            return current;
        }

        public int getPower() {
            return power;
        }

        public int getDistance() {
            return distance;
        }

		public int getTemperature() {
            return temperature;
        }



        @Override
        public String toString() {
            return "Status{" +
                    "speed=" + speed +
                    ", voltage=" + voltage +
                    ", batt=" + batt +
                    ", current=" + current +
                    ", power=" + power +
                    ", distance=" + distance +
					", temperature=" + temperature +

                    '}';
        }
    }
	

    public static class serialNumberStatus extends Status {
        private final String serialNumber;
        serialNumberStatus(String serialNumber) {
            super();
            this.serialNumber = serialNumber;
        }
        public String getSerialNumber() {
            return serialNumber;
        }
        public String getModel() {
            return "Ninebot";
        }
        @Override
        public String toString() {
            return "Infos{" +
                    "serialNumber='" + serialNumber + '\'' + '}';
        }
    }

    public static class versionStatus extends Status {
        private final String version;

        versionStatus(String version) {
            super();
            this.version = version;
        }

        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return "Infos{" +
                    "version='" + version + '\'' +
                    '}';
        }
    }


    public static class activationStatus extends Status {
        private final String activationDate;

        activationStatus(String activationDate) {
            super();
            this.activationDate = activationDate;
        }

        public String getVersion() {
            return activationDate;
        }

        @Override
        public String toString() {
            return "Infos{" +
                    "activation='" + activationDate + '\'' +
                    '}';
        }
    }

    private static String toHexString(byte[] buffer){
        String str = "[";

        for (int c : buffer) {
            str += String.format("%02X", (c & 0xFF));
        }
        str += "]";
        return str;
    }
    /**
     * Created by cedric on 29/12/2016.
     */
    public static class CANMessage {

        enum Addr {
            Controller(0x14),
            KeyGenerator(0x16),
            App(0x3e);

            private int value;

            Addr(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        enum Comm {
            Read(0x01),
            Write(0x03),
            Get(0x04),
            GetKey(0x5b);

            private int value;

            Comm(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        enum Param {
            GetKey(0x00),
            Start(0x68),
            SerialNumber(0x10),
            Firmware(0x1a),
            Angles(0x61),
            BatteryLevel(0x22),
            ActivationDate(0x69),
            LiveData(0xb0);


            private int value;

            Param(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        int len = 0;
        int source = 0;
        int destination = 0;
        int command = 0;
        int parameter = 0;
        byte[] data;
        int crc = 0;


        CANMessage(byte[] bArr) {
            if (protocolVersion == 0) {
                // Ninebot Z
                if (bArr.length < 7) return;
                len = bArr[0] & 0xff;
                source = bArr[1] & 0xff;
                destination = bArr[2] & 0xff;
                command = bArr[3] & 0xff;
                parameter = bArr[4] & 0xff;
                data = Arrays.copyOfRange(bArr, 5, bArr.length-2);
                crc = bArr[bArr.length-1] << 8 + bArr[bArr.length-2];
            }
            else {
                // Ninebot One S2
                if (bArr.length < 6) return;
                len = bArr[0] & 0xff;
                source = bArr[1] & 0xff;
                destination = bArr[1] & 0xff;
                command = bArr[2] & 0xff;
                parameter = bArr[3] & 0xff;
                data = Arrays.copyOfRange(bArr, 4, bArr.length-2);
                crc = bArr[bArr.length-1] << 8 + bArr[bArr.length-2];
            }
        }

        private CANMessage() {

        }

        public byte[] writeBuffer() {

            byte[] canBuffer = getBytes();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(0x5A);
            out.write(0xA5);
            try {
                out.write(canBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return out.toByteArray();

        }


        private byte[] getBytes() {

            ByteArrayOutputStream buff = new ByteArrayOutputStream();

            buff.write(len);
            buff.write(source);
            buff.write(destination);
            buff.write(command);
            buff.write(parameter);
            try {
                buff.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            crc = computeCheck(buff.toByteArray());
            buff.write(crc&0xff);
            buff.write((crc>>8)&0xff);
            byte[] cryptedBuffer = crypto(buff.toByteArray());
            return cryptedBuffer;
        }

        public void clearData() {
            data = new byte[data.length];
        }

        private static int computeCheck(byte[] buffer) {

            int check = 0;
            for (byte c : buffer) {
                check = check + ((int)c & 0xff);
				//check = (check + (int) c) % 256;
            }
            check ^= 0xFFFF;
            check &= 0xFFFF;

            return check;
        }
/////////////// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< fix ME
        static CANMessage verify(byte[] buffer) {

            Timber.i("Verifying");
            byte[] dataBuffer = Arrays.copyOfRange(buffer, 2, buffer.length);
            dataBuffer = crypto(dataBuffer);

            int check = (dataBuffer[dataBuffer.length-1]<<8 | ((dataBuffer[dataBuffer.length-2]) & 0xff)) & 0xffff;
            byte [] dataBufferCheck = Arrays.copyOfRange(dataBuffer, 0, dataBuffer.length-2);
            int checkBuffer = computeCheck(dataBufferCheck);
            if (check == checkBuffer) {
                Timber.i("Check OK");
            } else {
                Timber.i("Check FALSE, packet: %02X, calc: %02X",check, checkBuffer);
            }
            return (check == checkBuffer) ? new CANMessage(dataBuffer) : null;

            //return new CANMessage(dataBuffer);

        }

        static byte[] crypto(byte[] buffer) {

            byte[] dataBuffer = Arrays.copyOfRange(buffer, 0, buffer.length);
            //String crypto_text = "";
            //for (int j = 0; j < dataBuffer.length; j++) {
            //    crypto_text += String.format("%02X", dataBuffer[j]);
            //}
            //Timber.i("Initial packet: %s", crypto_text);
            Timber.i("Initial packet: %s", toHexString(dataBuffer));
            for (int j = 1; j < dataBuffer.length; j++) {
                dataBuffer[j] ^= gamma[(j-1)%16];
            }
            //crypto_text = "";
            //for (int j = 0; j < dataBuffer.length; j++) {
            //    crypto_text += String.format("%02X", dataBuffer[j]);
            //}
            //Timber.i("Decrypted packet: %s", crypto_text);
            Timber.i("En/Decrypted packet: %s", toHexString(dataBuffer));


            return dataBuffer;

        }

        private int intFromBytes(byte[] bytes, int starting) {
            if (bytes.length >= starting + 4) {
                return (((((((bytes[starting + 3] & 255)) << 8) | (bytes[starting + 2] & 255)) << 8) | (bytes[starting + 1] & 255)) << 8) | (bytes[starting] & 255);
            }
            return 0;
        }

        public int shortFromBytes(byte[] bytes, int starting) {
            if (bytes.length >= starting + 2) {
                return (((bytes[starting + 1] & 255) << 8) | (bytes[starting] & 255));
            }
            return 0;
        }

        public short signedShortFromBytes(byte[] bytes, int starting) {
            if (bytes.length >= starting + 2) {
                return (short) (((short) (((short) ((bytes[starting + 1] & 255))) << 8)) | (bytes[starting] & 255));
            }
            return (short) 0;
        }

        public static CANMessage startCommunication() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.Start.getValue();
            msg.data = new byte[]{0x02};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getKey() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.KeyGenerator.getValue();
            msg.command = Comm.GetKey.getValue();
            msg.parameter = Param.GetKey.getValue();
            msg.data = new byte[]{};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getSerialNumber() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.SerialNumber.getValue();
            msg.data = new byte[]{0x0e};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getVersion() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.Firmware.getValue();
            msg.data = new byte[]{0x02};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getActivationDate() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.ActivationDate.getValue();
            msg.data = new byte[]{0x02};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getLiveData() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.LiveData.getValue();
            msg.data = new byte[]{0x20};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }




        private byte[] parseKey() {
            byte [] gammaTemp = Arrays.copyOfRange(data, 0, data.length);
            String gamma_text = "";
            for (int j = 0; j < data.length; j++) {
                gamma_text += String.format("%02X", data[j]);
            }
            Timber.i("New key: %s", gamma_text);
            return gammaTemp;
        }

        serialNumberStatus parseSerialNumber() {
            String serialNumber;
            serialNumber = new String(data);
            return new serialNumberStatus(serialNumber);
        }

        versionStatus parseVersionNumber() {
            String versionNumber = "";
            for (int j = 0; j < data.length; j++) {
                versionNumber += String.format("%02X", data[j]);
            }
            return new versionStatus(versionNumber);
        }

        versionStatus parseVersionNumberOneS2() {
            String versionNumber = String.format(Locale.US, "%d.%d.%d", data[1] >> 4, data[0] >> 4, data[0] & 0xf);
            return new versionStatus(versionNumber);
        }

        activationStatus parseActivationDate() {

            int activationDate = this.shortFromBytes(data, 0);
            int year = activationDate>>9;
            int mounth = (activationDate>>5) & 0x0f;
            int day = activationDate & 0x1f;
            String activationDateStr = String.format("%02d.%02d.20%02d", day, mounth,year);
            return new activationStatus(activationDateStr);
        }

        Status parseLiveData() {
            int batt = this.shortFromBytes(data, 8);
            int speed = this.shortFromBytes(data, 10);
            int distance = this.intFromBytes(data,14);
            int temperature = this.shortFromBytes(data,22);
            int voltage = this.shortFromBytes(data, 24);
            int current = this.signedShortFromBytes(data, 26);
            int power = voltage*current;


           return new Status(speed, voltage, batt, current, power, distance, temperature);
        }

        Status parseLiveDataOneS2() {
            int batt = this.shortFromBytes(data, 8);
            int speed = this.shortFromBytes(data, 10) / 10;
            int distance = this.intFromBytes(data,14);
            int temperature = this.shortFromBytes(data,22);
            int voltage = this.shortFromBytes(data, 24);
            int current = this.signedShortFromBytes(data, 26);
            int power = voltage*current;
            return new Status(speed, voltage, batt, current, power, distance, temperature);
        }

        public byte[] getData() {
            return data;
        }

        public static String toHexString(byte[] buffer) {

            String str = "[";

            boolean comma = false;

            for (int c : buffer) {

                if (comma) {
                    str += ", ";
                }

                str += String.format("%02X", (c & 0xFF));
                //comma = true;
            }

            str += "]";

            return str;
        }
    }

    public ArrayList<Status> charUpdated(byte[] data) {
        ArrayList<Status> outValues = new ArrayList<>();

        if (protocolVersion == 0) {
            for (byte c : data) {
                if (unpackerZ.addChar(c)) {
                    Timber.i("Starting verification");
                    CANMessage result = CANMessage.verify(unpackerZ.getBuffer());
                    if (result != null) { // data OK
                        Timber.i("Verification successful, command %02X", result.parameter);
                        if (result.parameter == CANMessage.Param.Start.getValue()) {
                            Timber.i("Get start answer");
                            stateCon = 1;

                        } else if (result.parameter == CANMessage.Param.GetKey.getValue()) {
                            Timber.i("Get encryption key");
                            gamma = result.parseKey();
                            stateCon = 2;
                            //Alert alert = result.parseAlertInfoMessage();
                            //if (alert != null)
                            //	outValues.add(alert);

                        } else if (result.parameter == CANMessage.Param.SerialNumber.getValue()) {
                            Timber.i("Get serial number");
                            serialNumberStatus infos = result.parseSerialNumber();
                            stateCon = 3;
                            //Alert alert = result.parseAlertInfoMessage();
                            if (infos != null)
                                outValues.add(infos);

                        } else if (result.parameter == CANMessage.Param.Firmware.getValue()) {
                            Timber.i("Get version number");
                            versionStatus infos = result.parseVersionNumber();
                            stateCon = 4;
                            //Alert alert = result.parseAlertInfoMessage();
                            if (infos != null)
                                outValues.add(infos);

                        } else if (result.parameter == CANMessage.Param.LiveData.getValue()) {
                            Timber.i("Get life data");
                            Status status = result.parseLiveData();
                            if (status != null) {
                                outValues.add(status);
                            }
                        }

                    }
                }
            }
        }
        else
        if (protocolVersion == 1) {
            for (byte c : data) {
                if (unpacker.addChar(c)) {
                    Timber.i("Starting verification");
                    byte [] d = unpacker.getBuffer();
                    CANMessage result = CANMessage.verify(d);
                    if (result != null) { // data OK
                        switch (d[5]) {
                            case (byte)0x10:    // Serial No.
                                stateCon = 1;
                                serialNumberStatus sns = result.parseSerialNumber();
                                if (sns != null) outValues.add(sns);
                                break;
                            case (byte)0x1a:    // Controller firmware version
                                stateCon = 2;
                                versionStatus vs = result.parseVersionNumberOneS2();
                                if (vs != null) outValues.add(vs);
                                break;
                            case (byte)0x66:    // BMS firmware versions
                                stateCon = 3;
                                break;
                            case (byte)0x17:    // PIN number
                                stateCon = 4;
                                break;
                            case (byte)0xb0:    // Live data
                                Status s = result.parseLiveDataOneS2();
                                if (s != null) outValues.add(s);
                                break;
                        }
                    }
                }
            }
        }
        return outValues;
    }

    static class NinebotUnpacker {
        enum UnpackerState {
            unknown,
            started,
            collecting,
            done
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int oldc = 0;
        int len = 0;
        UnpackerState state = UnpackerState.unknown;
        byte[] getBuffer() {
            return buffer.toByteArray();
        }
        boolean addChar(int c) {
            switch (state) {
                case collecting:
                    buffer.write(c);
                    if (buffer.size() == len+6) {
                        protocolFound = true;
                        state = UnpackerState.done;
                        updateStep = 0;
                        Timber.i("Len %d", len);
                        Timber.i("Step reset");
                        return true;
                    }
                    break;
                case started:
                    buffer.write(c);
                    len = c & 0xff;
                    state = UnpackerState.collecting;
                    break;
                default:
                    if (c == (byte) 0xAA && oldc == (byte) 0x55) {
                        Timber.i("Find start");
                        buffer = new ByteArrayOutputStream();
                        buffer.write(0x55);
                        buffer.write(0xAA);
                        state = UnpackerState.started;
                    }
                    oldc = c;
            }
            return false;
        }
    }


    static class NinebotZUnpacker {

        enum UnpackerState {
            unknown,
            started,
            collecting,
            done
        }


        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int oldc = 0;
        int len = 0;
        UnpackerState state = UnpackerState.unknown;

        byte[] getBuffer() {
            return buffer.toByteArray();
        }

        boolean addChar(int c) {

            switch (state) {

                case collecting:

                    buffer.write(c);
                    if (buffer.size() == len+9) {
                        protocolFound = true;
                        state = UnpackerState.done;
                        updateStep = 0;
                        Timber.i("Len %d", len);
                        Timber.i("Step reset");
                        return true;
                    }
                    break;

                case started:

                    buffer.write(c);
                    len = c & 0xff;
                    state = UnpackerState.collecting;

                    break;

                default:
                    if (c == (byte) 0xA5 && oldc == (byte) 0x5A) {
                        Timber.i("Find start");
                        buffer = new ByteArrayOutputStream();
                        buffer.write(0x5A);
                        buffer.write(0xA5);
                        state = UnpackerState.started;
                    }
                    oldc = c;

            }
            return false;
        }

        void reset() {
            buffer = new ByteArrayOutputStream();
            oldc = 0;
            state = UnpackerState.unknown;

        }
    }

    public static NinebotZAdapter getInstance() {
        Timber.i("Get instance");
        if (INSTANCE == null) {
            Timber.i("New instance");
            INSTANCE = new NinebotZAdapter();
        }
        return INSTANCE;
    }

    public static void newInstance() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;

        }
        Timber.i("New instance");
        INSTANCE = new NinebotZAdapter();
    }

    public static void stopTimer() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;
        }
        Timber.i("Kill instance, stop timer");
        INSTANCE = null;
    }

}
