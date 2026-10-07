package com.investwise;

public enum InvestmentExperience {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    EXPERIENCED("Experienced"),
    NOT_SURE("Not Sure");

    private final String label;
    InvestmentExperience(String label) { this.label = label; }
    public String label() { return label; }

    public static InvestmentExperience fromLabel(String label) {
        for (InvestmentExperience value : values()) {
            if (value.label.equalsIgnoreCase(label.trim())) return value;
        }
        throw new IllegalArgumentException("Choose a valid investment experience level.");
    }
}
