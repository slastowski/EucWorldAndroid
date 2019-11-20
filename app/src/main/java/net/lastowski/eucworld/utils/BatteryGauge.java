package net.lastowski.eucworld.utils;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class BatteryGauge {

    // King Song KS-16X

    private static final double[] KS16X_Voltage_Std = {63.0, 65.0, 66.0, 68.0, 69.0, 71.0, 73.0, 74.0, 76.0, 77.0, 79.0, 81.0, 83.0};
    private static final double[] KS16X_Percent_Std = { 0.0, 10.0, 21.0, 31.0, 40.0, 49.0, 55.0, 63.0, 71.0, 79.0, 86.0, 93.0, 100.0};

    private static final double[] KS16X_Voltage_Opt = {64.0, 70.0, 83.0};
    private static final double[] KS16X_Percent_Opt = { 0.0, 10.0, 100.0};

    // King Song KS-18L

    private static final double[] KS18L_Voltage_Std = {61.0, 83.0};
    private static final double[] KS18L_Percent_Std = { 0.0, 100.0};

    private static final double[] KS18L_Voltage_Opt = {64.0, 70.0, 83.0};
    private static final double[] KS18L_Percent_Opt = { 0.0, 10.0, 100.0};

    // Gotway

    private static final double[] Gotway_Voltage_Std = {52.9, 65.8};
    private static final double[] Gotway_Percent_Std = { 0.0, 100.0};

    private static final double[] Gotway_Voltage_Opt = {54.0, 56.0, 66.0};
    private static final double[] Gotway_Percent_Opt = { 0.0, 10.0, 100.0};

    // Generic 84V (20S)

    private static final double[] Generic84V_Voltage_Std = {60.0, 83.0};
    private static final double[] Generic84V_Percent_Std = { 0.0, 100.0};

    private static final double[] Generic84V_Voltage_Opt = {63.0, 83.0};
    private static final double[] Generic84V_Percent_Opt = { 0.0, 100.0};

    // Generic 67V (16S)

    private static final double[] Generic67V_Voltage_Std = {50.0, 66.0};
    private static final double[] Generic67V_Percent_Std = { 0.0, 100.0};

    private static final double[] Generic67V_Voltage_Opt = {53.0, 66.0};
    private static final double[] Generic67V_Percent_Opt = { 0.0, 100.0};

    public static int KS16X(double voltage, boolean optimized) {
        double[] v = optimized ? KS16X_Voltage_Opt : KS16X_Voltage_Std;
        double[] p = optimized ? KS16X_Percent_Opt : KS16X_Percent_Std;
        return calc_1d(v, p, voltage);
    }

    public static int KS18L(double voltage, boolean optimized) {
        double[] v = optimized ? KS18L_Voltage_Opt : KS18L_Voltage_Std;
        double[] p = optimized ? KS18L_Percent_Opt : KS18L_Percent_Std;
        return calc_1d(v, p, voltage);
    }

    public static int Generic84V(double voltage, boolean optimized) {
        double[] v = optimized ? Generic84V_Voltage_Opt : Generic84V_Voltage_Std;
        double[] p = optimized ? Generic84V_Percent_Opt : Generic84V_Percent_Std;
        return calc_1d(v, p, voltage);
    }

    public static int Generic67V(double voltage, boolean optimized) {
        double[] v = optimized ? Generic67V_Voltage_Opt : Generic67V_Voltage_Std;
        double[] p = optimized ? Generic67V_Percent_Opt : Generic67V_Percent_Std;
        return calc_1d(v, p, voltage);
    }

    private static int calc_1d(double[] v, double[] p, double voltage) {
        if (voltage < v[0]) return (int)Math.round(p[0]);
        if (voltage > v[v.length - 1]) return (int)Math.round(p[p.length - 1]);
        LinearInterpolator li = new LinearInterpolator();
        PolynomialSplineFunction psf = li.interpolate(v, p);
        return (int)Math.round(psf.value(voltage));
    }

}
