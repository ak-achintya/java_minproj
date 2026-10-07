package com.investwise;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        String rawPort = System.getenv("PORT");
        int port = rawPort == null || rawPort.isBlank() ? 8080 : Integer.parseInt(rawPort);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("PORT must be between 1 and 65535.");
        }
        InvestmentServer server = new InvestmentServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}
