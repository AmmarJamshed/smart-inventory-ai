package com.smartinventory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Recommendation {

    private String type;
    private String pseudonym;
    private double score;
    private String reasoning;

    public Recommendation() {}

    public Recommendation(String type, String pseudonym, double score, String reasoning) {
        this.type      = type;
        this.pseudonym = pseudonym;
        this.score     = score;
        this.reasoning = reasoning;
    }

    public String getType()               { return type; }
    public void   setType(String v)       { this.type = v; }

    public String getPseudonym()          { return pseudonym; }
    public void   setPseudonym(String v)  { this.pseudonym = v; }

    public double getScore()              { return score; }
    public void   setScore(double v)      { this.score = v; }

    public String getReasoning()          { return reasoning; }
    public void   setReasoning(String v)  { this.reasoning = v; }

    @Override
    public String toString() {
        return String.format("Recommendation{type=%s, pseudonym=%s, score=%.2f}", type, pseudonym, score);
    }
}
