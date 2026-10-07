package com.investwise;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class InvestmentDataService {
    private final List<InvestmentIdea> investments;

    public InvestmentDataService() {
        this.investments = loadInvestments();
        if (investments.size() < 5) {
            throw new IllegalStateException("At least five investment options are required.");
        }
    }

    public List<InvestmentIdea> all() {
        return investments;
    }

    private static List<InvestmentIdea> loadInvestments() {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<InvestmentIdea>>() {}.getType();
        try (InputStream stream = openDataStream()) {
            if (stream == null) {
                throw new IllegalStateException("Investment metadata file was not found.");
            }
            List<InvestmentIdea> rows = gson.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), listType);
            if (rows == null || rows.isEmpty()) {
                throw new IllegalStateException("Investment metadata file is empty.");
            }
            Set<String> ids = new HashSet<>();
            for (InvestmentIdea row : rows) {
                row.validate();
                if (!ids.add(row.id)) {
                    throw new IllegalStateException("Investment metadata contains a duplicate identifier.");
                }
            }
            return List.copyOf(rows);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load investment metadata.", exception);
        }
    }

    private static InputStream openDataStream() throws IOException {
        String configuredPath = System.getenv("INVESTWISE_DATA_FILE");
        if (configuredPath != null && !configuredPath.isBlank()) {
            Path dataPath = Path.of(configuredPath);
            if (!Files.isRegularFile(dataPath)) {
                throw new IllegalStateException("Configured investment metadata file was not found.");
            }
            return Files.newInputStream(dataPath);
        }
        Path localData = Path.of("java", "data", "investments.json");
        if (Files.isRegularFile(localData)) return Files.newInputStream(localData);
        InputStream bundled = InvestmentDataService.class.getResourceAsStream("/investments.json");
        if (bundled != null) return bundled;
        Path workspaceData = Path.of("artifacts", "api-server", "java", "data", "investments.json");
        return Files.isRegularFile(workspaceData) ? Files.newInputStream(workspaceData) : null;
    }
}
