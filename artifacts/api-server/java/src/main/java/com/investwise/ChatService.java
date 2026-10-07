package com.investwise;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatService {
    private static final int MAX_SESSIONS = 500;
    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final long SESSION_TTL_SECONDS = 2 * 60 * 60;

    private final GeminiService gemini;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public ChatService(GeminiService gemini) {
        this.gemini = gemini;
    }

    public ChatResponse send(ChatRequest request) {
        if (request.message() == null || request.message().isBlank()
                || request.message().length() > 2000) {
            throw new IllegalArgumentException("Enter a message of 1 to 2,000 characters.");
        }
        if (!gemini.isConfigured()) {
            throw new GeminiService.AiUnavailableException(
                    "Gemini is not configured. Recommendations are still available.");
        }

        String sessionId = request.sessionId();
        Session session;
        if (sessionId == null || sessionId.isBlank()) {
            if (request.profile() == null || request.investments() == null || request.investments().isEmpty()) {
                throw new IllegalArgumentException("Start a chat from your recommendation results.");
            }
            removeExpiredSessions();
            if (sessions.size() >= MAX_SESSIONS) {
                evictOldestSession();
            }
            sessionId = UUID.randomUUID().toString();
            session = new Session(buildContext(
                    request.profile(), request.investments(), request.selectedIds()));
            sessions.put(sessionId, session);
        } else {
            session = sessions.get(sessionId);
            if (session == null || session.isExpired()) {
                sessions.remove(sessionId);
                throw new IllegalArgumentException("This chat session expired. Start a new chat.");
            }
        }

        synchronized (session) {
            if (request.profile() != null && request.investments() != null) {
                session.updateContext(buildContext(
                        request.profile(), request.investments(), request.selectedIds()));
            }
            String prompt = session.toPrompt(request.message());
            String answer = gemini.generate(prompt);
            session.addTurn(request.message(), answer);
            return new ChatResponse(sessionId, answer);
        }
    }

    private String buildContext(
            InvestmentInput profile, List<Recommendation> investments, List<String> selectedIds) {
        Set<String> selected = new HashSet<>(selectedIds == null ? List.of() : selectedIds);
        StringBuilder context = new StringBuilder("Profile preferences (educational context only): ")
                .append("risk ").append(profile.risk().label())
                .append(", duration ").append(profile.duration().label())
                .append(", goal ").append(profile.goal().label())
                .append(", liquidity ").append(profile.liquidity().label())
                .append(", style ").append(profile.style().label())
                .append(", experience ").append(profile.experience().label())
                .append(", regular income needed ").append(profile.regularIncome())
                .append(", selected categories ")
                .append(profile.selectedCategories().stream().map(Category::label).toList())
                .append(". Monetary amounts are deliberately not included. Do not treat these preferences as financial advice or a complete financial plan.")
                .append("\n\nOptions from the deterministic compatibility engine (scores mean profile compatibility, not return):\n");
        for (Recommendation investment : investments) {
            context.append(selected.contains(investment.id()) ? "- [selected] " : "- ")
                    .append(investment.name())
                    .append(" (").append(investment.category())
                    .append("), ").append(investment.matchPercentage())
                    .append("% profile compatibility, risk ").append(investment.riskLevel())
                    .append(", duration ").append(investment.duration())
                    .append(", liquidity ").append(investment.liquidity())
                    .append(". Why it may align: ").append(String.join("; ", investment.whyItMatches()))
                    .append(". Advantages: ").append(String.join("; ", investment.advantages()))
                    .append(". Risks: ").append(String.join("; ", investment.risks()))
                    .append(". Limitations: ").append(String.join("; ", investment.limitations()))
                    .append('\n');
        }
        return context.toString();
    }

    private void removeExpiredSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private void evictOldestSession() {
        sessions.entrySet().stream()
                .min(Comparator.comparing(entry -> entry.getValue().lastAccess))
                .ifPresent(entry -> sessions.remove(entry.getKey(), entry.getValue()));
    }

    public record ChatRequest(
            String sessionId,
            String message,
            InvestmentInput profile,
            List<Recommendation> investments,
            List<String> selectedIds) {}

    public record ChatResponse(String sessionId, String text) {}

    private static final class Session {
        private String context;
        private final List<Turn> history = new ArrayList<>();
        private Instant lastAccess = Instant.now();

        private Session(String context) {
            this.context = context;
        }

        private void updateContext(String context) {
            this.context = context;
        }

        private String toPrompt(String message) {
            StringBuilder prompt = new StringBuilder(context)
                    .append("\nConversation so far:\n");
            for (Turn turn : history) {
                prompt.append("User: ").append(turn.user()).append('\n')
                        .append("Assistant: ").append(turn.assistant()).append('\n');
            }
            prompt.append("User: ").append(message);
            return prompt.toString();
        }

        private void addTurn(String user, String assistant) {
            history.add(new Turn(user, assistant));
            while (history.size() * 2 > MAX_HISTORY_MESSAGES) {
                history.remove(0);
            }
            lastAccess = Instant.now();
        }

        private boolean isExpired() {
            return Instant.now().minusSeconds(SESSION_TTL_SECONDS).isAfter(lastAccess);
        }
    }

    private record Turn(String user, String assistant) {}
}
