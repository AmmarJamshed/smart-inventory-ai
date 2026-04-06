package com.smartinventory.model;

import javafx.beans.property.*;

public class InventoryItem {

    private final IntegerProperty id       = new SimpleIntegerProperty();
    private final StringProperty  type     = new SimpleStringProperty();
    private final StringProperty  item     = new SimpleStringProperty();
    private final StringProperty  quantity = new SimpleStringProperty();
    private final DoubleProperty  price    = new SimpleDoubleProperty();
    private final StringProperty  source   = new SimpleStringProperty();
    private final StringProperty  timestamp= new SimpleStringProperty();

    public InventoryItem() {}

    public InventoryItem(String type, String item, String quantity,
                         double price, String source, String timestamp) {
        setType(type); setItem(item); setQuantity(quantity);
        setPrice(price); setSource(source); setTimestamp(timestamp);
    }

    // ── id ──────────────────────────────────────────────────────────────────
    public int getId()                   { return id.get(); }
    public void setId(int v)             { id.set(v); }
    public IntegerProperty idProperty()  { return id; }

    // ── type ─────────────────────────────────────────────────────────────────
    public String getType()               { return type.get(); }
    public void setType(String v)         { type.set(v); }
    public StringProperty typeProperty()  { return type; }

    // ── item ─────────────────────────────────────────────────────────────────
    public String getItem()               { return item.get(); }
    public void setItem(String v)         { item.set(v); }
    public StringProperty itemProperty()  { return item; }

    // ── quantity ─────────────────────────────────────────────────────────────
    public String getQuantity()                { return quantity.get(); }
    public void setQuantity(String v)          { quantity.set(v); }
    public StringProperty quantityProperty()   { return quantity; }

    // ── price ─────────────────────────────────────────────────────────────────
    public double getPrice()              { return price.get(); }
    public void setPrice(double v)        { price.set(v); }
    public DoubleProperty priceProperty() { return price; }

    // ── source ────────────────────────────────────────────────────────────────
    public String getSource()               { return source.get(); }
    public void setSource(String v)         { source.set(v); }
    public StringProperty sourceProperty()  { return source; }

    // ── timestamp ─────────────────────────────────────────────────────────────
    public String getTimestamp()                { return timestamp.get(); }
    public void setTimestamp(String v)          { timestamp.set(v); }
    public StringProperty timestampProperty()   { return timestamp; }

    @Override
    public String toString() {
        return String.format("InventoryItem{id=%d, type=%s, item=%s, qty=%s, price=%.2f}",
                getId(), getType(), getItem(), getQuantity(), getPrice());
    }
}
