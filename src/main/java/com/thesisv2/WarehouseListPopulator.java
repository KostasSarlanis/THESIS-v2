package com.thesisv2;

public class WarehouseListPopulator {
    Integer WarehouseID;
    String Address, City, Country;

    public WarehouseListPopulator(Integer warehouseID, String address, String city, String country){
        WarehouseID = warehouseID;
        Address = address;
        City = city;
        Country = country;
    }

    public Integer getWarehouseID() {
        return WarehouseID;
    }

    public void setWarehouseID(Integer warehouseID) {
        WarehouseID = warehouseID;
    }

    public String getAddress() {
        return Address;
    }

    public void setAddress(String address) {
        Address = address;
    }

    public String getCity() {
        return City;
    }

    public void setCity(String city) {
        City = city;
    }

    public String getCountry() {
        return Country;
    }

    public void setCountry(String country) {
        Country = country;
    }
}
