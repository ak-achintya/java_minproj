package com.investwise;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public record InvestmentInput(
        double investmentAmount,
        double monthlyIncome,
        double monthlyExpenses,
        double existingSavings,
        double existingInvestments,
        RiskLevel risk,
        Duration duration,
        InvestmentGoal goal,
        LiquidityRequirement liquidity,
        InvestmentStyle style,
        InvestmentExperience experience,
        boolean regularIncome,
        List<Category> selectedCategories) {

    public static InvestmentInput fromJson(JsonObject json) {
        if (json == null) throw new IllegalArgumentException("Investment profile is required.");
        double investmentAmount = readAmount(json, "investmentAmount");
        double monthlyIncome = readAmount(json, "monthlyIncome");
        double monthlyExpenses = readAmount(json, "monthlyExpenses");
        double existingSavings = readAmount(json, "existingSavings");
        double existingInvestments = readAmount(json, "existingInvestments");
        if (investmentAmount <= 0) {
            throw new IllegalArgumentException("Investment amount must be greater than zero.");
        }
        String risk = readString(json, "risk");
        String duration = readString(json, "duration");
        String goal = readString(json, "goal");
        String liquidity = readString(json, "liquidity");
        String style = readString(json, "style");
        String experience = readString(json, "experience");
        JsonElement regularIncomeElement = json.get("regularIncome");
        if (regularIncomeElement == null || !regularIncomeElement.isJsonPrimitive()
                || !regularIncomeElement.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Regular income preference must be true or false.");
        }
        JsonElement categoriesElement = json.get("selectedCategories");
        if (categoriesElement == null || !categoriesElement.isJsonArray()) {
            throw new IllegalArgumentException("Selected categories must be a list.");
        }
        JsonArray categoryArray = categoriesElement.getAsJsonArray();
        if (categoryArray.size() > Category.values().length) {
            throw new IllegalArgumentException("Too many investment categories were selected.");
        }
        List<Category> categories = new ArrayList<>();
        for (JsonElement element : categoryArray) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Investment categories must be text values.");
            }
            Category category = Category.fromLabel(element.getAsString());
            if (!categories.contains(category)) categories.add(category);
        }
        return new InvestmentInput(
                investmentAmount,
                monthlyIncome,
                monthlyExpenses,
                existingSavings,
                existingInvestments,
                RiskLevel.fromLabel(risk),
                Duration.fromLabel(duration),
                InvestmentGoal.fromLabel(goal),
                LiquidityRequirement.fromLabel(liquidity),
                InvestmentStyle.fromLabel(style),
                InvestmentExperience.fromLabel(experience),
                regularIncomeElement.getAsBoolean(),
                List.copyOf(categories));
    }

    private static double readAmount(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Enter a valid number for " + humanize(key) + ".");
        }
        double amount = value.getAsDouble();
        if (!Double.isFinite(amount) || amount < 0 || amount > 1_000_000_000_000d) {
            throw new IllegalArgumentException(humanize(key) + " must be a valid non-negative amount.");
        }
        return amount;
    }

    private static String readString(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank()) {
            throw new IllegalArgumentException("Choose a value for " + humanize(key) + ".");
        }
        return value.getAsString().trim();
    }

    private static String humanize(String key) {
        return key.replaceAll("([A-Z])", " $1").toLowerCase();
    }
}
