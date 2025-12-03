package com.thesisv2;

public class ProductListPopulator {
    //naming the variables we need
    Integer ProductID, TotalStock, TotalPallets, OutOfPallet, PalletSize;
    String ProductDescription, Warehouses;
    Float PurchasedPrice, SellPrice, WholesalePrice;

    //make the objects
    public ProductListPopulator(Integer productID, String productDescription, String warehouses, Float purchasedPrice,
                                Float sellPrice, Float wholesalePrice, Integer totalStock, Integer totalPallets,
                                Integer outOfPallet, Integer palletSize) {

        //Populate the variables with the input we got
        ProductID = productID;
        TotalStock = totalStock;
        TotalPallets = totalPallets;
        OutOfPallet = outOfPallet;
        PalletSize = palletSize;
        ProductDescription = productDescription;
        Warehouses = warehouses;
        PurchasedPrice = purchasedPrice;
        SellPrice = sellPrice;
        WholesalePrice = wholesalePrice;
    }



    //setters

    public void setProductID(Integer productID) {
        ProductID = productID;
    }

    public void setTotalStock(Integer totalStock) {
        TotalStock = totalStock;
    }

    public void setTotalPallets(Integer totalPallets) {
        TotalPallets = totalPallets;
    }

    public void setOutOfPallet(Integer outOfPallet) {
        OutOfPallet = outOfPallet;
    }

    public void setPalletSize(Integer palletSize) {
        PalletSize = palletSize;
    }

    public void setProductDescription(String productDescription) {
        ProductDescription = productDescription;
    }

    public void setWarehouses(String warehouses) {
        Warehouses = warehouses;
    }

    public void setPurchasedPrice(Float purchasedPrice) {
        PurchasedPrice = purchasedPrice;
    }

    public void setSellPrice(Float sellPrice) {
        SellPrice = sellPrice;
    }

    public void setWholesalePrice(Float wholesalePrice) {
        WholesalePrice = wholesalePrice;
    }



    //getters

    public Integer getProductID() {
        return ProductID;
    }

    public Integer getTotalStock() {
        return TotalStock;
    }

    public Integer getTotalPallets() {
        return TotalPallets;
    }

    public Integer getOutOfPallet() {
        return OutOfPallet;
    }

    public Integer getPalletSize() {
        return PalletSize;
    }

    public String getProductDescription() {
        return ProductDescription;
    }

    public String getWarehouses() {
        return Warehouses;
    }

    public Float getPurchasedPrice() {
        return PurchasedPrice;
    }

    public Float getSellPrice() {
        return SellPrice;
    }

    public Float getWholesalePrice() {
        return WholesalePrice;
    }
}