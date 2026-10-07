package com.investwise;

import java.util.List;

public record Recommendation(
        String id,
        String name,
        String category,
        String riskLevel,
        String duration,
        String liquidity,
        List<String> suitableGoals,
        List<String> suitableStyles,
        String experienceSuitability,
        boolean regularIncomeSuitable,
        double minimumInvestment,
        List<String> advantages,
        List<String> risks,
        List<String> limitations,
        String description,
        int matchPercentage,
        List<String> whyItMatches) {

    public static Recommendation from(
            InvestmentIdea idea, int matchPercentage, List<String> whyItMatches) {
        return new Recommendation(
                idea.id,
                idea.name,
                idea.category,
                idea.riskLevel,
                idea.duration,
                idea.liquidity,
                idea.suitableGoals,
                idea.suitableStyles,
                idea.experienceSuitability,
                idea.regularIncomeSuitable,
                idea.minimumInvestment,
                idea.advantages,
                idea.risks,
                idea.limitations,
                idea.description,
                matchPercentage,
                whyItMatches);
    }
}
