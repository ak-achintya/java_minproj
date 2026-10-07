package com.investwise;

public enum LiquidityRequirement {
    NEED_ANYTIME("Need Money Anytime", 4),
    LOW("Low Liquidity", 1),
    MEDIUM("Medium Liquidity", 2),
    HIGH("High Liquidity", 3);

    private final String label;
    private final int requiredRank;

    LiquidityRequirement(String label, int requiredRank) {
        this.label = label;
        this.requiredRank = requiredRank;
    }

    public String label() { return label; }
    public int requiredRank() { return requiredRank; }

    public static LiquidityRequirement fromLabel(String label) {
        for (LiquidityRequirement value : values()) {
            if (value.label.equalsIgnoreCase(label.trim())) return value;
        }
        throw new IllegalArgumentException("Choose a valid liquidity requirement.");
    }
}
