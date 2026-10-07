package com.investwise;

public enum InvestmentStyle {
    GROWTH("Growth"),
    BALANCED("Balanced"),
    INCOME("Income"),
    CAPITAL_PRESERVATION("Capital Preservation"),
    DIVERSIFICATION("Diversification");

    private final String label;
    InvestmentStyle(String label) { this.label = label; }
    public String label() { return label; }

    public static InvestmentStyle fromLabel(String label) {
        for (InvestmentStyle value : values()) {
            if (value.label.equalsIgnoreCase(label.trim())) return value;
        }
        throw new IllegalArgumentException("Choose a valid investment style.");
    }
}
