package com.investwise;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class InvestmentServer {
    private static final int MAX_BODY_BYTES = 1_000_000;
    private static final Type RECOMMENDATION_LIST_TYPE =
            new TypeToken<List<Recommendation>>() {}.getType();

    private final Gson gson = new Gson();
    private final InvestmentDataService dataService;
    private final RecommendationEngine recommendationEngine = new RecommendationEngine();
    private final GeminiService gemini = new GeminiService();
    private final ChatService chatService = new ChatService(gemini);
    private final HttpServer server;

    public InvestmentServer(int port) throws IOException {
        dataService = new InvestmentDataService();
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/api", this::handle);
        server.setExecutor(Executors.newFixedThreadPool(12));
    }

    public void start() {
        server.start();
        System.out.println("InvestWise Java API listening on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(1);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String route = route(exchange);
        try {
            switch (route) {
                case "", "/" -> getOnly(exchange, () -> sendJson(exchange, 200,
                        Map.of("status", "ok", "service", "InvestWise API")));
                case "/health", "/healthz" -> getOnly(exchange, () -> sendJson(exchange, 200,
                        Map.of("status", "ok")));
                case "/gemini-status" -> getOnly(exchange, () -> sendJson(exchange, 200,
                        Map.of("configured", gemini.isConfigured(), "model", gemini.model())));
                case "/recommend" -> postOnly(exchange, () -> recommend(exchange));
                case "/analyze" -> postOnly(exchange, () -> analyze(exchange));
                case "/chat" -> postOnly(exchange, () -> chat(exchange));
                default -> sendError(exchange, 404, "The requested API route was not found.");
            }
        } catch (ClientRequestException exception) {
            sendError(exchange, exception.statusCode, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            sendError(exchange, 400, safeMessage(exception));
        } catch (GeminiService.AiUnavailableException exception) {
            sendError(exchange, 503, safeMessage(exception));
        } catch (Exception exception) {
            System.err.println("InvestWise API request failed on " + route + "; details withheld.");
            sendError(exchange, 500, "The request could not be completed. Please try again.");
        } finally {
            exchange.close();
        }
    }

    private void recommend(HttpExchange exchange) throws IOException {
        JsonObject body = readJsonBody(exchange);
        InvestmentInput profile = InvestmentInput.fromJson(asObject(body.get("profile"), "Profile"));
        RecommendationEngine.RecommendationResults results =
                recommendationEngine.recommend(profile, dataService.all());
        sendJson(exchange, 200, results);
    }

    private void analyze(HttpExchange exchange) throws IOException {
        JsonObject body = readJsonBody(exchange);
        InvestmentInput profile = InvestmentInput.fromJson(asObject(body.get("profile"), "Profile"));
        List<Recommendation> investments = canonicalRecommendations(profile, body.get("investments"));
        if (investments.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one investment to analyze.");
        }
        String prompt = """
                Explain the following deterministic compatibility results for educational purposes.
                Do not recalculate, change, or reinterpret their compatibility percentages as returns.
                For each option, explain why it may align with the profile, then mention the most
                important risks, limitations, and questions to verify before making any decision.
                Keep the language clear, concise, and balanced. Do not give a buy/sell instruction.

                Profile preferences:
                %s

                Compatibility results and educational metadata:
                %s
                """.formatted(profileSummary(profile), investmentSummary(investments));
        sendJson(exchange, 200, Map.of("text", gemini.generate(prompt)));
    }

    private void chat(HttpExchange exchange) throws IOException {
        JsonObject body = readJsonBody(exchange);
        String sessionId = optionalString(body, "sessionId");
        String message = requiredString(body, "message");
        InvestmentInput profile = body.has("profile")
                ? InvestmentInput.fromJson(asObject(body.get("profile"), "Profile"))
                : null;
        List<Recommendation> investments = profile == null ? null : allRecommendations(profile);
        List<String> selectedIds = readSelectedIds(body.get("selectedIds"));
        ChatService.ChatResponse response = chatService.send(
                new ChatService.ChatRequest(sessionId, message, profile, investments, selectedIds));
        sendJson(exchange, 200, response);
    }

    private List<Recommendation> canonicalRecommendations(InvestmentInput profile, JsonElement value) {
        List<Recommendation> submitted = readRecommendations(value);
        Map<String, Recommendation> authoritative = allRecommendations(profile).stream()
                .collect(java.util.stream.Collectors.toMap(Recommendation::id, item -> item));
        List<Recommendation> canonical = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Recommendation item : submitted) {
            if (!seen.add(item.id())) {
                throw new IllegalArgumentException("An investment option was selected more than once.");
            }
            Recommendation verified = authoritative.get(item.id());
            if (verified == null) {
                throw new IllegalArgumentException("An investment option was not found in the educational catalog.");
            }
            canonical.add(verified);
        }
        return List.copyOf(canonical);
    }

    private List<Recommendation> allRecommendations(InvestmentInput profile) {
        RecommendationEngine.RecommendationResults results =
                recommendationEngine.recommend(profile, dataService.all());
        return java.util.stream.Stream.concat(
                results.topOptions().stream(), results.additionalOptions().stream()).toList();
    }

    private List<String> readSelectedIds(JsonElement value) {
        if (value == null || value.isJsonNull()) return List.of();
        if (!value.isJsonArray()) {
            throw new IllegalArgumentException("Selected investment identifiers must be a list.");
        }
        JsonArray array = value.getAsJsonArray();
        if (array.size() > dataService.all().size()) {
            throw new IllegalArgumentException("Too many selected investments were provided.");
        }
        java.util.Set<String> knownIds = dataService.all().stream()
                .map(item -> item.id).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> seen = new java.util.HashSet<>();
        List<String> selected = new java.util.ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Selected investment identifiers must be text.");
            }
            String id = element.getAsString();
            if (!knownIds.contains(id) || !seen.add(id)) {
                throw new IllegalArgumentException("A selected investment identifier is invalid.");
            }
            selected.add(id);
        }
        return List.copyOf(selected);
    }

    private List<Recommendation> readRecommendations(JsonElement value) {
        if (value == null || !value.isJsonArray()) {
            throw new IllegalArgumentException("Investment options must be provided as a list.");
        }
        JsonArray array = value.getAsJsonArray();
        if (array.size() > dataService.all().size()) {
            throw new IllegalArgumentException("Too many investment options were provided.");
        }
        List<Recommendation> recommendations;
        try {
            recommendations = gson.fromJson(array, RECOMMENDATION_LIST_TYPE);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Investment options could not be read.");
        }
        if (recommendations == null || recommendations.stream().anyMatch(item ->
                item == null || blank(item.id()) || blank(item.name()) || blank(item.riskLevel())
                        || item.risks() == null || item.limitations() == null
                        || item.whyItMatches() == null
                        || item.matchPercentage() < 0 || item.matchPercentage() > 100)) {
            throw new IllegalArgumentException("Investment options contain invalid data.");
        }
        return recommendations;
    }

    private JsonObject readJsonBody(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            throw new ClientRequestException(415, "Send a JSON request body.");
        }
        byte[] raw;
        try (InputStream input = exchange.getRequestBody()) {
            raw = input.readNBytes(MAX_BODY_BYTES + 1);
        }
        if (raw.length > MAX_BODY_BYTES) {
            throw new ClientRequestException(413, "The request body is too large.");
        }
        if (raw.length == 0) {
            throw new IllegalArgumentException("A JSON request body is required.");
        }
        try {
            JsonElement parsed = JsonParser.parseString(new String(raw, StandardCharsets.UTF_8));
            return asObject(parsed, "Request");
        } catch (RuntimeException exception) {
            if (exception instanceof IllegalArgumentException argumentException
                    && argumentException.getMessage() != null
                    && argumentException.getMessage().startsWith("Request ")) {
                throw argumentException;
            }
            throw new IllegalArgumentException("The request body is not valid JSON.");
        }
    }

    private static JsonObject asObject(JsonElement value, String name) {
        if (value == null || !value.isJsonObject()) {
            throw new IllegalArgumentException(name + " must be a JSON object.");
        }
        return value.getAsJsonObject();
    }

    private static String requiredString(JsonObject body, String key) {
        String value = optionalString(body, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A message is required.");
        }
        return value.trim();
    }

    private static String optionalString(JsonObject body, String key) {
        JsonElement element = body.get(key);
        if (element == null || element.isJsonNull()) return null;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(key + " must be text.");
        }
        return element.getAsString();
    }

    private static String profileSummary(InvestmentInput profile) {
        return "Risk: " + profile.risk().label()
                + "; duration: " + profile.duration().label()
                + "; goal: " + profile.goal().label()
                + "; liquidity: " + profile.liquidity().label()
                + "; style: " + profile.style().label()
                + "; experience: " + profile.experience().label()
                + "; regular income needed: " + profile.regularIncome()
                + "; selected categories: "
                + profile.selectedCategories().stream().map(Category::label).toList();
    }

    private static String investmentSummary(List<Recommendation> investments) {
        StringBuilder output = new StringBuilder();
        for (Recommendation item : investments) {
            output.append("- ").append(item.name())
                    .append(" (").append(item.category()).append("); profile compatibility: ")
                    .append(item.matchPercentage()).append("%; risk: ").append(item.riskLevel())
                    .append("; duration: ").append(item.duration())
                    .append("; liquidity: ").append(item.liquidity())
                    .append("; reasons: ").append(String.join("; ", item.whyItMatches()))
                    .append("; risks: ").append(String.join("; ", item.risks()))
                    .append("; limitations: ").append(String.join("; ", item.limitations()))
                    .append('\n');
        }
        return output.toString();
    }

    private static void getOnly(HttpExchange exchange, IoAction action) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            throw new ClientRequestException(405, "Use GET for this route.");
        }
        action.run();
    }

    private static void postOnly(HttpExchange exchange, IoAction action) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            throw new ClientRequestException(405, "Use POST for this route.");
        }
        action.run();
    }

    private static String route(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String prefix = "/api";
        if (!path.startsWith(prefix)) return "/__not_found__";
        String route = path.substring(prefix.length());
        return route.isEmpty() ? "/" : route;
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] response = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, Map.of("error", message));
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "The request is invalid." : message;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }

    private static final class ClientRequestException extends RuntimeException {
        private final int statusCode;

        private ClientRequestException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }
}
