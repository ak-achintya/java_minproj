package com.investwise;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RecommendationEngine {
    private static final int TOP_COUNT = 5;
    private static final double RISK_WEIGHT = 24;
    private static final double DURATION_WEIGHT = 16;
    private static final double GOAL_WEIGHT = 16;
    private static final double LIQUIDITY_WEIGHT = 12;
    private static final double STYLE_WEIGHT = 12;
    private static final double EXPERIENCE_WEIGHT = 8;
    private static final double CATEGORY_WEIGHT = 7;
    private static final double INCOME_WEIGHT = 5;

    public RecommendationResults recommend(InvestmentInput profile, List<InvestmentIdea> options) {
        List<Ranked> ranked = options.stream()
                .map(option -> score(profile, option))
                .sorted(Comparator.comparingInt(Ranked::score).reversed()
                        .thenComparing(result -> result.idea().name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<Recommendation> all = ranked.stream().map(Ranked::recommendation).toList();
        int splitAt = Math.min(TOP_COUNT, all.size());
        return new RecommendationResults(
                all.subList(0, splitAt),
                all.subList(splitAt, all.size()),
                all.size());
    }

    private Ranked score(InvestmentInput profile, InvestmentIdea option) {
        double risk = Math.max(0, 1.0 - Math.abs(profile.risk().targetBand() - option.riskBand) * 0.5);
        double duration = durationCompatibility(profile.duration(), option);
        double goal = option.suitableGoals.contains(profile.goal().label()) ? 1.0
                : profile.goal() == InvestmentGoal.NOT_SURE ? 0.75 : 0.0;
        double liquidity = Math.min(1.0, option.liquidityRank / (double) profile.liquidity().requiredRank());
        double style = option.suitableStyles.contains(profile.style().label()) ? 1.0 : 0.0;
        double experience = option.suitableExperience.contains(profile.experience().label()) ? 1.0 : 0.5;
        double category = profile.selectedCategories().isEmpty()
                || profile.selectedCategories().stream().anyMatch(item -> item.label().equals(option.category))
                ? 1.0 : 0.0;
        double income = !profile.regularIncome() || option.regularIncomeSuitable ? 1.0 : 0.0;

        double total = (risk * RISK_WEIGHT)
                + (duration * DURATION_WEIGHT)
                + (goal * GOAL_WEIGHT)
                + (liquidity * LIQUIDITY_WEIGHT)
                + (style * STYLE_WEIGHT)
                + (experience * EXPERIENCE_WEIGHT)
                + (category * CATEGORY_WEIGHT)
                + (income * INCOME_WEIGHT);
        List<String> reasons = new ArrayList<>();
        if (risk >= 0.75) reasons.add("Risk level is reasonably aligned with your " + profile.risk().label().toLowerCase() + " preference.");
        if (duration >= 0.75) reasons.add("The stated holding period overlaps your " + profile.duration().label() + " horizon.");
        if (goal == 1.0) reasons.add("It is commonly considered for goals such as " + profile.goal().label() + ".");
        if (liquidity >= 0.75) reasons.add("Its liquidity profile is closer to your stated access needs.");
        if (style == 1.0) reasons.add("It fits your " + profile.style().label().toLowerCase() + " investing style.");
        if (experience == 1.0) reasons.add("Its general complexity is suitable for " + profile.experience().label().toLowerCase() + " investors.");
        if (category == 1.0 && !profile.selectedCategories().isEmpty()) reasons.add("It is in one of the categories you selected.");
        if (profile.regularIncome() && option.regularIncomeSuitable) reasons.add("It may provide scheduled or distribution-based income, subject to its terms.");
        if (reasons.isEmpty()) reasons.add("This option is included for exploration; several profile preferences may not align.");

        int percentage = (int) Math.round(total);
        return new Ranked(option, percentage, Recommendation.from(option, percentage, List.copyOf(reasons)));
    }

    private double durationCompatibility(Duration profileDuration, InvestmentIdea option) {
        double overlap = Math.min(profileDuration.maxYears(), option.durationMaxYears)
                - Math.max(profileDuration.minYears(), option.durationMinYears);
        if (overlap > 0 || (profileDuration.minYears() == option.durationMinYears
                && profileDuration.maxYears() == option.durationMaxYears)) return 1.0;
        double gap = Math.max(option.durationMinYears - profileDuration.maxYears(),
                profileDuration.minYears() - option.durationMaxYears);
        return gap <= 2.0 ? 0.35 : 0.0;
    }

    private record Ranked(InvestmentIdea idea, int score, Recommendation recommendation) {}

    public record RecommendationResults(
            List<Recommendation> topOptions,
            List<Recommendation> additionalOptions,
            int total) {}
}
