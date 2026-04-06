package com.smartinventory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedResult {

    private String  type;
    private String  item;
    private String  quantity;
    private double  price;
    private String  source;

    @JsonProperty("needs_confirmation")
    private boolean needsConfirmation;

    public ParsedResult() {}

    public String  getType()              { return type; }
    public void    setType(String v)      { this.type = v; }

    public String  getItem()              { return item; }
    public void    setItem(String v)      { this.item = v; }

    public String  getQuantity()          { return quantity; }
    public void    setQuantity(String v)  { this.quantity = v; }

    public double  getPrice()             { return price; }
    public void    setPrice(double v)     { this.price = v; }

    public String  getSource()            { return source; }
    public void    setSource(String v)    { this.source = v; }

    public boolean isNeedsConfirmation()        { return needsConfirmation; }
    public void    setNeedsConfirmation(boolean v){ this.needsConfirmation = v; }

    @Override
    public String toString() {
        return String.format("ParsedResult{type=%s, item=%s, qty=%s, price=%.2f, source=%s, confirm=%b}",
                type, item, quantity, price, source, needsConfirmation);
    }
}
