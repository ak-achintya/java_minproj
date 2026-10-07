# InvestWise

InvestWise is an educational explorer for Indian investment options. It ranks static educational descriptions against a profile, supports side-by-side comparisons, and optionally asks Gemini to explain the results. Compatibility percentages are not expected returns or investment instructions.

## Run in Replit

Start the managed `artifacts/api-server: API Server` and `artifacts/investwise: web` workflows. The web app calls the Java API through same-origin `/api` routes. The API workflow builds the Java server with Maven before starting it.

## Build locally

Requirements: Java 17 or later, Maven, and pnpm.

```sh
pnpm install
mvn -f artifacts/api-server/java/pom.xml package
pnpm --filter @workspace/investwise run build
```

To run the Java API directly after building:

```sh
PORT=8080 java -jar artifacts/api-server/java/target/investwise-server.jar
```

The frontend is plain HTML, CSS, and JavaScript. Vite is used only for local static serving and building.

## Optional Gemini setup

Set `GEMINI_API_KEY` as a server-side Replit Secret to enable educational analysis and chat. `GEMINI_MODEL` is optional and defaults to `gemini-2.5-flash`; the example values are in `artifacts/api-server/.env.example`. Never put an API key in frontend code or commit it.

Without a Gemini key, deterministic recommendations and comparisons continue to work; analysis and chat show an unavailable state. The server keeps chat sessions in memory for up to two hours and does not persist profiles or chats in a database. Gemini prompts include preference context and option metadata, not the monetary amounts entered in the form.

## API routes

- `GET /api/healthz` — health check
- `GET /api/gemini-status` — optional Gemini availability
- `POST /api/recommend` — validate a profile and rank the static option set
- `POST /api/analyze` — request an educational explanation
- `POST /api/chat` — session-based educational Q&A

The Java API data source is `artifacts/api-server/java/data/investments.json`. It contains no live prices or return projections. Verify current product terms, fees, tax treatment, and eligibility with official sources.

## Disclaimer

InvestWise is an educational investment exploration tool. It does not provide personalized financial, tax, or legal advice and does not guarantee investment performance. Investments involve risk.
