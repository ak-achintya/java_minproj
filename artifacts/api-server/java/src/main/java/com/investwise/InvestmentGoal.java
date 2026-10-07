package com.investwise;

public enum InvestmentGoal {
    WEALTH_CREATION("Wealth Creation"),
    RETIREMENT("Retirement"),
    EMERGENCY_FUND("Emergency Fund"),
    SHORT_TERM_GOAL("Short-Term Goal"),
    LONG_TERM_GOAL("Long-Term Goal"),
    REGULAR_INCOME("Regular Income"),
    CAPITAL_PRESERVATION("Capital Preservation"),
    TAX_SAVING("Tax Saving"),
    EDUCATION("Education"),
    MAJOR_PURCHASE("Major Purchase"),
    NOT_SURE("Not Sure");

    private final String label;
    InvestmentGoal(String label) { this.label = label; }
    public String label() { return label; }

    public static InvestmentGoal fromLabel(String label) {
        for (InvestmentGoal value : values()) {
            if (value.label.equalsIgnoreCase(label.trim())) return value;
        }
        throw new IllegalArgumentException("Choose a valid investment goal.");
    }
}
