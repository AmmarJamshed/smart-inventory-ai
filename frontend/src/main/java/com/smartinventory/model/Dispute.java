package com.smartinventory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Dispute {

    @JsonProperty("dispute_id")
    private final StringProperty disputeId     = new SimpleStringProperty();

    @JsonProperty("transaction_id")
    private final StringProperty transactionId  = new SimpleStringProperty();

    private final StringProperty evidence       = new SimpleStringProperty();
    private final StringProperty status         = new SimpleStringProperty();
    private final StringProperty resolution     = new SimpleStringProperty();

    @JsonProperty("created_at")
    private final StringProperty createdAt      = new SimpleStringProperty();

    public Dispute() {}

    public Dispute(String disputeId, String transactionId, String evidence,
                   String status, String resolution, String createdAt) {
        setDisputeId(disputeId);
        setTransactionId(transactionId);
        setEvidence(evidence);
        setStatus(status);
        setResolution(resolution);
        setCreatedAt(createdAt);
    }

    // ── disputeId ────────────────────────────────────────────────────────────
    public String getDisputeId()                   { return disputeId.get(); }
    public void   setDisputeId(String v)           { disputeId.set(v); }
    public StringProperty disputeIdProperty()      { return disputeId; }

    // ── transactionId ─────────────────────────────────────────────────────────
    public String getTransactionId()               { return transactionId.get(); }
    public void   setTransactionId(String v)       { transactionId.set(v); }
    public StringProperty transactionIdProperty()  { return transactionId; }

    // ── evidence ──────────────────────────────────────────────────────────────
    public String getEvidence()                    { return evidence.get(); }
    public void   setEvidence(String v)            { evidence.set(v); }
    public StringProperty evidenceProperty()       { return evidence; }

    // ── status ────────────────────────────────────────────────────────────────
    public String getStatus()                      { return status.get(); }
    public void   setStatus(String v)              { status.set(v); }
    public StringProperty statusProperty()         { return status; }

    // ── resolution ────────────────────────────────────────────────────────────
    public String getResolution()                  { return resolution.get(); }
    public void   setResolution(String v)          { resolution.set(v != null ? v : "Pending"); }
    public StringProperty resolutionProperty()     { return resolution; }

    // ── createdAt ─────────────────────────────────────────────────────────────
    public String getCreatedAt()                   { return createdAt.get(); }
    public void   setCreatedAt(String v)           { createdAt.set(v); }
    public StringProperty createdAtProperty()      { return createdAt; }
}
