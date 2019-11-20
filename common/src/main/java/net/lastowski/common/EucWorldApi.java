package net.lastowski.common;

public class EucWorldApi {

    public interface WKI {

        public interface GPS {

            String Altitude =                               "gal";

            String Bearing =                                "gbe";

            String Distance =                               "gdi";

            String Duration =                               "gua";
            String Duration_Riding =                        "gur";

            String Heading =                                "ghe";

            String Latitude =                               "gla";
            String Longitude =                              "glo";

            String Speed =                                  "gsp";
            String Speed_Avg =                              "gsa";
            String Speed_Avg_Riding =                       "gsr";
            String Speed_Max =                              "gsx";

        }

        public interface Phone {

            String Battery_Level =                          "pba";

        }

        public interface Watch {

            String Battery_Level =                          "wba";

        }

        public interface Vehicle {

            String Battery_Level =                          "vba";
            String Battery_Level_Filtered =                 "vbf";
            String Battery_Level_Min =                      "vbm";
            String Battery_Level_Max =                      "vbx";

            String Control_Sensitivity =                    "vmcs";
            String Cooling_Fan_Status =                     "vmcf";

            String Current =                                "vcu";
            String Current_Filtered =                       "vcf";
            String Current_Min =                            "vcn";
            String Current_Avg =                            "vca";
            String Current_Max =                            "vcx";

            String Distance =                               "vdi";
            String Distance_User =                          "vdu";
            String Distance_Vehicle =                       "vdv";
            String Distance_Total =                         "vdt";

            String Duration =                               "vua";
            String Duration_Riding =                        "vur";
            String Duration_User =                          "vuu";
            String Duration_Total =                         "vut";

            String Firmware_Version =                       "vmfv";

            String Load =                                   "vlo";
            String Load_Filtered =                          "vlf";
            String Load_Max_Regen =                         "vln";
            String Load_Max =                               "vlx";

            String Make =                                   "vmma";
            String Model =                                  "vmmo";
            String Name =                                   "vmna";

            String Power =                                  "vpo";
            String Power_Filtered =                         "vpf";
            String Power_Min =                              "vpn";
            String Power_Avg =                              "vpa";
            String Power_Max =                              "vpx";

            String Roll =                                   "vro";

            String Serial_number =                          "vmsn";

            String Speed =                                  "vsp";
            String Speed_Avg =                              "vsa";
            String Speed_Avg_Riding =                       "vsr";
            String Speed_Max =                              "vsx";

            String Temperature =                            "vte";
            String Temperature_Min =                        "vtn";
            String Temperature_Max =                        "vtx";

            String Temperature_Battery =                    "vteb";
            String Temperature_Battery_Min =                "vtnb";
            String Temperature_Battery_Max =                "vtxb";

            String Temperature_Controller =                 "vtec";
            String Temperature_Controller_Min =             "vtnc";
            String Temperature_Controller_Max =             "vtxc";

            String Temperature_Motor =                      "vtem";
            String Temperature_Motor_Min =                  "vtnm";
            String Temperature_Motor_Max =                  "vtxm";

            String Tilt =                                   "vil";

            String Voltage =                                "vvo";
            String Voltage_Filtered =                       "vvf";
            String Voltage_Min =                            "vvn";
            String Voltage_Avg =                            "vva";
            String Voltage_Max =                            "vvx";

        }

    }

}
