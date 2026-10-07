package com.investwise;

import java.util.Arrays;

public enum Category {
    STOCKS("Stocks"),
    MUTUAL_FUNDS("Mutual Funds"),
    ETFS("ETFs"),
    INDEX_FUNDS("Index Funds"),
    LARGE_CAP_FUNDS("Large Cap Funds"),
    MID_CAP_FUNDS("Mid Cap Funds"),
    SMALL_CAP_FUNDS("Small Cap Funds"),
    HYBRID_FUNDS("Hybrid Funds"),
    DEBT_FUNDS("Debt Funds"),
    LIQUID_FUNDS("Liquid Funds"),
    CORPORATE_BONDS("Corporate Bonds"),
    GOVERNMENT_SECURITIES("Government Securities"),
    FIXED_DEPOSITS("Fixed Deposits"),
    RECURRING_DEPOSITS("Recurring Deposits"),
    GOLD("Gold"),
    GOLD_ETFS("Gold ETFs"),
    SILVER("Silver"),
    SOVEREIGN_GOLD_BONDS("Sovereign Gold Bonds"),
    REITS("REITs"),
    INVITS("InvITs"),
    PPF("PPF"),
    NPS("NPS"),
    INTERNATIONAL_STOCKS("International Stocks"),
    INTERNATIONAL_ETFS("International ETFs");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Category fromLabel(String label) {
        return Arrays.stream(values())
                .filter(value -> value.label.equalsIgnoreCase(label.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported investment category."));
    }
}
