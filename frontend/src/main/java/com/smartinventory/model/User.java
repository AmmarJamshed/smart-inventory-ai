package com.smartinventory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    private int    id;

    @JsonProperty("real_name_enc")
    private String realNameEnc;

    @JsonProperty("phone_enc")
    private String phoneEnc;

    private String pseudonym;
    private String did;

    @JsonProperty("reputation_score")
    private double reputationScore;

    @JsonProperty("transaction_count")
    private int transactionCount;

    @JsonProperty("created_at")
    private String createdAt;

    public User() {}

    public int    getId()                         { return id; }
    public void   setId(int v)                    { this.id = v; }

    public String getRealNameEnc()                { return realNameEnc; }
    public void   setRealNameEnc(String v)        { this.realNameEnc = v; }

    public String getPhoneEnc()                   { return phoneEnc; }
    public void   setPhoneEnc(String v)           { this.phoneEnc = v; }

    public String getPseudonym()                  { return pseudonym; }
    public void   setPseudonym(String v)          { this.pseudonym = v; }

    public String getDid()                        { return did; }
    public void   setDid(String v)                { this.did = v; }

    public double getReputationScore()            { return reputationScore; }
    public void   setReputationScore(double v)    { this.reputationScore = v; }

    public int    getTransactionCount()           { return transactionCount; }
    public void   setTransactionCount(int v)      { this.transactionCount = v; }

    public String getCreatedAt()                  { return createdAt; }
    public void   setCreatedAt(String v)          { this.createdAt = v; }

    public String getReputationTier() {
        if (reputationScore >= 90) return "Platinum";
        if (reputationScore >= 75) return "Gold";
        if (reputationScore >= 55) return "Silver";
        return "Bronze";
    }

    @Override
    public String toString() {
        return String.format("User{pseudonym=%s, reputation=%.1f, tier=%s}",
                pseudonym, reputationScore, getReputationTier());
    }
}
