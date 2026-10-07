package com.investwise;

import java.util.List;

public final class InvestmentIdea {
    public String id;
    public String name;
    public String category;
    public String riskLevel;
    public int riskBand;
    public String duration;
    public double durationMinYears;
    public double durationMaxYears;
    public String liquidity;
    public int liquidityRank;
    public List<String> suitableGoals;
    public List<String> suitableStyles;
    public List<String> suitableExperience;
    public String experienceSuitability;
    public boolean regularIncomeSuitable;
    public double minimumInvestment;
    public List<String> advantages;
    public List<String> risks;
    public List<String> limitations;
    public String description;

    public void validate() {
        if (blank(id) || blank(name) || blank(category) || blank(riskLevel)
                || blank(duration) || blank(liquidity) || blank(description)
                || blank(experienceSuitability)) {
            throw new IllegalStateException("Investment data has a missing required text field.");
        }
        Category.fromLabel(category);
        if (riskBand < 1 || riskBand > 3 || liquidityRank < 1 || liquidityRank > 4
                || durationMinYears < 0 || durationMaxYears < durationMinYears
                || minimumInvestment < 0) {
            throw new IllegalStateException("Investment data has an invalid compatibility range.");
        }
        if (suitableGoals == null || suitableGoals.isEmpty()
                || suitableStyles == null || suitableStyles.isEmpty()
                || suitableExperience == null || suitableExperience.isEmpty()
                || advantages == null || risks == null || limitations == null) {
            throw new IllegalStateException("Investment data is missing educational metadata.");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
