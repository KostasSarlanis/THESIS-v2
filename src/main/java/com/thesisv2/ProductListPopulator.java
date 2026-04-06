package com.thesisv2;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProductListPopulator {

    Integer ProductID, TotalStock, TotalPallets, OutOfPallet, PalletSize;
    String ProductDescription, Warehouses;
    Float PurchasedPrice, SellPrice, WholesalePrice;

    private Integer OriginalTotalStock;
    private Integer OriginalTotalPallets;
    private Integer OriginalOutOfPallet;

    private Map<String, Integer> WarehouseStock = new LinkedHashMap<>();

    public ProductListPopulator(Integer productID,
                                String productDescription,
                                String warehouses,
                                Float purchasedPrice,
                                Float sellPrice,
                                Float wholesalePrice,
                                Integer totalStock,
                                Integer totalPallets,
                                Integer outOfPallet,
                                Integer palletSize) {
        this(productID, productDescription, warehouses, purchasedPrice, sellPrice, wholesalePrice,
                totalStock, totalPallets, outOfPallet, palletSize, new LinkedHashMap<>());
    }

    public ProductListPopulator(Integer productID,
                                String productDescription,
                                String warehouses,
                                Float purchasedPrice,
                                Float sellPrice,
                                Float wholesalePrice,
                                Integer totalStock,
                                Integer totalPallets,
                                Integer outOfPallet,
                                Integer palletSize,
                                Map<String, Integer> warehouseStock) {
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

        OriginalTotalStock = totalStock;
        OriginalTotalPallets = totalPallets;
        OriginalOutOfPallet = outOfPallet;

        if (warehouseStock != null) {
            WarehouseStock = new LinkedHashMap<>(warehouseStock);
        }
    }

    public void resetDisplayedStockToOriginal() {
        TotalStock = OriginalTotalStock;
        TotalPallets = OriginalTotalPallets;
        OutOfPallet = OriginalOutOfPallet;
    }

    public void applyWarehouseView(String warehouse) {
        Integer stock = WarehouseStock.get(warehouse);
        if (stock == null) {
            resetDisplayedStockToOriginal();
            return;
        }

        TotalStock = stock;

        if (PalletSize != null && PalletSize > 0) {
            TotalPallets = stock / PalletSize;
            OutOfPallet = stock % PalletSize;
        } else {
            TotalPallets = 0;
            OutOfPallet = stock;
        }
    }

    public Map<String, Integer> getWarehouseStock() {
        return WarehouseStock;
    }

    public void setWarehouseStock(Map<String, Integer> warehouseStock) {
        WarehouseStock = warehouseStock != null ? new LinkedHashMap<>(warehouseStock) : new LinkedHashMap<>();
    }

    public Integer getOriginalTotalStock() {
        return OriginalTotalStock;
    }

    public void setOriginalTotals(Integer totalStock, Integer totalPallets, Integer outOfPallet) {
        OriginalTotalStock = totalStock;
        OriginalTotalPallets = totalPallets;
        OriginalOutOfPallet = outOfPallet;

        TotalStock = totalStock;
        TotalPallets = totalPallets;
        OutOfPallet = outOfPallet;
    }

    public void setProductID(Integer productID) { ProductID = productID; }
    public void setTotalStock(Integer totalStock) { TotalStock = totalStock; }
    public void setTotalPallets(Integer totalPallets) { TotalPallets = totalPallets; }
    public void setOutOfPallet(Integer outOfPallet) { OutOfPallet = outOfPallet; }
    public void setPalletSize(Integer palletSize) { PalletSize = palletSize; }
    public void setProductDescription(String productDescription) { ProductDescription = productDescription; }
    public void setWarehouses(String warehouses) { Warehouses = warehouses; }
    public void setPurchasedPrice(Float purchasedPrice) { PurchasedPrice = purchasedPrice; }
    public void setSellPrice(Float sellPrice) { SellPrice = sellPrice; }
    public void setWholesalePrice(Float wholesalePrice) { WholesalePrice = wholesalePrice; }


    public Integer getProductID() { return ProductID; }
    public Integer getTotalStock() { return TotalStock; }
    public Integer getTotalPallets() { return TotalPallets; }
    public Integer getOutOfPallet() { return OutOfPallet; }
    public Integer getPalletSize() { return PalletSize; }
    public String getProductDescription() { return ProductDescription; }
    public String getWarehouses() { return Warehouses; }
    public Float getPurchasedPrice() { return PurchasedPrice; }
    public Float getSellPrice() { return SellPrice; }
    public Float getWholesalePrice() { return WholesalePrice; }
}