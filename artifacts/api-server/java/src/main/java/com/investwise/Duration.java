package com.investwise;

public enum Duration {
    UNDER_ONE_YEAR("Less than 1 year", 0.0, 1.0),
    ONE_TO_THREE("1–3 years", 1.0, 3.0),
    THREE_TO_FIVE("3–5 years", 3.0, 5.0),
    FIVE_TO_TEN("5–10 years", 5.0, 10.0),
    TEN_PLUS("10+ years", 10.0, 40.0);

    private final String label;
    private final double minYears;
    private final double maxYears;

    Duration(String label, double minYears, double maxYears) {
        this.label = label;
        this.minYears = minYears;
        this.maxYears = maxYears;
    }

    public String label() { return label; }
    public double minYears() { return minYears; }
    public double maxYears() { return maxYears; }

    public static Duration fromLabel(String label) {
        for (Duration value : values()) {
            if (value.label.equalsIgnoreCase(label.trim())) return value;
        }
        throw new IllegalArgumentException("Choose a valid investment duration.");
    }
}
