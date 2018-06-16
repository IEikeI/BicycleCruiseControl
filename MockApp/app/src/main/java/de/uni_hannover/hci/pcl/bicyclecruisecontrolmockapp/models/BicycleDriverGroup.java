package de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.models;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Model that holds a drivers corresponding group with all its members
 */
public class BicycleDriverGroup implements Serializable {

    private int groupId;
    private ArrayList<BicycleDriver> bicycleDrivers;

    public BicycleDriverGroup(int groupId, ArrayList<BicycleDriver> bicycleDrivers) {
        this.groupId = groupId;
        this.bicycleDrivers = bicycleDrivers;
    }

    public BicycleDriverGroup(int groupId) {
        this.groupId = groupId;
    }

    /**
     * Automatically init a new list if there isn´t one already
     * @param bicycleDriver
     */
    public void addToDriverGroup(BicycleDriver bicycleDriver){
        if(bicycleDrivers == null){
            bicycleDrivers = new ArrayList<BicycleDriver>();
        }
        bicycleDrivers.add(bicycleDriver);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BicycleDriverGroup that = (BicycleDriverGroup) o;

        if (groupId != that.groupId) return false;
        return bicycleDrivers != null ? bicycleDrivers.equals(that.bicycleDrivers) : that.bicycleDrivers == null;
    }

    @Override
    public int hashCode() {
        int result = groupId;
        result = 31 * result + (bicycleDrivers != null ? bicycleDrivers.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BicycleDriverGroup{" +
                "groupId=" + groupId +
                ", bicycleDrivers=" + bicycleDrivers +
                '}';
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public ArrayList<BicycleDriver> getBicycleDrivers() {
        return bicycleDrivers;
    }

    public void setBicycleDrivers(ArrayList<BicycleDriver> bicycleDrivers) {
        this.bicycleDrivers = bicycleDrivers;
    }
}
