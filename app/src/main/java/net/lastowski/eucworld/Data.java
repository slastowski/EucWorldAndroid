package net.lastowski.eucworld;

import android.content.Context;

import net.lastowski.common.EucWorldApi;
import net.lastowski.common.Value;

import java.util.ArrayList;

public class Data {

    public static final Value vehicleBatteryLevelFiltered   = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Battery_Level_Filtered).setFormat("%.0f\u2009%%");
    public static final Value vehicleBatteryLevel           = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Battery_Level).setFormat("%.0f\u2009%%");
    public static final Value vehicleBatteryLevelMin        = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Battery_Level_Min).setFormat("%.0f\u2009%%");
    public static final Value vehicleBatteryLevelMax        = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Battery_Level_Max).setFormat("%.0f\u2009%%");
    public static final Value vehicleCurrent                = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Current).setFormat("%.1f\u2009A");
    public static final Value vehicleCurrentMin             = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Current_Min).setFormat("%.1f\u2009A");
    public static final Value vehicleCurrentAvg             = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Current_Avg).setFormat("%.1f\u2009A");
    public static final Value vehicleCurrentMax             = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Current_Max).setFormat("%.1f\u2009A");
    public static final Value vehicleDistance               = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Distance).setFormat("%.2f").setSuffixImperial("\u2009mi").setSuffixMetric("\u2009km");
    public static final Value vehicleDistanceTotal          = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Distance_Vehicle).setFormat("%.2f").setSuffixImperial("\u2009mi").setSuffixMetric("\u2009km");
    public static final Value vehicleDistanceUser           = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Distance_User).setFormat("%.2f").setSuffixImperial("\u2009mi").setSuffixMetric("\u2009km");
    public static final Value vehicleDistanceVehicle        = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Distance_Vehicle).setFormat("%.2f").setSuffixImperial("\u2009mi").setSuffixMetric("\u2009km");
    public static final Value vehicleDuration               = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Duration).setFormat("HH:mm:ss").setMultiplier(1000).setTimeZone("UTC");
    public static final Value vehicleDurationRiding         = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Duration_Riding).setFormat("HH:mm:ss").setMultiplier(1000).setTimeZone("UTC");
    public static final Value vehicleLoad                   = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Load).setFormat("%.0f\u2009%%");
    public static final Value vehicleLoadMax                = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Load_Max).setFormat("%.0f\u2009%%");
    public static final Value vehicleLoadMaxRegen           = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Load_Max).setFormat("%.0f\u2009%%");
    public static final Value vehiclePower                  = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Power).setFormat("%.0f\u2009W");
    public static final Value vehiclePowerMin               = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Power_Min).setFormat("%.0f\u2009W");
    public static final Value vehiclePowerAvg               = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Power_Avg).setFormat("%.0f\u2009W");
    public static final Value vehiclePowerMax               = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Power_Max).setFormat("%.0f\u2009W");
    public static final Value vehicleSpeed                  = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Speed).setFormat("%.1f").setSuffixImperial("\u2009mph").setSuffixMetric("\u2009km/h");
    public static final Value vehicleSpeedMax               = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Speed_Max).setFormat("%.1f").setSuffixImperial("\u2009mph").setSuffixMetric("\u2009km/h");
    public static final Value vehicleSpeedAvg               = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Speed_Avg).setFormat("%.1f").setSuffixImperial("\u2009mph").setSuffixMetric("\u2009km/h");
    public static final Value vehicleSpeedAvgRiding         = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Speed_Avg_Riding).setFormat("%.1f").setSuffixImperial("\u2009mph").setSuffixMetric("\u2009km/h");
    public static final Value vehicleTemperature            = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Temperature).setFormat("%.0f").setSuffixImperial("\u2009°F").setSuffixMetric("\u2009°C");
    public static final Value vehicleTemperatureMin         = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Temperature_Min).setFormat("%.0f").setSuffixImperial("\u2009°F").setSuffixMetric("\u2009°C");
    public static final Value vehicleTemperatureMax         = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Temperature_Max).setFormat("%.0f").setSuffixImperial("\u2009°F").setSuffixMetric("\u2009°C");
    public static final Value vehicleTemperatureBattery     = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Temperature_Battery).setFormat("%.0f").setSuffixImperial("\u2009°F").setSuffixMetric("\u2009°C");
    public static final Value vehicleTemperatureMotor       = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Temperature_Motor).setFormat("%.0f").setSuffixImperial("\u2009°F").setSuffixMetric("\u2009°C");
    public static final Value vehicleVoltage                = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Voltage).setFormat("%.1f\u2009V");
    public static final Value vehicleVoltageMin             = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Voltage_Min).setFormat("%.1f\u2009V");
    public static final Value vehicleVoltageMax             = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Voltage_Max).setFormat("%.1f\u2009V");
    public static final Value vehicleTilt                   = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Tilt).setFormat("%.1f\u2009°");
    public static final Value vehicleRoll                   = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Roll).setFormat("%.1f\u2009°");
    public static final Value vehicleCoolingFanStatus       = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Cooling_Fan_Status).setFormat("%s");
    public static final Value vehicleControlSensitivity     = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Control_Sensitivity).setFormat("%s");
    public static final Value vehicleName                   = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Name).setFormat("%s");
    public static final Value vehicleModel                  = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Model).setFormat("%s");
    public static final Value vehicleFirmwareVersion        = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Firmware_Version).setFormat("%s");
    public static final Value vehicleSerialNumber           = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.Vehicle.Serial_number).setFormat("%s");

    public static final Value tourAltitude                  = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.GPS.Altitude).setFormat("%.0f").setSuffixImperial("\u2009ft").setSuffixMetric("\u2009m");
    public static final Value tourBearing                   = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.GPS.Bearing).setFormat("%.0f\u2009°");
    public static final Value tourDistance                  = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.GPS.Distance).setFormat("%.2f").setSuffixImperial("\u2009mi").setSuffixMetric("\u2009km");
    public static final Value tourDuration                  = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.GPS.Duration).setFormat("HH:mm:ss").setMultiplier(1000).setTimeZone("UTC");
    public static final Value tourDurationRiding            = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.GPS.Duration_Riding).setFormat("HH:mm:ss").setMultiplier(1000).setTimeZone("UTC");
    public static final Value tourSpeed                     = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.GPS.Speed).setFormat("%.1f").setSuffixImperial("\u2009mph").setSuffixMetric("\u2009km/h");
    public static final Value tourSpeedMax                  = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.GPS.Speed_Max).setFormat("%.1f").setSuffixImperial("\u2009mph").setSuffixMetric("\u2009km/h");
    public static final Value tourSpeedAvg                  = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.GPS.Speed_Avg).setFormat("%.1f").setSuffixImperial("\u2009mph").setSuffixMetric("\u2009km/h");
    public static final Value tourSpeedAvgRiding            = new Value(Value.DEFAULT_VALIDITY).setWKI(EucWorldApi.WKI.GPS.Speed_Avg_Riding).setFormat("%.1f").setSuffixImperial("\u2009mph").setSuffixMetric("\u2009km/h");

    public static final Value phoneBatteryLevel             = new Value(30000).setWKI(EucWorldApi.WKI.Phone.Battery_Level).setFormat("%.0f\u2009%%");
    public static final Value watchBatteryLevel             = new Value(30000).setWKI(EucWorldApi.WKI.Watch.Battery_Level).setFormat("%.0f\u2009%%");

    private static ArrayList<Value> vehicle = new ArrayList<>();
    private static ArrayList<Value> tour = new ArrayList<>();

    public static void initialize(Context context) {
        vehicle.add(vehicleBatteryLevelFiltered.setTitle(context.getResources().getString(R.string.battery)));
        vehicle.add(vehicleBatteryLevel.setTitle(context.getResources().getString(R.string.battery)));
        vehicle.add(vehicleBatteryLevelMin.setTitle(context.getResources().getString(R.string.battery_min)));
        vehicle.add(vehicleBatteryLevelMax.setTitle(context.getResources().getString(R.string.battery_max)));
        vehicle.add(vehicleCurrent.setTitle(context.getResources().getString(R.string.current)));
        vehicle.add(vehicleCurrentMin.setTitle(context.getResources().getString(R.string.current_min)));
        vehicle.add(vehicleCurrentAvg.setTitle(context.getResources().getString(R.string.current_avg)));
        vehicle.add(vehicleCurrentMax.setTitle(context.getResources().getString(R.string.current_max)));
        vehicle.add(vehicleDistance.setTitle(context.getResources().getString(R.string.distance)));
        vehicle.add(vehicleDistanceTotal.setTitle(context.getResources().getString(R.string.total_distance)));
        vehicle.add(vehicleDistanceUser.setTitle(context.getResources().getString(R.string.user_distance)));
        vehicle.add(vehicleDistanceVehicle.setTitle(context.getResources().getString(R.string.wheel_distance)));
        vehicle.add(vehicleDuration.setTitle(context.getResources().getString(R.string.ride_time)));
        vehicle.add(vehicleDurationRiding.setTitle(context.getResources().getString(R.string.riding_time)));
        vehicle.add(vehicleLoad.setTitle(context.getResources().getString(R.string.load)));
        vehicle.add(vehicleLoadMax.setTitle(context.getResources().getString(R.string.load_max)));
        vehicle.add(vehicleLoadMaxRegen.setTitle(context.getResources().getString(R.string.load_max_regen)));
        vehicle.add(vehiclePower.setTitle(context.getResources().getString(R.string.power)));
        vehicle.add(vehiclePowerMin.setTitle(context.getResources().getString(R.string.power_min)));
        vehicle.add(vehiclePowerAvg.setTitle(context.getResources().getString(R.string.power_avg)));
        vehicle.add(vehiclePowerMax.setTitle(context.getResources().getString(R.string.power_max)));
        vehicle.add(vehicleSpeed.setTitle(context.getResources().getString(R.string.speed)));
        vehicle.add(vehicleSpeedMax.setTitle(context.getResources().getString(R.string.top_speed)));
        vehicle.add(vehicleSpeedAvg.setTitle(context.getResources().getString(R.string.average_speed)));
        vehicle.add(vehicleSpeedAvgRiding.setTitle(context.getResources().getString(R.string.average_riding_speed)));
        vehicle.add(vehicleTemperature.setTitle(context.getResources().getString(R.string.temperature)));
        vehicle.add(vehicleTemperatureBattery.setTitle(context.getResources().getString(R.string.temperature_battery)));
        vehicle.add(vehicleTemperatureMotor.setTitle(context.getResources().getString(R.string.temperature_motor)));
        vehicle.add(vehicleTemperatureMin.setTitle(context.getResources().getString(R.string.temperature_min)));
        vehicle.add(vehicleTemperatureMax.setTitle(context.getResources().getString(R.string.temperature_max)));
        vehicle.add(vehicleVoltage.setTitle(context.getResources().getString(R.string.voltage)));
        vehicle.add(vehicleVoltageMin.setTitle(context.getResources().getString(R.string.voltage_min)));
        vehicle.add(vehicleVoltageMax.setTitle(context.getResources().getString(R.string.voltage_max)));
        vehicle.add(vehicleTilt.setTitle(context.getResources().getString(R.string.tilt)));
        vehicle.add(vehicleRoll.setTitle(context.getResources().getString(R.string.roll)));
        vehicle.add(vehicleCoolingFanStatus.setTitle(context.getResources().getString(R.string.fan_status)));
        vehicle.add(vehicleControlSensitivity.setTitle(context.getResources().getString(R.string.mode)));
        vehicle.add(vehicleName.setTitle(context.getResources().getString(R.string.name)));
        vehicle.add(vehicleModel.setTitle(context.getResources().getString(R.string.model)));
        vehicle.add(vehicleFirmwareVersion.setTitle(context.getResources().getString(R.string.version)));
        vehicle.add(vehicleSerialNumber.setTitle(context.getResources().getString(R.string.serial_number)));

        tour.add(tourAltitude.setTitle(context.getResources().getString(R.string.altitude)));
        tour.add(tourBearing.setTitle(context.getResources().getString(R.string.bearing)));
        tour.add(tourDistance.setTitle(context.getResources().getString(R.string.distance)));
        tour.add(tourDuration.setTitle(context.getResources().getString(R.string.ride_time)));
        tour.add(tourDurationRiding.setTitle(context.getResources().getString(R.string.riding_time)));
        tour.add(tourSpeed.setTitle(context.getResources().getString(R.string.speed)));
        tour.add(tourSpeedMax.setTitle(context.getResources().getString(R.string.top_speed)));
        tour.add(tourSpeedAvg.setTitle(context.getResources().getString(R.string.average_speed)));
        tour.add(tourSpeedAvgRiding.setTitle(context.getResources().getString(R.string.average_riding_speed)));
    }

    public static void setUseFt(boolean useFt) {
        tourAltitude.setImperial(useFt);
    }

    public static void setUseF(boolean useF) {
        vehicleTemperature.setImperial(useF);
        vehicleTemperatureBattery.setImperial(useF);
        vehicleTemperatureMotor.setImperial(useF);
        vehicleTemperatureMin.setImperial(useF);
        vehicleTemperatureMax.setImperial(useF);
    }

    public static void setUseMi(boolean useMi) {
        vehicleDistance.setImperial(useMi);
        vehicleDistanceTotal.setImperial(useMi);
        vehicleDistanceUser.setImperial(useMi);
        vehicleDistanceVehicle.setImperial(useMi);
        vehicleSpeed.setImperial(useMi);
        vehicleSpeedMax.setImperial(useMi);
        vehicleSpeedAvg.setImperial(useMi);
        vehicleSpeedAvgRiding.setImperial(useMi);

        tourDistance.setImperial(useMi);
        tourSpeed.setImperial(useMi);
        tourSpeedMax.setImperial(useMi);
        tourSpeedAvg.setImperial(useMi);
        tourSpeedAvgRiding.setImperial(useMi);
    }

    public static void invalidateVehicleValues() { for (Value v : vehicle) v.invalidate(); }
    public static void refreshVehicleValues() { for (Value v : vehicle) v.invalidateViews(); }
    public static void invalidateTourValues() { for (Value v : tour) v.invalidate(); }
    public static void refreshTourValues() { for (Value v : tour) v.invalidateViews(); }

}
