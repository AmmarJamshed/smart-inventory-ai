package com.smartinventory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction {

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("seller_did")
    private String sellerDid;

    @JsonProperty("buyer_did")
    private String buyerDid;

    private String item;
    private String quantity;
    private double price;
    private String timestamp;
    private String status;
    private String hash;

    public Transaction() {}

    public String getTransactionId()             { return transactionId; }
    public void   setTransactionId(String v)     { this.transactionId = v; }

    public String getSellerDid()                 { return sellerDid; }
    public void   setSellerDid(String v)         { this.sellerDid = v; }

    public String getBuyerDid()                  { return buyerDid; }
    public void   setBuyerDid(String v)          { this.buyerDid = v; }

    public String getItem()                      { return item; }
    public void   setItem(String v)              { this.item = v; }

    public String getQuantity()                  { return quantity; }
    public void   setQuantity(String v)          { this.quantity = v; }

    public double getPrice()                     { return price; }
    public void   setPrice(double v)             { this.price = v; }

    public String getTimestamp()                 { return timestamp; }
    public void   setTimestamp(String v)         { this.timestamp = v; }

    public String getStatus()                    { return status; }
    public void   setStatus(String v)            { this.status = v; }

    public String getHash()                      { return hash; }
    public void   setHash(String v)              { this.hash = v; }

    @Override
    public String toString() {
        return String.format("Transaction{id=%s, item=%s, qty=%s, status=%s}",
                transactionId, item, quantity, status);
    }
}
