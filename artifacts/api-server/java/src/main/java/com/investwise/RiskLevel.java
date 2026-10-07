package com.investwise;

public enum RiskLevel {
    CONSERVATIVE("Conservative", 1),
    MODERATE("Moderate", 2),
    AGGRESSIVE("Aggressive", 3),
    NOT_SURE("Not Sure", 2);

    private final String label;
    private final int targetBand;

    RiskLevel(String label, int targetBand) {
        this.label = label;
        this.targetBand = targetBand;
    }

    public String label() { return label; }
    public int targetBand() { return targetBand; }

    public static RiskLevel fromLabel(String label) {
        for (RiskLevel value : values()) {
            if (value.label.equalsIgnoreCase(label.trim())) return value;
        }
        throw new IllegalArgumentException("Choose a valid risk level.");
    }
}
