package de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.models;

/**
 * Interface for defined Tags used in the BLE-data protocol
 */
public interface BLEProtocolTags {

    //use '#' als general Tag key command
    //'_' as separator between tags
    //'$' as separator between tags and actual data

    //DEBUG
    public static final String PING = "#ping_";
    public static final String ECHO = "#echo_";

    //SENDING-DATA
    public static final String SEND_GROUP_ID = "#send_grpid$";
    public static final String SEND_DRIVER_ID = "#send_id$";
    public static final String SEND_NAME = "#send_name$";
    public static final String SEND_SPEED = "#send_speed$";
    public static final String SEND_HEARTRATE = "#semd_hrt$";
    public static final String SEND_HEARTRATE_WARN = "#send_hrtwrn$";

    //RETRIEVING-DATA
    public static final String RET_GROUP_ID = "#ret_grpid$";
    public static final String RET_DRIVER_ID = "#ret_id$";
    public static final String RET_NAME = "#ret_name$";
    public static final String RET_SPEED = "#ret_speed$";
    public static final String RET_HEARTRATE = "#ret_hrt$";
    public static final String RET_HEARTRATE_WARN = "#ret_hrtwrn$";

}
