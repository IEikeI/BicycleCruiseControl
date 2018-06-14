package de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.Model;

/**
 * Model that holds the current status of the bicycle driver (simulated).
 */
public class BicycleDriver {

    /**
     * A fixed threshold for a heart rate warning
     */
    private final double criticalHeartRate = 0.00; //TODO adjust me

    /**
     * Usage for identifying the drivers corresponding group
     */
    private int groupId; //only use this if we need id

    /**
     * Name of the driver
     */
    private String name; //only use this if we need it

    /**
     * A number for sequencing the group members (bicycle drivers)
     */
    private int driverId; //only use this if we need id

    /**
     * Current value of drivers heart rate (simulated)
     */
    private double heartRate;

    /**
     * Current value of drivers speed (simulated)
     */
    private double speed;

    public BicycleDriver(int groupId, double heartRate, double speed) {
        this.groupId = groupId;
        this.heartRate = heartRate;
        this.speed = speed;
    }

    public BicycleDriver(int groupId, String name, int driverId, double heartRate, double speed) {
        this.groupId = groupId;
        this.name = name;
        this.driverId = driverId;
        this.heartRate = heartRate;
        this.speed = speed;
    }

    /**
     * Returns true if the current heart rate is above the set threshold
     * @return
     */
    public boolean isHeartRateCritikal(){
        if (this.heartRate > criticalHeartRate){
            return true;
        } else {
            return false;
        }
    }

    public double getCriticalHeartRate() {
        return criticalHeartRate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDriverId() {
        return driverId;
    }

    public void setDriverId(int id) {
        this.driverId = id;
    }

    public double getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(double heartRate) {
        this.heartRate = heartRate;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }
}
