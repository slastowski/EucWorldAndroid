package net.lastowski.eucworld;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.os.Vibrator;

import net.lastowski.eucworld.utils.BatteryGauge;
import net.lastowski.eucworld.utils.Constants;
import net.lastowski.eucworld.utils.Constants.ALARM_TYPE;
import net.lastowski.eucworld.utils.Constants.WHEEL_TYPE;
import net.lastowski.eucworld.utils.InMotionAdapter;
import net.lastowski.eucworld.utils.NinebotAdapter;
import net.lastowski.eucworld.utils.NinebotZAdapter;
import net.lastowski.eucworld.utils.SettingsUtil;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class WheelData {

    private static final int TIME_BUFFER = 10;
    private static WheelData mInstance;
	private Timer timer;
	private Context mContext;

    private BluetoothLeService mBluetoothLeService;

    private long graph_last_update_time;
    private static final int GRAPH_UPDATE_INTERVAL = 1000; // milliseconds
    private static final int MAX_BATTERY_AVERAGE_COUNT = 150;
    private static final int MAX_CURRENT_AVERAGE_COUNT = 10;
    private static final int MAX_VOLTAGE_AVERAGE_COUNT = 10;
	private static final double RATIO_GW = 0.875;
    private ArrayList<String> xAxis = new ArrayList<>();
    private ArrayList<Float> currentAxis = new ArrayList<>();
    private ArrayList<Float> speedAxis = new ArrayList<>();

    private long mLastTimestamp;
    private double mWattHoursDischarge;
    private double mAmpHoursDischarge;
    private double mWattHoursRecharge;
    private double mAmpHoursRecharge;

    private int mBatteryLevelMin;
    private int mBatteryLevelMax;
    private double mPowerAvg;
    private long mPowerAvgCount;
    private double mPowerMin;
    private double mPowerMax;
    private double mTemperatureMin;
    private double mTemperatureMax;
    private double mCurrentAvg;
    private long mCurrentAvgCount;
    private double mCurrentMax;
    private double mCurrentMin;

    private double speedAvg;
    private long speedAvgCount;
    private double speedAvgRiding;
    private long speedAvgRidingCount;

    private int mSpeed;
    private long mTotalDistance;
    private int mCurrent;
    private double mAverageCurrent;
    private double mAverageCurrentCount;
    private int mTemperature;
	private int mTemperature2;
	private double mAngle;
	private double mRoll;

    private int mMode;
    private int mBattery;
    private double mAverageBattery;
    private double mAverageBatteryCount;
    private int mVoltage;
    private double mVoltageMin;
    private double mVoltageMax;
    private double mAverageVoltage;
    private double mAverageVoltageCount;
    private long mDistance;
	private long mUserDistance;
    private int mRideTime;
	private int mRidingTime;
    private int mLastRideTime;
    private int mTopSpeed;
    private int mFanStatus;
    private int mVoiceStatus;
    private int mLightStatus;
    private long mLastDataReceived = 0;
    private boolean mConnected = false;
	private boolean mNewWheelSettings = false;
    private String mName = "";
    private String mModel = "";
	private String mModeStr = "";
	private String mBtName = "";

	private String mAlert = "";

//    private int mVersion; # sorry King, but INT not good for Inmo
	private String mVersion = "";
    private String mSerialNumber = "";
    private WHEEL_TYPE mWheelType = WHEEL_TYPE.Unknown;
    private long rideStartTime;
    private long mStartTotalDistance;
	/// Wheel Settings
	private boolean mWheelLightEnabled = false;
	private boolean mWheelLedEnabled = false;
	private boolean mWheelButtonDisabled = false;
	private int mWheelMaxSpeed = 25;
	private int mWheelSpeakerVolume = 50;
	private int mWheelTiltHorizon = 0;
	
    private boolean mAlarmsEnabled = false;
    private boolean mVoltageAlarmEnabled = false;
    private boolean mDisablePhoneVibrate = false;
    private int mAlarm1Speed = 0;
    private int mAlarm2Speed = 0;
    private int mAlarm3Speed = 0;
    private int mAlarm1Battery = 0;
    private int mAlarm2Battery = 0;
    private int mAlarm3Battery = 0;
    private int mAlarmPeakCurrent = 0;
    private int mAlarmSustainedCurrent = 0;
	private int mAlarmTemperature = 0;
    private double alarmVoltage = 0;
    private int mGotwayVoltageScaler = 0;

	private boolean mUseRatio = false;
	//private boolean mGotway84V = false;
    private boolean mSpeed1stAlarmExecuted = false;
    private boolean mSpeed2ndAlarmExecuted = false;
    private boolean mSpeed3rdAlarmExecuted = false;
    private boolean mCurrentPeakAlarmExecuted = false;
    private boolean mCurrentSustainedAlarmExecuted = false;
	private boolean mTemperatureAlarmExecuted = false;
    private boolean voltageAlarmExecuted = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (Constants.ACTION_BLUETOOTH_CONNECTION_STATE.equals(action)) {
                int connectionState = intent.getIntExtra(Constants.INTENT_EXTRA_CONNECTION_STATE, BluetoothLeService.STATE_DISCONNECTED);
                switch (connectionState) {
                    case BluetoothLeService.STATE_CONNECTED:
                        break;
                    case BluetoothLeService.STATE_DISCONNECTED:
                        if (mConnected) {
                            mConnected = false;
                            mContext.sendBroadcast(new Intent(Constants.ACTION_WHEEL_DISCONNECTED));
                        }
                        break;
                    case BluetoothLeService.STATE_CONNECTING:
                        break;
                }
            }
        }
    };

    static void initiate(Context context) {
        if (mInstance == null) {
            mInstance = new WheelData();
            mInstance.init(context);
        }
        mInstance.full_reset(false);
    }

    public void destroy() {
        if (mInstance != null) {
            mContext.unregisterReceiver(receiver);
        }
    }

    private void init(Context context) {
        mContext = context;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE);
        mContext.registerReceiver(receiver, intentFilter);

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                long now = SystemClock.elapsedRealtime();
                if (!BluetoothLeService.isDisconnected() && now - mLastDataReceived >= Constants.WHEEL_DATA_VALIDITY) {
                    if (mConnected) {
                        mConnected = false;
                        mContext.sendBroadcast(new Intent(Constants.ACTION_WHEEL_CONNECTION_LOST));
                    }
                }
                if (mConnected && getSpeedDouble() >= Constants.MIN_RIDING_SPEED_EUC) {
                    mRidingTime += 1;
                }
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 1000);
    }

    public static WheelData getInstance() {
        return mInstance;
    }

    public long getDataAge() {
        return SystemClock.elapsedRealtime() - mLastDataReceived;
    }

    int getSpeed() {
        return (int)((mSpeed * SettingsUtil.getSpeedCorrectionFactor(mContext)) / 10);
    }
	
	boolean getWheelLight() {
        return mWheelLightEnabled;
    }
	
	boolean getWheelLed() {
        return mWheelLedEnabled;
    }
	
	boolean getWheelHandleButton() {
        return mWheelButtonDisabled;
    }
	
    int getWheelMaxSpeed() {
        return (int)(mWheelMaxSpeed * SettingsUtil.getSpeedCorrectionFactor(mContext));
    }
	
	int getSpeakerVolume() {
        return mWheelSpeakerVolume;
    }
	
	int getPedalsPosition() {
        return mWheelTiltHorizon;
    }

    public void setBtName(String btName) {
        mBtName = btName;
    }

    public void updateLight(boolean enabledLight) {
		if (mWheelLightEnabled != enabledLight) {
			mWheelLightEnabled = enabledLight;
			InMotionAdapter.getInstance().setLightState(enabledLight);
		}
    }
	
	public void updateLed(boolean enabledLed) {
		if (mWheelLedEnabled != enabledLed) {
			mWheelLedEnabled = enabledLed;
			InMotionAdapter.getInstance().setLedState(enabledLed);
		}
    }
	
	public void updatePedalsMode(int pedalsMode) {
		if (mWheelType == WHEEL_TYPE.GOTWAY) {
			switch (pedalsMode) {
				case 0:
					mBluetoothLeService.writeBluetoothGattCharacteristic("h".getBytes());
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
					break;
				case 1: 
					mBluetoothLeService.writeBluetoothGattCharacteristic("f".getBytes());
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
					break;
				case 2: 
					mBluetoothLeService.writeBluetoothGattCharacteristic("s".getBytes());
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
					break;	
			}			
		}
		
		if (mWheelType == WHEEL_TYPE.KINGSONG) {
            byte[] data = new byte[20];
            data[0] = (byte) 0xAA;
            data[1] = (byte) 0x55;
			data[2] = (byte) pedalsMode;
			data[3] = (byte) 0xE0;
            data[16] = (byte) 0x87;
            data[17] = (byte) 0x15;
            data[18] = (byte) 0x5A;
            data[19] = (byte) 0x5A;
            mBluetoothLeService.writeBluetoothGattCharacteristic(data);
		}
	
    }
	
	public void updateLightMode(int lightMode) {
		if (mWheelType == WHEEL_TYPE.GOTWAY) {
			switch (lightMode) {
				case 0:
					mBluetoothLeService.writeBluetoothGattCharacteristic("E".getBytes());   // 0x45
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());   // 0x62
					break;
				case 1: 
					mBluetoothLeService.writeBluetoothGattCharacteristic("Q".getBytes());   // 0x51
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());   // 0x62
					break;
				case 2: 
					mBluetoothLeService.writeBluetoothGattCharacteristic("T".getBytes());   // 0x54
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());   // 0x62
					break;	
			}			
		}
		
		if (mWheelType == WHEEL_TYPE.KINGSONG) {
            byte[] data = new byte[20];
            data[0] = (byte) 0xAA;
            data[1] = (byte) 0x55;
			data[2] = (byte) (lightMode + 0x12);
			data[3] = (byte) 0x01;
            data[16] = (byte) 0x73;
            data[17] = (byte) 0x14;
            data[18] = (byte) 0x5A;
            data[19] = (byte) 0x5A;
            mBluetoothLeService.writeBluetoothGattCharacteristic(data);
		}
	
    }

	public void updateStrobe(int strobeMode) {
		if (mWheelType == WHEEL_TYPE.KINGSONG) {
            byte[] data = new byte[20];
            data[0] = (byte) 0xAA;
            data[1] = (byte) 0x55;
			data[2] = (byte) strobeMode;
            data[16] = (byte) 0x53;
            data[17] = (byte) 0x14;
            data[18] = (byte) 0x5A;
            data[19] = (byte) 0x5A;
            mBluetoothLeService.writeBluetoothGattCharacteristic(data);
		}
		
    }
	
	public void updateLedMode(int ledMode) {
		if (mWheelType == WHEEL_TYPE.KINGSONG) {
            byte[] data = new byte[20];
            data[0] = (byte) 0xAA;
            data[1] = (byte) 0x55;
			data[2] = (byte) ledMode;
            data[16] = (byte) 0x6C;
            data[17] = (byte) 0x14;
            data[18] = (byte) 0x5A;
            data[19] = (byte) 0x5A;
            mBluetoothLeService.writeBluetoothGattCharacteristic(data);
		}
		
    }
	
	
	public void updateAlarmMode(int alarmMode) {
		if (mWheelType == WHEEL_TYPE.GOTWAY) {
			switch (alarmMode) {
				case 0:
					mBluetoothLeService.writeBluetoothGattCharacteristic("u".getBytes());   // 0x75
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());   // 0x62
					break;
				case 1: 
					mBluetoothLeService.writeBluetoothGattCharacteristic("i".getBytes());   // 0x69
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());   // 0x62
					break;
				case 2: 
					mBluetoothLeService.writeBluetoothGattCharacteristic("o".getBytes());   // 0x6f
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());   // 0x62
					break;	
			}			
		}
		
    }
	
	public void updateCalibration() {
		if (mWheelType == WHEEL_TYPE.GOTWAY) {
		    /*
			mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
			mBluetoothLeService.writeBluetoothGattCharacteristic("c".getBytes());
			mBluetoothLeService.writeBluetoothGattCharacteristic("y".getBytes());
			mBluetoothLeService.writeBluetoothGattCharacteristic("c".getBytes());
			mBluetoothLeService.writeBluetoothGattCharacteristic("y".getBytes());	
			mBluetoothLeService.writeBluetoothGattCharacteristic("c".getBytes());
			mBluetoothLeService.writeBluetoothGattCharacteristic("y".getBytes());
		     */
            mBluetoothLeService.writeBluetoothGattCharacteristic("c".getBytes());
            mBluetoothLeService.writeBluetoothGattCharacteristic("y".getBytes());
		}
    }


	public void updateHandleButton(boolean enabledButton) {
		if (mWheelButtonDisabled != enabledButton) {
			mWheelButtonDisabled = enabledButton;
			InMotionAdapter.getInstance().setHandleButtonState(enabledButton);
		}
    }

	public void updateMaxSpeed(int wheelMaxSpeed) {
		if (mWheelType == WHEEL_TYPE.INMOTION) {
			if (mWheelMaxSpeed != wheelMaxSpeed) {
				mWheelMaxSpeed = wheelMaxSpeed;
				InMotionAdapter.getInstance().setMaxSpeedState(wheelMaxSpeed);
			}
		}

		if (mWheelType == WHEEL_TYPE.GOTWAY) {
			byte[] data = new byte[1];
			if (wheelMaxSpeed != 0) {
				int wheelMaxSpeed2 = wheelMaxSpeed;
				if (mUseRatio) wheelMaxSpeed2 = (int)Math.round(wheelMaxSpeed2/RATIO_GW);
				mBluetoothLeService.writeBluetoothGattCharacteristic("W".getBytes());
				mBluetoothLeService.writeBluetoothGattCharacteristic("Y".getBytes());
				mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
				
				data[0] = (byte)((wheelMaxSpeed2/10)+0x30);
				mBluetoothLeService.writeBluetoothGattCharacteristic(data);
				mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
				data[0] = (byte)((wheelMaxSpeed2%10)+0x30);
				mBluetoothLeService.writeBluetoothGattCharacteristic(data);
				mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
				mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
			} else {
				data[0] = 0x22;
				mBluetoothLeService.writeBluetoothGattCharacteristic(data); // "
				mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());			
			}
		}
		if (mWheelType == WHEEL_TYPE.KINGSONG) {
            byte[] data = new byte[20];
            data[0] = (byte) 0xAA;
            data[1] = (byte) 0x55;
			data[6] = (byte) 0x1F;
			data[8] = (byte) wheelMaxSpeed;
            data[16] = (byte) 0x85;
            data[17] = (byte) 0x14;
            data[18] = (byte) 0x5A;
            data[19] = (byte) 0x5A;
            mBluetoothLeService.writeBluetoothGattCharacteristic(data);
		}
		
	}
	
	public void updateSpeakerVolume(int speakerVolume) {
        if (mWheelSpeakerVolume != speakerVolume) {
			mWheelSpeakerVolume = speakerVolume;
			InMotionAdapter.getInstance().setSpeakerVolumeState(speakerVolume);
		}
    }
	
	public void updatePedals(int pedalAdjustment) {
        if (mWheelTiltHorizon != pedalAdjustment) {
			mWheelTiltHorizon = pedalAdjustment;
			InMotionAdapter.getInstance().setTiltHorizon(pedalAdjustment);
		}
    }


    public int getBatteryLevelMax() { return mBatteryLevelMax == -999999 ? 0 : mBatteryLevelMax; }
    public int getBatteryLevelMin() { return mBatteryLevelMin == 999999 ? 0 : mBatteryLevelMin; }
    public double getTemperatureMinDouble() { return mTemperatureMin == 999999 ? 0 : mTemperatureMin; }
    public double getTemperatureMaxDouble() { return mTemperatureMax == -999999 ? 0 : mTemperatureMax; }
    public double getCurrentAvgDouble() { return mCurrentAvg; }
    public double getCurrentMaxDouble() { return mCurrentMax == -999999 ? 0 : mCurrentMax; }
    public double getCurrentMinDouble() { return mCurrentMin == 999999 ? 0 : mCurrentMin; }
    public double getVoltageMaxDouble() { return mVoltageMax == -999999 ? 0 : mVoltageMax; }
    public double getVoltageMinDouble() { return mVoltageMin == 999999 ? 0 : mVoltageMin; }
    public double getPowerAvgDouble() { return mPowerAvg; }
    public double getPowerMaxDouble() { return mPowerMax == -999999 ? 0 : mPowerMax; }
    public double getPowerMinDouble() { return mPowerMin == 999999 ? 0 : mPowerMin; }


    public boolean isLoadAvailable() {
        return (mAlarmPeakCurrent > 0 || mAlarmSustainedCurrent > 0);
    }
    public double getLoadDouble() {
        int alarm = (mAlarmPeakCurrent > mAlarmSustainedCurrent) ? mAlarmPeakCurrent : mAlarmSustainedCurrent;
        return (alarm > 0) ? (getCurrentDouble() * 10000) / alarm : 0;
    }

    public double getLoadMaxDouble() {
        int alarm = (mAlarmPeakCurrent > mAlarmSustainedCurrent) ? mAlarmPeakCurrent : mAlarmSustainedCurrent;
        return (alarm > 0) ? (mCurrentMax * 10000) / alarm : 0;
    }

    public double getLoadMaxRegenDouble() {
        int alarm = (mAlarmPeakCurrent > mAlarmSustainedCurrent) ? mAlarmPeakCurrent : mAlarmSustainedCurrent;
        return (alarm > 0) ? (Math.abs(mCurrentMin) * 10000) / alarm : 0;
    }

    public double getWattHours() { return mWattHoursDischarge - mWattHoursRecharge; }
    public double getAmpHours() { return mAmpHoursDischarge - mAmpHoursRecharge; }
    public double getWattHoursDischarge() { return mWattHoursDischarge; }
    public double getAmpHoursDischarge() { return mAmpHoursDischarge; }
    public double getWattHoursRecharge() { return mWattHoursRecharge; }
    public double getAmpHoursRecharge() { return mAmpHoursRecharge; }

    public int getTemperature() { return mTemperature / 100; }

    public double getTemperatureDouble() { return mTemperature / 100.0; }

    public int getTemperature2() {
        return mTemperature2 / 100;
    }

    public double getTemperature2Double() {
        return mTemperature2 / 100.0;
    }

	public double getAngle() {
        return mAngle;
    }
	
	public double getRoll() {
        return mRoll;
    }
	
    public int getBatteryLevel() {
        return mBattery;
    }

    public double getAverageBatteryLevelDouble() { return mAverageBattery; }

    int getLightStatus() { return mLightStatus; }
    void setLightStatus(int status) { mLightStatus = status; }
    int getVoiceStatus() { return mVoiceStatus; }
    int getFanStatus() { return mFanStatus; }

    boolean isConnected() {
        return mConnected;
    }

    //    int getTopSpeed() { return mTopSpeed; }
    String getVersion() {
        return mVersion;
    }

    //    int getCurrentTime() { return mCurrentTime+mLastCurrentTime; }
    int getMode() {
        return mMode;
    }

    WHEEL_TYPE getWheelType() {
        return mWheelType;
    }

    String getName() {
        return mName;
    }

    String getModel() {
        return mModel;
    }
	
	String getModeStr() {
        return mModeStr;
    }
	
	String getAlert() {
		String nAlert = mAlert;
		mAlert = "";
        return nAlert;
    }
	
    String getSerial() {
        return mSerialNumber;
    }

    int getRideTime() { return mRideTime; }

    int getRidingTime() { return mRidingTime; }

    double getAverageSpeedDouble() { return speedAvg; }
	
	double getAverageRidingSpeedDouble() { return speedAvgRiding; }
	
    String getRideTimeString() {
        int currentTime = mRideTime + mLastRideTime;
        long hours = TimeUnit.SECONDS.toHours(currentTime);
        long minutes = TimeUnit.SECONDS.toMinutes(currentTime) -
                TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(currentTime));
        long seconds = TimeUnit.SECONDS.toSeconds(currentTime) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(currentTime));
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

	String getRidingTimeString() {
        long hours = TimeUnit.SECONDS.toHours(mRidingTime);
        long minutes = TimeUnit.SECONDS.toMinutes(mRidingTime) -
                TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(mRidingTime));
        long seconds = TimeUnit.SECONDS.toSeconds(mRidingTime) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(mRidingTime));
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    double getSpeedDouble() { return (mSpeed * SettingsUtil.getSpeedCorrectionFactor(mContext)) / 100.0; }
    double getSpeedForTizen() { return (SettingsUtil.isUseMi(mBluetoothLeService.getApplicationContext())) ? getSpeedDouble() / 1.609 : getSpeedDouble(); }

    double getAverageVoltageDouble() { return mAverageVoltage; }
    double getVoltageDouble() { return mVoltage / 100.0; }

    double getAveragePowerDouble() {
        return mAverageCurrent * mAverageVoltage;
    }
    double getPowerDouble() {
        return (mCurrent * mVoltage) / 10000.0;
    }

    double getAverageCurrentDouble() {
        return mAverageCurrent;
    }
    int getAverageCurrent() { return (int)Math.round(mAverageCurrent * 100); }

    double getCurrentDouble() {
        return mCurrent / 100.0;
    }

    int getTopSpeed() { return (int)(mTopSpeed * SettingsUtil.getSpeedCorrectionFactor(mContext)); }

    double getTopSpeedDouble() { return (mTopSpeed * SettingsUtil.getSpeedCorrectionFactor(mContext)) / 100.0; }

    int getDistance() { return (int) (mTotalDistance - mStartTotalDistance); }

    String getDistanceForTizen() {
        double dist = (SettingsUtil.isUseMi(mBluetoothLeService.getApplicationContext())) ? getDistanceDouble() / 1.609 : getDistanceDouble();
        if (dist >= 10)
            return String.format(Locale.US, "%.1f", dist);
        else
        if (dist >= 1)
            return String.format(Locale.US, "%.2f", dist);
        else
            return String.format(Locale.US, "%.3f", dist);
    }

	long getWheelDistance() { 
		return (long)(mDistance * SettingsUtil.getDistCorrectionFactor(mContext));
	}

	public double getWheelDistanceDouble() {
        return (mDistance * SettingsUtil.getDistCorrectionFactor(mContext)) / 1000.0;
    }

	public double getUserDistanceDouble() {
		if (mUserDistance == 0 && mTotalDistance != 0 )  {
			mUserDistance = SettingsUtil.getUserDistance(mContext, mBluetoothLeService.getBluetoothDeviceAddress());
			if (mUserDistance == 0) {
				SettingsUtil.setUserDistance(mContext, mBluetoothLeService.getBluetoothDeviceAddress(),mTotalDistance);
				mUserDistance = mTotalDistance;
			}
		}
		return (mTotalDistance - mUserDistance)/1000.0; 
    }

    public void resetUserDistance() {
        if (mTotalDistance != 0) {
            SettingsUtil.setUserDistance(mContext, mBluetoothLeService.getBluetoothDeviceAddress(), mTotalDistance);
            mUserDistance = mTotalDistance;
        }
    }
	
	public void resetTopSpeed() {
		mTopSpeed = 0;
    }
	

    public double getDistanceDouble() {
        return ((mTotalDistance - mStartTotalDistance) * SettingsUtil.getDistCorrectionFactor(mContext)) / 1000.0;
    }

    double getTotalDistanceDouble() {
        return (mTotalDistance * SettingsUtil.getDistCorrectionFactor(mContext)) / 1000.0;
    }
	
	long getTotalDistance() {
        return (long)(mTotalDistance * SettingsUtil.getDistCorrectionFactor(mContext));
    }

    ArrayList<String> getXAxis() {
        return xAxis;
    }

    ArrayList<Float> getCurrentAxis() {
        return currentAxis;
    }

    ArrayList<Float> getSpeedAxis() {
        return speedAxis;
    }

    void setAlarmsEnabled(boolean enabled) {
        mAlarmsEnabled = enabled;
    }
	
	void setUseRatio(boolean enabled) {
        if (mUseRatio != enabled) {
            mUseRatio = enabled;
            reset();
        }
    }
	
	void setGotwayVoltage(int voltage) {
        mGotwayVoltageScaler = voltage;
    }

    void setPreferences(int alarm1Speed, int alarm1Battery,
                        int alarm2Speed, int alarm2Battery,
                        int alarm3Speed, int alarm3Battery,
                        int alarmPeakCurrent,
                        int alarmSustainedCurrent,
                        int alarmTemperature,
                        boolean enableVoltageAlarm,
                        boolean disablePhoneVibrate) {
        mAlarm1Speed = alarm1Speed * 100;
        mAlarm2Speed = alarm2Speed * 100;
        mAlarm3Speed = alarm3Speed * 100;
        mAlarm1Battery = alarm1Battery;
        mAlarm2Battery = alarm2Battery;
        mAlarm3Battery = alarm3Battery;
        mAlarmPeakCurrent = alarmPeakCurrent*100;
        mAlarmSustainedCurrent = alarmSustainedCurrent*100;
		mAlarmTemperature = alarmTemperature*100;
        mVoltageAlarmEnabled = enableVoltageAlarm;
        mDisablePhoneVibrate = disablePhoneVibrate;
    }

    private int byteArrayInt2(byte low, byte high) {
        return (low & 255) + ((high & 255) * 256);
    }

    private long byteArrayInt4(byte value1, byte value2, byte value3, byte value4) {
        return (((((long) ((value1 & 255) << 16))) | ((long) ((value2 & 255) << 24))) | ((long) (value3 & 255))) | ((long) ((value4 & 255) << 8));
    }

    private void setDistance(long distance) {
        if (mStartTotalDistance == 0 && mTotalDistance != 0)
            mStartTotalDistance = mTotalDistance;

        mDistance = distance;
        Data.vehicleDistance.setAsDistance(getDistanceDouble());
    }

    private void setCurrentTime(int currentTime) {
        if (mRideTime > (currentTime + TIME_BUFFER))
            mLastRideTime += mRideTime;
        mRideTime = currentTime;
        Data.vehicleDuration.setAsDate(getRideTime());
        Data.vehicleDurationRiding.setAsDate(getRidingTime());

    }

    private void setTopSpeed(int topSpeed) {
        if (topSpeed > mTopSpeed) {
            mTopSpeed = topSpeed;
        }
        Data.vehicleSpeedMax.setAsSpeed(getTopSpeedDouble());
    }

    private void setBatteryPercent(int battery) {
        mBattery = battery;
        mAverageBatteryCount = mAverageBatteryCount < MAX_BATTERY_AVERAGE_COUNT ? mAverageBatteryCount + 1 : MAX_BATTERY_AVERAGE_COUNT;
        mAverageBattery += (battery - mAverageBattery) / mAverageBatteryCount;
    }

    private void setAverageCurrent() {
        mAverageCurrentCount = mAverageCurrentCount < MAX_CURRENT_AVERAGE_COUNT ? mAverageCurrentCount + 1 : MAX_CURRENT_AVERAGE_COUNT;
        mAverageCurrent += (getCurrentDouble() - mAverageCurrent) / mAverageCurrentCount;
    }

    private void setAverageVoltage(double voltage) {
        mAverageVoltageCount = mAverageVoltageCount < MAX_VOLTAGE_AVERAGE_COUNT ? mAverageVoltageCount + 1 : MAX_VOLTAGE_AVERAGE_COUNT;
        mAverageVoltage += (voltage - mAverageVoltage) / mAverageVoltageCount;
    }

    private void setAverageSpeed() {
        speedAvg = ((speedAvg * speedAvgCount) + getSpeedDouble()) / ++speedAvgCount;
        if (getSpeedDouble() >= Constants.MIN_RIDING_SPEED_EUC)
            speedAvgRiding = ((speedAvgRiding * speedAvgRidingCount) + getSpeedDouble()) / ++speedAvgRidingCount;
    }

    boolean isSpeedAlarm1Active() { return mConnected && mAlarmsEnabled && mSpeed1stAlarmExecuted; }

    boolean isSpeedAlarm2Active() { return mConnected && mAlarmsEnabled && mSpeed2ndAlarmExecuted; }

    boolean isSpeedAlarm3Active() { return mConnected && mAlarmsEnabled && mSpeed3rdAlarmExecuted; }

    boolean isVoltageAlarmActive() { return mConnected && mAlarmsEnabled && voltageAlarmExecuted; }

    boolean isPeakCurrentAlarmActive() { return mConnected && mAlarmsEnabled && mCurrentPeakAlarmExecuted; }

    boolean isSustainedCurrentAlarmActive() { return mConnected && mAlarmsEnabled && mCurrentSustainedAlarmExecuted; }

    boolean isTemperatureAlarmActive() { return mConnected && mAlarmsEnabled && mTemperatureAlarmExecuted; }

    private void checkAlarmStatus() {
        boolean vdaDisabled = !SettingsUtil.getVoltageDependentAlarms(mContext);

        if (!mSpeed1stAlarmExecuted) {
            if (mAlarmsEnabled &&  mAlarm1Speed > 0 && ((mAlarm1Battery > 0 && mAverageBattery <= mAlarm1Battery) || vdaDisabled) && mSpeed >= mAlarm1Speed)
                raiseAlarm(ALARM_TYPE.SPEED_1ST);
        }
        else {
            if (!mAlarmsEnabled || mAlarm1Speed == 0 || (mAlarm1Battery == 0 && !vdaDisabled) || (mAverageBattery > mAlarm1Battery && !vdaDisabled) || mSpeed < mAlarm1Speed)
                ceaseAlarm(ALARM_TYPE.SPEED_1ST);
        }

        if (!mSpeed2ndAlarmExecuted) {
            if (!mSpeed1stAlarmExecuted && mAlarmsEnabled &&  mAlarm2Speed > 0 && ((mAlarm2Battery > 0 && mAverageBattery <= mAlarm2Battery) || vdaDisabled) && mSpeed >= mAlarm2Speed)
                raiseAlarm(ALARM_TYPE.SPEED_2ND);
        }
        else {
            if (mSpeed1stAlarmExecuted || !mAlarmsEnabled || mAlarm2Speed == 0 || (mAlarm2Battery == 0 && !vdaDisabled) || (mAverageBattery > mAlarm2Battery && !vdaDisabled) || mSpeed < mAlarm2Speed)
                ceaseAlarm(ALARM_TYPE.SPEED_2ND);
        }

        if (!mSpeed3rdAlarmExecuted) {
            if (!mSpeed1stAlarmExecuted && !mSpeed2ndAlarmExecuted && mAlarmsEnabled &&  mAlarm3Speed > 0 && ((mAlarm3Battery > 0 && mAverageBattery <= mAlarm3Battery) || vdaDisabled) && mSpeed >= mAlarm3Speed)
                raiseAlarm(ALARM_TYPE.SPEED_3RD);
        }
        else {
            if (mSpeed1stAlarmExecuted || mSpeed2ndAlarmExecuted || !mAlarmsEnabled || mAlarm3Speed == 0 || (mAlarm3Battery == 0 && !vdaDisabled) || (mAverageBattery > mAlarm3Battery && !vdaDisabled) || mSpeed < mAlarm3Speed)
                ceaseAlarm(ALARM_TYPE.SPEED_3RD);
        }

        if (!mCurrentPeakAlarmExecuted) {
            if (mAlarmsEnabled &&  mAlarmPeakCurrent > 0 && mCurrent >= mAlarmPeakCurrent)
                raiseAlarm(ALARM_TYPE.CURRENT_PEAK);
        }
        else {
            if (!mAlarmsEnabled || mAlarmPeakCurrent == 0 || mCurrent < mAlarmPeakCurrent)
                ceaseAlarm(ALARM_TYPE.CURRENT_PEAK);
        }

        if (!mCurrentSustainedAlarmExecuted) {
            if (mAlarmsEnabled &&  mAlarmSustainedCurrent > 0 && getAverageCurrent() >= mAlarmSustainedCurrent)
                raiseAlarm(ALARM_TYPE.CURRENT_SUSTAINED);
        }
        else {
            if (!mAlarmsEnabled || mAlarmSustainedCurrent == 0 || getAverageCurrent() < mAlarmSustainedCurrent)
                ceaseAlarm(ALARM_TYPE.CURRENT_SUSTAINED);
        }

		if (!mTemperatureAlarmExecuted) {
            if (mAlarmsEnabled &&  mAlarmTemperature > 0 && mTemperature >= mAlarmTemperature)
                raiseAlarm(ALARM_TYPE.TEMPERATURE);
        }
		else {
            if (!mAlarmsEnabled || mAlarmTemperature == 0 || mTemperature < mAlarmTemperature)
                ceaseAlarm(ALARM_TYPE.TEMPERATURE);
        }

        if (!voltageAlarmExecuted) {
            if (mAlarmsEnabled &&  alarmVoltage > 0 && getVoltageDouble() >= alarmVoltage)
                raiseAlarm(ALARM_TYPE.VOLTAGE);
        }
        else {
            if (!mAlarmsEnabled || alarmVoltage == 0 || getVoltageDouble() < alarmVoltage)
                ceaseAlarm(ALARM_TYPE.VOLTAGE);
        }

    }

    private void raiseAlarm(ALARM_TYPE alarmType) {
        long[] pattern = {0};
        switch (alarmType) {
            case SPEED_1ST:
                pattern = new long[]{0, 300};
                mSpeed1stAlarmExecuted = true;
                break;
            case SPEED_2ND:
                pattern = new long[]{0, 300, 200, 300};
                mSpeed2ndAlarmExecuted = true;
                break;
            case SPEED_3RD:
                pattern = new long[]{0, 300, 200, 300, 200, 600};
                mSpeed3rdAlarmExecuted = true;
                break;
            case CURRENT_PEAK:
                pattern = new long[]{0, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100};
                mCurrentPeakAlarmExecuted = true;
                break;
            case CURRENT_SUSTAINED:
                pattern = new long[]{0, 200, 100, 200, 100, 200, 100, 200, 100, 200, 100, 200};
                mCurrentSustainedAlarmExecuted = true;
                break;
			case TEMPERATURE:
                pattern = new long[]{0, 500, 100, 100, 100, 500, 100, 100, 100, 500, 100, 100};
                mTemperatureAlarmExecuted = true;
                break;
            case VOLTAGE:
                pattern = new long[]{0, 300, 300, 250, 250, 200, 200, 150, 150, 100};
                voltageAlarmExecuted = true;
                break;
        }
        Intent intent = new Intent(Constants.ACTION_ALARM_TRIGGERED);
        intent.putExtra(Constants.INTENT_EXTRA_ALARM_TYPE, alarmType);
        mContext.sendBroadcast(intent);

        Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (v.hasVibrator() && !mDisablePhoneVibrate)
            v.vibrate(pattern, -1);
    }

    private void ceaseAlarm(ALARM_TYPE alarmType) {
        switch (alarmType) {
            case SPEED_1ST:
                mSpeed1stAlarmExecuted = false;
                break;
            case SPEED_2ND:
                mSpeed2ndAlarmExecuted = false;
                break;
            case SPEED_3RD:
                mSpeed3rdAlarmExecuted = false;
                break;
            case CURRENT_PEAK:
                mCurrentPeakAlarmExecuted = false;
                break;
            case CURRENT_SUSTAINED:
                mCurrentSustainedAlarmExecuted = false;
                break;
            case TEMPERATURE:
                mTemperatureAlarmExecuted = false;
                break;
            case VOLTAGE:
                voltageAlarmExecuted = false;
                break;
        }
        Intent intent = new Intent(Constants.ACTION_ALARM_FINISHED);
        intent.putExtra(Constants.INTENT_EXTRA_ALARM_TYPE, alarmType);
        mContext.sendBroadcast(intent);
    }

    void decodeResponse(byte[] data) {

        StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte aData : data)
            stringBuilder.append(String.format(Locale.US, "%02X", aData));
        //Timber.i("Received: " + stringBuilder.toString());
