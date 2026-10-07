# InvestWise

InvestWise is an educational investment explorer that ranks Indian investment options by profile compatibility, supports side-by-side comparisons, and offers optional AI explanations.

## Run & Operate

- Start the `artifacts/api-server: API Server` workflow for the Java API (port 8080).
- Start the `artifacts/investwise: web` workflow for the Vite-served frontend.
- `mvn -f artifacts/api-server/java/pom.xml package` — compile and package the Java server.
- `pnpm --filter @workspace/investwise run build` — build the static frontend.
- `pnpm run typecheck` — full typecheck across all packages
- `pnpm --filter @workspace/api-spec run codegen` — regenerate API hooks and Zod schemas from the OpenAPI spec
- Optional env: `GEMINI_API_KEY`; `GEMINI_MODEL` defaults to `gemini-2.5-flash`.
- No database or user account is used. Keep Gemini credentials in Replit Secrets.

## Stack

- Frontend: HTML5, CSS3, vanilla JavaScript, and Vite for static development/build serving.
- API: Java 17+ with `com.sun.net.httpserver.HttpServer`; Gson is used for JSON.
- Investment metadata: static JSON file, validated when the Java server starts.
- Gemini is optional and server-side; deterministic recommendation results do not depend on it.

## Where things live

- `artifacts/investwise/index.html`, `src/main.js`, `src/index.css` — user interface.
- `artifacts/api-server/java/src/main/java/com/investwise/` — Java API, validation, ranking, and optional Gemini/chat services.
- `artifacts/api-server/java/data/investments.json` — educational option metadata; no prices or projected returns.
- `lib/api-spec/openapi.yaml` — API contract reference.
- `artifacts/investwise/README.md` — run, configuration, and endpoint notes.

## Architecture decisions

- Compatibility percentages describe preference alignment only; they are not expected returns or investment instructions.
- Investment amounts and personal profile values do not enter the compatibility score. The optional Gemini prompts use preference summaries, not the monetary amounts entered in the form.
- Chat history exists in server memory for a limited session and is not stored in a database.
- The Vite service hosts the plain-JavaScript app; all product API behavior is implemented in Java.

## Product

- Explore 24 Indian investment categories/options using a deterministic profile-compatibility ranking.
- Review details, advantages, risks, limitations, and compare selected options.
- Request optional Gemini-powered educational analysis and session chat when configured.
- Always show the financial-education disclaimer and work without Gemini.

## User preferences

- Keep the product on vanilla HTML/CSS/JavaScript with a Java `HttpServer` API and JSON data.
- Do not add React, an Express/Node backend, Spring Boot, Firebase, or a database.

## Gotchas

- The frontend calls same-origin `/api` paths. The shared proxy routes `/api` to the Java API service.
- Build the API with Maven before running the server; the API workflow does this automatically.
- Missing `GEMINI_API_KEY` disables only Gemini analysis/chat; recommendations remain available.

## Pointers

- See the root OpenAPI contract in `lib/api-spec/openapi.yaml`; the deployed API implementation is Java.