//        FileUtil.writeLine("bluetoothOutput.txt", stringBuilder.toString());

        boolean new_data = false;
        if (mWheelType == WHEEL_TYPE.KINGSONG)
            new_data = decodeKingSong(data);
        else if (mWheelType == WHEEL_TYPE.GOTWAY)
            new_data = decodeGotway(data);
        else if (mWheelType == WHEEL_TYPE.INMOTION)
            new_data = decodeInmotion(data);
        else if (mWheelType == WHEEL_TYPE.NINEBOT)
            new_data = decodeNinebot(data);
        else if (mWheelType == WHEEL_TYPE.NINEBOT_Z)
            new_data = decodeNinebotZ(data);

        if (!new_data)
			return;

		Intent intent = new Intent(Constants.ACTION_WHEEL_DATA_AVAILABLE);
		
		if (mNewWheelSettings) {
			intent.putExtra(Constants.INTENT_EXTRA_WHEEL_SETTINGS, true);
			mNewWheelSettings = false;
		}
		
        if (graph_last_update_time + GRAPH_UPDATE_INTERVAL < Calendar.getInstance().getTimeInMillis()) {
            graph_last_update_time = Calendar.getInstance().getTimeInMillis();
            intent.putExtra(Constants.INTENT_EXTRA_GRAPH_UPDATE_AVILABLE, true);
            currentAxis.add((float) getCurrentDouble());
            speedAxis.add((float) getSpeedDouble());
            xAxis.add(new SimpleDateFormat("HH:mm:ss", Locale.US).format(Calendar.getInstance().getTime()));
            if (speedAxis.size() > (3600000 / GRAPH_UPDATE_INTERVAL)) {
                speedAxis.remove(0);
                currentAxis.remove(0);
                xAxis.remove(0);
            }
        }

        checkAlarmStatus();

        mLastDataReceived = SystemClock.elapsedRealtime();
        if (!mConnected && BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_CONNECTED) {
            mConnected = true;
            mContext.sendBroadcast(new Intent(Constants.ACTION_WHEEL_CONNECTED));
        }
        mContext.sendBroadcast(intent);
    }

    private boolean decodeKingSong(byte[] data) {

        if (rideStartTime == 0) {
            rideStartTime = Calendar.getInstance().getTimeInMillis();
			mRidingTime = 0;
		}
        if (data.length >= 20) {
            int a1 = data[0] & 255;
            int a2 = data[1] & 255;
            if (a1 != 170 || a2 != 85) {
                return false;
            }
            if ((data[16] & 255) == 169) { // Live data

                //Log.d("RX< ", bytesToHex(data));

                mCurrent = ((data[10]&0xFF) + (data[11]<<8));
                setAverageCurrent();
                setMinMaxCurrent();

                mVoltage = byteArrayInt2(data[2], data[3]);
                setAverageVoltage(getVoltageDouble());
                setMinMaxVoltage();
                setMinMaxAvgPower();

                mSpeed = byteArrayInt2(data[4], data[5]);
                setAverageSpeed();
                Data.vehicleSpeed.setAsSpeed(getSpeedDouble());
                Data.vehicleSpeedAvg.setAsSpeed(getAverageSpeedDouble());
                Data.vehicleSpeedAvgRiding.setAsSpeed(getAverageRidingSpeedDouble());

                mTotalDistance = byteArrayInt4(data[6], data[7], data[8], data[9]);
                Data.vehicleDistanceTotal.setAsDistance(getTotalDistanceDouble());
                Data.vehicleDistanceUser.setAsDistance(getUserDistanceDouble());
                Data.vehicleDistanceVehicle.setAsDistance(getWheelDistanceDouble());

				mTemperature = byteArrayInt2(data[12], data[13]);
                setMinMaxTemperature();

                if ((data[15] & 255) == 224) {
                    mMode = data[14];
					mModeStr = String.format(Locale.US, "%d", mMode);
					Data.vehicleControlSensitivity.setAsString(mModeStr);
                }

                int battery;

                if ((mModel.compareTo("KS-18L") == 0) || (mModel.compareTo("KS-16X") == 0)) {
                    if (mLastTimestamp > 0) {
                        double dt = (double)(System.currentTimeMillis() - mLastTimestamp) / (3600 * 1000);
                        double dah = getCurrentDouble() * dt;
                        double dwh = getVoltageDouble() * getCurrentDouble() * dt;
                        if (dah < 0) {
                            mAmpHoursRecharge += Math.abs(dah);
                            mWattHoursRecharge += Math.abs(dwh);
                        }
                        else {
                            mAmpHoursDischarge += dah;
                            mWattHoursDischarge += dwh;
                        }
                    }
                    mLastTimestamp = System.currentTimeMillis();
                }

                if (!mModel.isEmpty()) {
                    if (mModel.compareTo("KS-16X") == 0) {
                        battery = BatteryGauge.KS16X(getVoltageDouble(), SettingsUtil.getOptimizedBatteryLevel(mContext));
                        alarmVoltage = mVoltageAlarmEnabled ? 85f : 0;
                    }
                    else if (mModel.compareTo("KS-18L") == 0) {
                        battery = BatteryGauge.KS18L(getVoltageDouble(), SettingsUtil.getOptimizedBatteryLevel(mContext));
                        alarmVoltage = mVoltageAlarmEnabled ? 85f : 0;
                    }
                    else if ((mBtName.compareTo("RW") == 0) || (mName.startsWith("ROCKW"))) {
                        battery = BatteryGauge.Generic84V(getVoltageDouble(), SettingsUtil.getOptimizedBatteryLevel(mContext));
                        alarmVoltage = mVoltageAlarmEnabled ? 85f : 0;
                    }
                    else {
                        battery = BatteryGauge.Generic67V(getVoltageDouble(), SettingsUtil.getOptimizedBatteryLevel(mContext));
                        alarmVoltage = mVoltageAlarmEnabled ? 68f : 0;
                    }
                    setBatteryPercent(battery);
                    setMinMaxBattery();
                }

                return true;
            }
            else if ((data[16] & 255) == 185) { // Distance/Time/Fan Data/Motor Temperature
                long distance = byteArrayInt4(data[2], data[3], data[4], data[5]);
                setDistance(distance);
                //int currentTime = byteArrayInt2(data[6], data[7]);
	            int currentTime = (int) (Calendar.getInstance().getTimeInMillis() - rideStartTime) / 1000;
                setCurrentTime(currentTime);
                setTopSpeed(byteArrayInt2(data[8], data[9]));
                mLightStatus = data[10];
                mVoiceStatus = data[11];
                mFanStatus = data[12];
                Data.vehicleCoolingFanStatus.setAsString(getFanStatus() == 0 ? mContext.getString(R.string.fan_off) : mContext.getString(R.string.fan_on));

                if ((mModel.compareTo("KS-18L") == 0) || (mModel.compareTo("KS-16X") == 0)) {
                    mTemperature2 = byteArrayInt2(data[14], data[15]);
                    Data.vehicleTemperatureMotor.setAsTemperature(getTemperature2Double());
                }
                Data.vehicleSerialNumber.touch();
                Data.vehicleFirmwareVersion.touch();
                Data.vehicleName.touch();
                Data.vehicleModel.touch();
            }
            else if ((data[16] & 255) == 187) { // Name and Type data
                int end = 0;
                int i = 0;
                while (i < 14 && data[i + 2] != 0) {
                    end++;
                    i++;
                }
                mName = new String(data, 2, end).trim();
                Data.vehicleName.setAsString(mName);
                mModel = "";
                String[] ss = mName.split("-");
                for (i = 0; i < ss.length - 1; i++) {
                    if (i != 0) {
                        mModel += "-";
                    }
                    mModel += ss[i];
                }
                Data.vehicleModel.setAsString(mModel);
                try {
                    mVersion = String.format(Locale.US, "%.2f", ((double)(Integer.parseInt(ss[ss.length - 1])/100.0)));
                    Data.vehicleFirmwareVersion.setAsString(mVersion);
                }
                catch (Exception ignored) { }

            }
            else if ((data[16] & 255) == 179) { // Serial Number
                byte[] sndata = new byte[18];
                System.arraycopy(data, 2, sndata, 0, 14);
                System.arraycopy(data, 17, sndata, 14, 3);
                sndata[17] = (byte) 0;
                mSerialNumber = new String(sndata);
                Data.vehicleSerialNumber.setAsString(mSerialNumber);
            }
        }
        return false;
    }

    private boolean decodeGotway(byte[] data) {
        if (rideStartTime == 0) {
            rideStartTime = Calendar.getInstance().getTimeInMillis();
			mRidingTime = 0;
		}
        if (data.length >= 20 && data[0] == 85 && data[1] == -86 && data[18] == 0) {
            if (data[5] >= 0)
                mSpeed = (int) Math.abs(((data[4] * 256.0) + data[5]) * 3.6);
            else
                mSpeed = (int) Math.abs((((data[4] * 256.0) + 256.0) + data[5]) * 3.6);

			if (mUseRatio) mSpeed = (int)Math.round(mSpeed * RATIO_GW);
            setAverageSpeed();
            setTopSpeed(mSpeed);
            Data.vehicleSpeed.setAsSpeed(getSpeedDouble());
            Data.vehicleSpeedAvg.setAsSpeed(getAverageSpeedDouble());
            Data.vehicleSpeedAvgRiding.setAsSpeed(getAverageRidingSpeedDouble());

            mTemperature = (int) Math.round(((((data[12] * 256) + data[13]) / 340.0) + 35) * 100);
            setMinMaxTemperature();

            mTemperature2 = mTemperature;

            long distance = byteArrayInt2(data[9], data[8]);
			if (mUseRatio) distance = Math.round(distance * RATIO_GW);
            setDistance(distance);

            mVoltage = (data[2] * 256) + (data[3] & 255);

            mCurrent = Math.abs((short) ((data[10] << 8) | (data[11] & 0xff)));

            setAverageCurrent();
            setMinMaxCurrent();

            int battery;

//            if (mVoltage > 6680) {
//                battery = 100;
//            } else if (mVoltage > 5440) {
//                battery = (mVoltage - 5380) / 13;
//            } else if (mVoltage > 5290){
//                battery = (int)Math.round((mVoltage - 5290) / 32.5);
//            } else {
//                battery = 0;
//            }
            if (mVoltage <= 5290) {
                battery = 0;
            } else if (mVoltage >= 6580) {
                battery = 100;
            } else {
                battery = (mVoltage - 5290) / 13;
            }
            setBatteryPercent(battery);
            setMinMaxBattery();

//			if (mGotway84V) {
//				mVoltage = (int)Math.round(mVoltage / 0.8);
//			}
            mVoltage = mVoltage + (int)Math.round(mVoltage*0.25*mGotwayVoltageScaler);
            alarmVoltage = mVoltageAlarmEnabled ? 68 + (17 * mGotwayVoltageScaler) : 0;
            setAverageVoltage(getVoltageDouble());
            setMinMaxVoltage();
            setMinMaxAvgPower();

            int currentTime = (int) (Calendar.getInstance().getTimeInMillis() - rideStartTime) / 1000;
            setCurrentTime(currentTime);

            return true;
        }
        else
        if (data.length >= 10 && data[0] == 90 && data[4] == 85 && data[5] == -86) {

            mTotalDistance = ((data[6]&0xFF) <<24) + ((data[7]&0xFF) << 16) + ((data[8] & 0xFF) <<8) + (data[9] & 0xFF);

			if (mUseRatio) mTotalDistance = Math.round(mTotalDistance * RATIO_GW);
            Data.vehicleDistanceTotal.setAsDistance(getTotalDistanceDouble());
            Data.vehicleDistanceUser.setAsDistance(getUserDistanceDouble());
            Data.vehicleDistanceVehicle.setAsDistance(getWheelDistanceDouble());

            return true;
        }
        return false;
    }

    private boolean decodeNinebot(byte[] data) {
        ArrayList<NinebotAdapter.Status> statuses = NinebotAdapter.getInstance().charUpdated(data);
        if (statuses.size() < 1) return false;
        if (rideStartTime == 0) {
            rideStartTime = Calendar.getInstance().getTimeInMillis();
            mRidingTime = 0;
        }
        for (NinebotAdapter.Status status: statuses) {
            Timber.i(status.toString());
            if (status instanceof NinebotAdapter.serialNumberStatus) {
                mSerialNumber = ((NinebotAdapter.serialNumberStatus) status).getSerialNumber();
                Data.vehicleSerialNumber.setAsString(mSerialNumber);
                mModel = "Ninebot";
                Data.vehicleModel.setAsString(mModel);
            }
            else
            if (status instanceof NinebotAdapter.versionStatus){
                mVersion = ((NinebotAdapter.versionStatus) status).getVersion();
                Data.vehicleFirmwareVersion.setAsString(mVersion);
            }
            else {
                mSpeed = status.getSpeed();
                setAverageSpeed();
                Data.vehicleSpeed.setAsSpeed(getSpeedDouble());
                Data.vehicleSpeedAvg.setAsSpeed(getAverageSpeedDouble());
                Data.vehicleSpeedAvgRiding.setAsSpeed(getAverageRidingSpeedDouble());

                mVoltage = status.getVoltage();
                alarmVoltage = 0;
                setAverageVoltage(getVoltageDouble());
                setMinMaxVoltage();

                setBatteryPercent(status.getBatt());
                setMinMaxBattery();

                mCurrent = status.getCurrent();
                setAverageCurrent();
                setMinMaxCurrent();
                setMinMaxAvgPower();

                mTotalDistance = (long) (status.getDistance());
                Data.vehicleDistanceTotal.setAsDistance(getTotalDistanceDouble());
                Data.vehicleDistanceUser.setAsDistance(getUserDistanceDouble());
                Data.vehicleDistanceVehicle.setAsDistance(getWheelDistanceDouble());

                mTemperature = status.getTemperature() * 10;
                setMinMaxTemperature();

                setDistance(status.getDistance());
                int currentTime = (int) (Calendar.getInstance().getTimeInMillis() - rideStartTime) / 1000;
                setCurrentTime(currentTime);
                setTopSpeed(mSpeed);
            }
        }
        return true;
    }

    private boolean decodeNinebotZ(byte[] data) {
        ArrayList<NinebotZAdapter.Status> statuses = NinebotZAdapter.getInstance().charUpdated(data);
        if (statuses.size() < 1) return false;
        if (rideStartTime == 0) {
            rideStartTime = Calendar.getInstance().getTimeInMillis();
            mRidingTime = 0;
        }
        for (NinebotZAdapter.Status status: statuses) {
            Timber.i(status.toString());
            if (status instanceof NinebotZAdapter.serialNumberStatus) {
                mSerialNumber = ((NinebotZAdapter.serialNumberStatus) status).getSerialNumber();
                Data.vehicleSerialNumber.setAsString(mSerialNumber);
                mModel = ((NinebotZAdapter.serialNumberStatus) status).getModel();
                Data.vehicleModel.setAsString(mModel);
            }
            else
            if (status instanceof NinebotZAdapter.versionStatus){
                mVersion = ((NinebotZAdapter.versionStatus) status).getVersion();
                Data.vehicleFirmwareVersion.setAsString(mVersion);
            }
            else {
                mSpeed = status.getSpeed();
                setAverageSpeed();
                Data.vehicleSpeed.setAsSpeed(getSpeedDouble());
                Data.vehicleSpeedAvg.setAsSpeed(getAverageSpeedDouble());
                Data.vehicleSpeedAvgRiding.setAsSpeed(getAverageRidingSpeedDouble());

                mVoltage = status.getVoltage();
                alarmVoltage = 0;
                setAverageVoltage(getVoltageDouble());
                setMinMaxVoltage();

                mCurrent = status.getCurrent();
                setAverageCurrent();
                setMinMaxCurrent();
                setMinMaxAvgPower();

                mTotalDistance = (long) (status.getDistance());
                Data.vehicleDistanceTotal.setAsDistance(getTotalDistanceDouble());
                Data.vehicleDistanceUser.setAsDistance(getUserDistanceDouble());
                Data.vehicleDistanceVehicle.setAsDistance(getWheelDistanceDouble());

                mTemperature = status.getTemperature() * 10;
                setMinMaxTemperature();

                setBatteryPercent(status.getBatt());
                setMinMaxBattery();

                setDistance((long) status.getDistance());

                int currentTime = (int) (Calendar.getInstance().getTimeInMillis() - rideStartTime) / 1000;
                setCurrentTime(currentTime);
                setTopSpeed(mSpeed);
            }
        }
        return true;
    }

    private boolean decodeInmotion(byte[] data) {
        ArrayList<InMotionAdapter.Status> statuses = InMotionAdapter.getInstance().charUpdated(data);
		if (statuses.size() < 1) return false;
        if (rideStartTime == 0) {
            rideStartTime = Calendar.getInstance().getTimeInMillis();
			mRidingTime = 0;
		}		
        for (InMotionAdapter.Status status: statuses) {
            Timber.i(status.toString());
            if (status instanceof InMotionAdapter.Infos) {
				mWheelLightEnabled = ((InMotionAdapter.Infos) status).getLightState();
				mWheelLedEnabled = ((InMotionAdapter.Infos) status).getLedState();
				mWheelButtonDisabled = ((InMotionAdapter.Infos) status).getHandleButtonState();
				mWheelMaxSpeed = ((InMotionAdapter.Infos) status).getMaxSpeedState();
				mWheelSpeakerVolume = ((InMotionAdapter.Infos) status).getSpeakerVolumeState();
				mWheelTiltHorizon = ((InMotionAdapter.Infos) status).getTiltHorizon(); 
                mSerialNumber = ((InMotionAdapter.Infos) status).getSerialNumber();
                Data.vehicleSerialNumber.setAsString(mSerialNumber);
                mModel = ((InMotionAdapter.Infos) status).getModelString();
                Data.vehicleModel.setAsString(mModel);
                mVersion = ((InMotionAdapter.Infos) status).getVersion();
                Data.vehicleFirmwareVersion.setAsString(mVersion);
				mNewWheelSettings = true;
            } else if (status instanceof InMotionAdapter.Alert){
				if (mAlert == "") {
					mAlert = ((InMotionAdapter.Alert) status).getfullText();
				} else {
					mAlert = mAlert + " | " + ((InMotionAdapter.Alert) status).getfullText();
				}
			} else {
                mSpeed = (int) (status.getSpeed() * 360d);
                setAverageSpeed();
                Data.vehicleSpeed.setAsSpeed(getSpeedDouble());
                Data.vehicleSpeedAvg.setAsSpeed(getAverageSpeedDouble());
                Data.vehicleSpeedAvgRiding.setAsSpeed(getAverageRidingSpeedDouble());
                mVoltage = (int) (status.getVoltage() * 100d);
                alarmVoltage = mVoltageAlarmEnabled ? 85f : 0;
                setAverageVoltage(getVoltageDouble());
                setMinMaxVoltage();
                mCurrent = (int) (status.getCurrent() * 100d);
                setAverageCurrent();
                setMinMaxCurrent();
                setMinMaxAvgPower();
                mTemperature = (int) (status.getTemperature() * 100d);
				mTemperature2 = (int) (status.getTemperature2() * 100d);
				setMinMaxTemperature();
                mTotalDistance = (long) (status.getDistance()*1000d);
                Data.vehicleDistanceTotal.setAsDistance(getTotalDistanceDouble());
                Data.vehicleDistanceUser.setAsDistance(getUserDistanceDouble());
                Data.vehicleDistanceVehicle.setAsDistance(getWheelDistanceDouble());
				mAngle = (double) (status.getAngle());
                Data.vehicleTilt.setAsDouble(getAngle());
				mRoll = (double) (status.getRoll());
                Data.vehicleRoll.setAsDouble(getRoll());

				mModeStr = status.getWorkModeString();
                setBatteryPercent((int) status.getBatt());
                setMinMaxBattery();
                setDistance((long) status.getDistance());
				
                int currentTime = (int) (Calendar.getInstance().getTimeInMillis() - rideStartTime) / 1000;
                setCurrentTime(currentTime);
                setTopSpeed(mSpeed);
            }
        }
        return true;
    }

    private void setMinMaxTemperature() {
        if (mTemperatureMax < getTemperatureDouble()) mTemperatureMax = getTemperatureDouble();
        if (mTemperatureMin > getTemperatureDouble()) mTemperatureMin = getTemperatureDouble();

        Data.vehicleTemperature.setAsTemperature(getTemperatureDouble());
        Data.vehicleTemperatureMin.setAsTemperature(getTemperatureMinDouble());
        Data.vehicleTemperatureMax.setAsTemperature(getTemperatureMaxDouble());
    }

    private void setMinMaxAvgPower() {
        if (mPowerMax < getPowerDouble()) mPowerMax = getPowerDouble();
        if (mPowerMin > getPowerDouble()) mPowerMin = getPowerDouble();
        mPowerAvg = ((mPowerAvg * mPowerAvgCount) + getPowerDouble()) / ++mPowerAvgCount;

        Data.vehiclePower.setAsDouble(getPowerDouble());
        Data.vehiclePowerMin.setAsDouble(getPowerMinDouble());
        Data.vehiclePowerAvg.setAsDouble(getPowerAvgDouble());
        Data.vehiclePowerMax.setAsDouble(getPowerMaxDouble());
    }

    private void setMinMaxCurrent() {
        if (mCurrentMax < getCurrentDouble()) mCurrentMax = getCurrentDouble();
        if (mCurrentMin > getCurrentDouble()) mCurrentMin = getCurrentDouble();
        mCurrentAvg = ((mCurrentAvg * mCurrentAvgCount) + getCurrentDouble()) / ++mCurrentAvgCount;

        Data.vehicleCurrent.setAsDouble(getCurrentDouble());
        Data.vehicleCurrentMin.setAsDouble(getCurrentMinDouble());
        Data.vehicleCurrentAvg.setAsDouble(getCurrentAvgDouble());
        Data.vehicleCurrentMax.setAsDouble(getCurrentMaxDouble());

        if (isLoadAvailable()) {
            Data.vehicleLoad.setAsDouble(getLoadDouble());
            Data.vehicleLoadMax.setAsDouble(getLoadMaxDouble());
            Data.vehicleLoadMaxRegen.setAsDouble(getLoadMaxRegenDouble());
        }
    }

    private void setMinMaxVoltage() {
        if (mVoltageMax < getVoltageDouble()) mVoltageMax = getVoltageDouble();
        if (mVoltageMin > getVoltageDouble()) mVoltageMin = getVoltageDouble();

        Data.vehicleVoltage.setAsDouble(getVoltageDouble());
        Data.vehicleVoltageMin.setAsDouble(getVoltageMinDouble());
        Data.vehicleVoltageMax.setAsDouble(getVoltageMaxDouble());
    }

    private void setMinMaxBattery() {
        if (mBatteryLevelMax < getBatteryLevel()) mBatteryLevelMax = getBatteryLevel();
        if (mBatteryLevelMin > getBatteryLevel()) mBatteryLevelMin = getBatteryLevel();

        Data.vehicleBatteryLevel.setAsDouble(getBatteryLevel());
        Data.vehicleBatteryLevelFiltered.setAsDouble(getAverageBatteryLevelDouble());
        Data.vehicleBatteryLevelMin.setAsDouble(getBatteryLevelMin());
        Data.vehicleBatteryLevelMax.setAsDouble(getBatteryLevelMax());
    }

    void full_reset(boolean keepData) {
        if (mWheelType == WHEEL_TYPE.INMOTION) InMotionAdapter.getInstance().stopTimer();
        if (mWheelType == WHEEL_TYPE.NINEBOT) NinebotAdapter.getInstance().stopTimer();
        if (mWheelType == WHEEL_TYPE.NINEBOT_Z) NinebotZAdapter.getInstance().stopTimer();
        mBluetoothLeService = null;
        mWheelType = WHEEL_TYPE.Unknown;
        xAxis.clear();
        speedAxis.clear();
        currentAxis.clear();
        if (!keepData)
            reset();
    }

    private void reset() {
        mLastTimestamp = 0;
        alarmVoltage = 0;

        mTemperatureMax = -999999;
        mTemperatureMin = 999999;
        mBatteryLevelMax = -999999;
        mBatteryLevelMin = 999999;
        mCurrentAvg = 0;
        mCurrentAvgCount = 0;
        mCurrentMax = -999999;
        mCurrentMin = 999999;
        mVoltageMax = -999999;
        mVoltageMin = 999999;
        mPowerAvg = 0;
        mPowerAvgCount = 0;
        mPowerMax = -999999;
        mPowerMin = 999999;

        speedAvg = 0;
        speedAvgCount = 0;
        speedAvgRiding = 0;
        speedAvgRidingCount = 0;

        mWattHoursDischarge = 0;
        mAmpHoursDischarge = 0;
        mWattHoursRecharge = 0;
        mAmpHoursRecharge = 0;
        mSpeed = 0;
        mTotalDistance = 0;
        mCurrent = 0;
        mAverageCurrentCount = 0;
        mAverageCurrent = 0;
        mTemperature = 0;
		mTemperature2 = 0;
		mAngle = 0;
		mRoll = 0;
        mMode = 0;
        mBattery = 0;
        mAverageBatteryCount = 0;
        mAverageBattery = 0;
        mVoltage = 0;
        mAverageVoltageCount = 0;
        mAverageVoltage = 0;
        mRideTime = 0;
		mRidingTime = 0;
        mTopSpeed = 0;
        mFanStatus = 0;
        mVoiceStatus = 0;
        mLightStatus = 0;
		mDistance = 0;
		mUserDistance = 0;
        mName = "";
        mModel = "";
		mModeStr = "";
        mVersion = "";
        mSerialNumber = "";
        mBtName = "";
        rideStartTime = 0;
        mStartTotalDistance = 0;
		mWheelTiltHorizon = 0;
		mWheelLightEnabled = false;
		mWheelLedEnabled = false;
		mWheelButtonDisabled = false;
		mWheelMaxSpeed = 25;
		mWheelSpeakerVolume = 50;
    }

    boolean detectWheel(BluetoothLeService bluetoothService) {
        mBluetoothLeService = bluetoothService;

        Class<R.array> res = R.array.class;
        String wheel_types[] = mContext.getResources().getStringArray(R.array.wheel_types);
        for (String wheel_Type : wheel_types) {
            boolean detected_wheel = true;
            java.lang.reflect.Field services_res = null;
            try {
                services_res = res.getField(wheel_Type + "_services");
            } catch (Exception ignored) {
            }
            int services_res_id = 0;
            if (services_res != null)
                try {
                    services_res_id = services_res.getInt(null);
                } catch (Exception ignored) {
                }

            String services[] = mContext.getResources().getStringArray(services_res_id);

            if (services.length != mBluetoothLeService.getSupportedGattServices().size())
                continue;

            for (String service_uuid : services) {
                UUID s_uuid = UUID.fromString(service_uuid.replace("_", "-"));
                BluetoothGattService service = mBluetoothLeService.getGattService(s_uuid);
                if (service != null) {
                    java.lang.reflect.Field characteristic_res = null;
                    try {
                        characteristic_res = res.getField(wheel_Type + "_" + service_uuid);
                    } catch (Exception ignored) {
                    }
                    int characteristic_res_id = 0;
                    if (characteristic_res != null)
                        try {
                            characteristic_res_id = characteristic_res.getInt(null);
                        } catch (Exception ignored) {
                        }
                    String characteristics[] = mContext.getResources().getStringArray(characteristic_res_id);
                    for (String characteristic_uuid : characteristics) {
                        UUID c_uuid = UUID.fromString(characteristic_uuid.replace("_", "-"));
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(c_uuid);
                        if (characteristic == null) {
                            detected_wheel = false;
                            break;
                        }
                    }
                } else {
                    detected_wheel = false;
                    break;
                }
            }

            if (detected_wheel) {
				final Intent intent = new Intent(Constants.ACTION_WHEEL_TYPE_RECOGNIZED); // update preferences
                intent.putExtra(Constants.INTENT_EXTRA_WHEEL_TYPE, wheel_Type);
				mContext.sendBroadcast(intent);
				Timber.i("Protocol recognized as %s", wheel_Type);
				//System.out.println("WheelRecognizedWD");
                if (mContext.getResources().getString(R.string.gotway).equals(wheel_Type) && (mBtName.equals("RW") || mName.startsWith("ROCKW"))) {
                    Timber.i("It seems to be RockWheel, force to Kingsong proto");
                    wheel_Type = mContext.getResources().getString(R.string.kingsong);
                }
                if (mContext.getResources().getString(R.string.kingsong).equals(wheel_Type)) {
                    mWheelType = WHEEL_TYPE.KINGSONG;
                    BluetoothGattService targetService = mBluetoothLeService.getGattService(UUID.fromString(Constants.KINGSONG_SERVICE_UUID));
                    BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.KINGSONG_READ_CHARACTER_UUID));
                    mBluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                    BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString(Constants.KINGSONG_DESCRIPTER_UUID));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothLeService.writeBluetoothGattDescriptor(descriptor);
					
                    return true;
                } else if (mContext.getResources().getString(R.string.gotway).equals(wheel_Type)) {
                    mWheelType = WHEEL_TYPE.GOTWAY;
                    BluetoothGattService targetService = mBluetoothLeService.getGattService(UUID.fromString(Constants.GOTWAY_SERVICE_UUID));
                    BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.GOTWAY_READ_CHARACTER_UUID));
                    mBluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                    // Let the user know it's working by making the wheel beep
                    mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
                    return true;
                } else if (mContext.getResources().getString(R.string.inmotion).equals(wheel_Type)) {
                    mWheelType = WHEEL_TYPE.INMOTION;
                    BluetoothGattService targetService = mBluetoothLeService.getGattService(UUID.fromString(Constants.INMOTION_SERVICE_UUID));
                    BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.INMOTION_READ_CHARACTER_UUID));
                    mBluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                    BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString(Constants.INMOTION_DESCRIPTER_UUID));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothLeService.writeBluetoothGattDescriptor(descriptor);
                    if (SettingsUtil.hasPasswordForWheel(mContext, mBluetoothLeService.getBluetoothDeviceAddress())) {
                        String inmotionPassword = SettingsUtil.getPasswordForWheel(mBluetoothLeService.getApplicationContext(), mBluetoothLeService.getBluetoothDeviceAddress());
                        InMotionAdapter.getInstance().startKeepAliveTimer(mBluetoothLeService, inmotionPassword);
                        return true;
                    }
                    return false;
                } else if (mContext.getResources().getString(R.string.ninebot).equals(wheel_Type)) {
                    /*
                        TODO: Need to buy or rent a 9B wheel to add & test support for Ninebot protocol
                    */
                    return false;
                } else if (mContext.getResources().getString(R.string.ninebot_z).equals(wheel_Type)) {
                    Timber.i("Trying to start Ninebot Z/One S2");
                    mWheelType = WHEEL_TYPE.NINEBOT_Z;
                    BluetoothGattService targetService = mBluetoothLeService.getGattService(UUID.fromString(Constants.NINEBOT_Z_SERVICE_UUID));
                    Timber.i("service UUID");
                    BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.NINEBOT_Z_READ_CHARACTER_UUID));
                    Timber.i("read UUID");
                    if (notifyCharacteristic == null) {
                        Timber.i("it seems that RX UUID doesn't exist");
                    }
                    mBluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                    Timber.i("notify UUID");
                    BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString(Constants.NINEBOT_Z_DESCRIPTER_UUID));
                    Timber.i("descr UUID");
                    if (descriptor == null) {
                        Timber.i("it seems that descr UUID doesn't exist");
                    }
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    Timber.i("enable notify UUID");
                    mBluetoothLeService.writeBluetoothGattDescriptor(descriptor);
                    Timber.i("write notify");
                    NinebotZAdapter.getInstance().startKeepAliveTimer(mBluetoothLeService,"");
                    Timber.i("starting ninebot z/one s2 adapter");
                    return true;
                }
                else if (mContext.getResources().getString(R.string.ninebot).equals(wheel_Type)) {
                    Timber.i("Trying to start Ninebot");
                    mWheelType = WHEEL_TYPE.NINEBOT;
                    BluetoothGattService targetService = mBluetoothLeService.getGattService(UUID.fromString(Constants.NINEBOT_SERVICE_UUID));
                    Timber.i("service UUID");
                    BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.NINEBOT_READ_CHARACTER_UUID));
                    Timber.i("read UUID");
                    if (notifyCharacteristic == null) {
                        Timber.i("it seems that RX UUID doesn't exist");
                    }
                    mBluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                    Timber.i("notify UUID");
                    BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString(Constants.NINEBOT_DESCRIPTER_UUID));
                    Timber.i("descr UUID");
                    if (descriptor == null) {
                        Timber.i("it seems that descr UUID doesn't exist");
                    }
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    Timber.i("enable notify UUID");
                    mBluetoothLeService.writeBluetoothGattDescriptor(descriptor);
                    Timber.i("write notify");
                    NinebotAdapter.getInstance().startKeepAliveTimer(mBluetoothLeService,"");
                    Timber.i("starting ninebot adapter");
                    return true;
                }
            }
        }
        return false;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x ", b));
        }
        return sb.toString();
    }
}
