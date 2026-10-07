const app = document.querySelector("#app");
const geminiLabel = document.querySelector("#ai-status-label");
const disclaimer = "InvestWise is an educational investment exploration tool. It does not provide personalized financial, tax, or legal advice and does not guarantee investment performance. Investments involve risk.";
const categories = [
  ["Equity", ["Stocks", "Mutual Funds", "ETFs", "Index Funds", "Large Cap Funds", "Mid Cap Funds", "Small Cap Funds"]],
  ["Hybrid", ["Hybrid Funds"]],
  ["Debt / Fixed income", ["Debt Funds", "Liquid Funds", "Corporate Bonds", "Government Securities", "Fixed Deposits", "Recurring Deposits"]],
  ["Gold / commodities", ["Gold", "Gold ETFs", "Silver", "Sovereign Gold Bonds"]],
  ["Alternative / real estate", ["REITs", "InvITs"]],
  ["Government / retirement", ["PPF", "NPS"]],
  ["International", ["International Stocks", "International ETFs"]],
];
const state = {
  gemini: null,
  geminiError: "",
  profile: null,
  recommendations: null,
  additionalVisible: false,
  selected: new Set(),
  requestLoading: false,
  requestError: "",
  analysis: "",
  analysisLoading: false,
  analysisError: "",
  chatHistory: [],
  chatSession: "",
  chatLoading: false,
  chatError: "",
  retryMessage: "",
};
const fields = {
  risk: ["Conservative", "Moderate", "Aggressive", "Not Sure"],
  duration: ["Less than 1 year", "1–3 years", "3–5 years", "5–10 years", "10+ years"],
  goal: ["Wealth Creation", "Retirement", "Emergency Fund", "Short-Term Goal", "Long-Term Goal", "Regular Income", "Capital Preservation", "Tax Saving", "Education", "Major Purchase", "Not Sure"],
  liquidity: ["Need Money Anytime", "Low Liquidity", "Medium Liquidity", "High Liquidity"],
  style: ["Growth", "Balanced", "Income", "Capital Preservation", "Diversification"],
  experience: ["Beginner", "Intermediate", "Experienced", "Not Sure"],
};

function escapeHTML(value = "") {
  return String(value).replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
}
function list(values) {
  return Array.isArray(values) ? values.map((value) => `<li>${escapeHTML(value)}</li>`).join("") : "";
}
function textOr(values, fallback = "Not provided") {
  return Array.isArray(values) && values.length ? values.map(escapeHTML).join(", ") : fallback;
}
function money(value) {
  const amount = Number(value);
  return Number.isFinite(amount) ? new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(amount) : "—";
}
function icon(name) {
  const paths = {
    shield: '<path d="M12 3 4.8 6v5.3c0 4.2 3 7.9 7.2 9.7 4.2-1.8 7.2-5.5 7.2-9.7V6L12 3Z"/><path d="m9 12 2 2 4-4"/>',
    arrow: '<path d="M5 12h14"/><path d="m13 6 6 6-6 6"/>',
    book: '<path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21V5.5Z"/><path d="M4 17a2.5 2.5 0 0 1 2.5-2.5H20"/>',
  };
  return `<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${paths[name] || paths.book}</svg>`;
}
function selectMarkup(key, label, hint = "") {
  const currentValue = state.profile?.[key];
  return `<div class="field"><label for="profile-${key}">${label}</label><select id="profile-${key}" name="${key}" required data-testid="select-${key}"><option value="" disabled ${currentValue ? "" : "selected"}>Choose one</option>${fields[key].map((item) => `<option value="${escapeHTML(item)}" ${currentValue === item ? "selected" : ""}>${escapeHTML(item)}</option>`).join("")}</select>${hint ? `<div class="field-hint">${hint}</div>` : ""}</div>`;
}
function amountMarkup(name, label, hint) {
  const value = state.profile?.[name];
  const minimum = name === "investmentAmount" ? 1 : 0;
  const placeholder = name === "investmentAmount" ? "5000" : "0";
  return `<div class="field"><label for="profile-${name}">${label}</label><div class="input-prefix"><span>₹</span><input id="profile-${name}" name="${name}" type="number" min="${minimum}" step="1" inputmode="decimal" placeholder="${placeholder}" value="${value === undefined ? "" : escapeHTML(value)}" required data-testid="input-${name}" /></div>${hint ? `<div class="field-hint">${hint}</div>` : ""}</div>`;
}
function formView() {
  return `<section class="hero">
    <div class="eyebrow">A thoughtful first step</div>
    <h1>Make sense of investing,<br><em>at your own pace.</em></h1>
    <p>Tell us a little about your goals and preferences. We’ll show educational options that may be compatible with your profile—not predictions or instructions.</p>
    <div class="hero-note">${icon("shield")}<span>Monetary figures are collected for this profile but do not affect match scores and are not sent to Gemini. Scores reflect your selected preferences—not expected returns or performance.</span></div>
  </section>
  <div class="layout">
    <form id="profile-form" class="panel form-panel" novalidate data-testid="form-profile">
      <div class="section-head"><div><h2>Your investing picture</h2><p>There are no right answers. Use your best current estimate.</p></div><span class="step-label">PROFILE · 01</span></div>
      <section class="form-section">
        <h3 class="form-section-title"><span class="section-number">1</span>Money in context</h3>
        <div class="field-grid">
          ${amountMarkup("investmentAmount", "Amount you’re exploring", "The amount you are considering for this exploration.")}
          ${amountMarkup("monthlyIncome", "Monthly income", "Approximate monthly income in INR.")}
          ${amountMarkup("monthlyExpenses", "Monthly expenses", "Approximate monthly household expenses.")}
          ${amountMarkup("existingSavings", "Existing savings", "Savings you already have set aside.")}
          ${amountMarkup("existingInvestments", "Existing investments", "Current investments, if any. Enter 0 if none.")}
        </div>
      </section>
      <section class="form-section">
        <h3 class="form-section-title"><span class="section-number">2</span>Goals &amp; preferences</h3>
        <div class="field-grid">
          ${selectMarkup("risk", "Risk comfort")}
          ${selectMarkup("duration", "How long can you leave this invested?")}
          ${selectMarkup("goal", "What are you exploring this for?")}
          ${selectMarkup("liquidity", "How soon might you need access?")}
          ${selectMarkup("style", "Investment style")}
          ${selectMarkup("experience", "Your investing experience")}
          <label class="field-check full" for="profile-regularIncome"><input id="profile-regularIncome" name="regularIncome" type="checkbox" ${state.profile?.regularIncome ? "checked" : ""} data-testid="input-regular-income" /><span>I’m looking for options that may provide regular income</span></label>
        </div>
      </section>
      <section class="form-section">
        <h3 class="form-section-title"><span class="section-number">3</span>Areas you’d like to explore</h3>
        <p class="field-hint" style="margin:-7px 0 13px">Select categories you are interested in, or leave blank to explore the full list.</p>
        <div class="categories" data-testid="group-categories">${categories.map(([group, items]) => items.map((item) => `<label class="category-option" title="${escapeHTML(group)}"><input type="checkbox" name="selectedCategories" value="${escapeHTML(item)}" ${state.profile?.selectedCategories?.includes(item) ? "checked" : ""} data-testid="category-${item.toLowerCase().replace(/[^a-z0-9]+/g, "-")}" /><span>${escapeHTML(item)}</span></label>`).join("")).join("")}</div>
        <div class="field-hint" style="margin-top:10px">Categories are educational starting points, not endorsements.</div>
      </section>
      <div id="form-error" class="result-alert error hidden" role="alert" data-testid="error-profile"></div>
      <div class="form-footer"><span class="privacy-note">No account needed. You can start over any time. Nothing here is a substitute for advice tailored to your circumstances.</span><button class="button button-primary" type="submit" data-testid="button-get-options">Explore compatible options ${icon("arrow")}</button></div>
    </form>
    <aside class="side-rail" aria-label="How it works">
      <section class="panel rail-card"><h3>How this works</h3><div class="mini-steps"><div class="mini-step"><b>1</b><span>Share your goals and preferences.</span></div><div class="mini-step"><b>2</b><span>Review options ranked by profile compatibility.</span></div><div class="mini-step"><b>3</b><span>Explore details, risks, and differences.</span></div></div></section>
      <section class="panel rail-card"><h3>Compatibility, not performance</h3><p>A match percentage describes alignment with your profile. It does not estimate returns, predict outcomes, or tell you what to buy.</p></section>
      <section class="panel rail-card rail-disclaimer"><h3>Keep in mind</h3><p class="disclaimer-copy">${disclaimer}</p><button class="text-button" style="padding-left:0" type="button" data-action="about" data-testid="button-read-disclaimer">Read about InvestWise →</button></section>
    </aside>
  </div>`;
}
function displayOptions() {
  const result = state.recommendations || {};
  const top = Array.isArray(result.topOptions) ? result.topOptions : [];
  const additional = state.additionalVisible && Array.isArray(result.additionalOptions) ? result.additionalOptions : [];
  return [...top, ...additional];
}
function cardView(item, index) {
  const id = String(item.id ?? `option-${index}`);
  const checked = state.selected.has(id);
  return `<article class="panel option-card" data-testid="card-option-${escapeHTML(id)}">
    <div>
      <div class="option-titleline"><h3>${escapeHTML(item.name || "Investment option")}</h3><span class="category-tag">${escapeHTML(item.category || "Option")}</span><span class="risk-tag">${escapeHTML(item.riskLevel || "Risk varies")} risk</span></div>
      <div class="meta-row"><span><strong>Duration</strong> · ${escapeHTML(item.duration || "Not specified")}</span><span><strong>Liquidity</strong> · ${escapeHTML(item.liquidity || "Not specified")}</span><span><strong>Illustrative minimum</strong> · ${money(item.minimumInvestment)}</span></div>
      ${Array.isArray(item.whyItMatches) && item.whyItMatches.length ? `<div class="reason-list" aria-label="Why it may match">${item.whyItMatches.map((reason) => `<span class="reason-chip">${escapeHTML(reason)}</span>`).join("")}</div>` : ""}
      ${Array.isArray(item.risks) && item.risks.length ? `<div class="card-risks"><strong>Risks to understand</strong><ul>${list(item.risks.slice(0, 2))}</ul></div>` : ""}
    </div>
    <div class="match-block" aria-label="${escapeHTML(item.matchPercentage)} percent profile compatibility"><div class="match-score">${Number.isFinite(Number(item.matchPercentage)) ? `${escapeHTML(item.matchPercentage)}%` : "—"}</div><span class="match-caption">profile match</span></div>
    <div class="option-footer full">
      <span class="field-hint">A compatibility indicator only. Not an expected return.</span>
      <div class="option-buttons"><label class="compare-toggle"><input type="checkbox" data-action="compare-toggle" data-id="${escapeHTML(id)}" ${checked ? "checked" : ""} data-testid="checkbox-compare-${escapeHTML(id)}" /> Compare</label><button class="option-link" type="button" data-action="details" data-id="${escapeHTML(id)}" data-testid="button-details-${escapeHTML(id)}">Explore details →</button></div>
    </div>
  </article>`;
}
function detailItems(title, values) {
  if (!Array.isArray(values) || !values.length) return "";
  return `<section class="detail-section"><h3>${title}</h3><ul>${list(values)}</ul></section>`;
}
function comparisonView(items) {
  if (items.length < 2) return "";
  const row = (label, getter) => `<tr><td>${label}</td>${items.map((item) => `<td>${getter(item)}</td>`).join("")}</tr>`;
  return `<section class="compare-section" id="comparison"><div class="section-head"><div><h3>Side by side</h3><p>Compare the details that matter to your preferences.</p></div><button class="button button-secondary button-small" type="button" data-action="clear-compare" data-testid="button-clear-comparison">Clear comparison</button></div><div class="compare-scroll"><table class="compare-table" data-testid="table-comparison"><thead><tr><th scope="col">Consideration</th>${items.map((item) => `<th scope="col">${escapeHTML(item.name || "Option")}<br><button class="comparison-remove" type="button" data-action="remove-compare" data-id="${escapeHTML(item.id)}" data-testid="button-remove-compare-${escapeHTML(item.id)}">Remove</button></th>`).join("")}</tr></thead><tbody>
  ${row("Category", (item) => escapeHTML(item.category || "—"))}${row("Risk level", (item) => escapeHTML(item.riskLevel || "—"))}${row("Duration", (item) => escapeHTML(item.duration || "—"))}${row("Liquidity", (item) => escapeHTML(item.liquidity || "—"))}${row("Suitable goals", (item) => textOr(item.suitableGoals))}${row("Suitable styles", (item) => textOr(item.suitableStyles))}${row("Advantages", (item) => `<ul class="table-cell-list">${list(item.advantages)}</ul>`)}${row("Risks", (item) => `<ul class="table-cell-list">${list(item.risks)}</ul>`)}${row("Limitations", (item) => `<ul class="table-cell-list">${list(item.limitations)}</ul>`)}
  </tbody></table></div></section>`;
}
function renderResults() {
  const result = state.recommendations || {};
  const top = Array.isArray(result.topOptions) ? result.topOptions : [];
  const additional = Array.isArray(result.additionalOptions) ? result.additionalOptions : [];
  const shown = displayOptions();
  const allOptions = [...top, ...additional];
  const selectedOptions = allOptions.filter((item) => state.selected.has(String(item.id)));
  const count = Number(result.total) || top.length + additional.length;
  let alert = "";
  if (state.requestError) alert = `<div class="result-alert error" role="alert" data-testid="error-recommendations">${escapeHTML(state.requestError)} <button class="text-button" type="button" data-action="retry-recommend" data-testid="button-retry-recommendations">Try again</button></div>`;
  else if (state.gemini === false) alert = `<div class="result-alert" data-testid="notice-ai-unavailable">Gemini learning tools are currently unavailable. Your deterministic recommendations remain available and are not affected.</div>`;
  let skeletons = "";
  if (state.requestLoading) skeletons = `<div class="option-list" aria-label="Loading recommendations"><div class="panel skeleton"><div class="skeleton-inner"><div class="skel-line"></div><div class="skel-line"></div><div class="skel-line"></div></div></div><div class="panel skeleton"><div class="skeleton-inner"><div class="skel-line"></div><div class="skel-line"></div><div class="skel-line"></div></div></div><div class="panel skeleton"><div class="skeleton-inner"><div class="skel-line"></div><div class="skel-line"></div><div class="skel-line"></div></div></div></div>`;
  const noResults = !state.requestLoading && !top.length && !state.requestError ? `<section class="panel empty-state" data-testid="empty-recommendations"><div class="empty-state-mark">${icon("book")}</div><h3>No options were returned</h3><p>The service did not return any investment options for this profile. You can try again or adjust your profile.</p><button class="button button-secondary" type="button" data-action="retry-recommend" data-testid="button-retry-empty">Try again</button></section>` : "";
  const cards = shown.length ? `<div class="option-list">${shown.map(cardView).join("")}</div>` : "";
  const moreButton = additional.length ? `<div style="text-align:center;margin:17px 0"><button class="button button-secondary" type="button" data-action="toggle-additional" data-testid="button-${state.additionalVisible ? "hide" : "show"}-additional">${state.additionalVisible ? "Show top five only" : `Explore ${additional.length} more option${additional.length === 1 ? "" : "s"}`}</button></div>` : "";
  const unavailableAnalysis = state.gemini === false ? `<div class="analysis-unavailable" data-testid="notice-analysis-unavailable">Gemini educational analysis is unavailable right now. Deterministic recommendations above are still available. No AI response has been generated.</div>` : state.geminiError ? `<div class="analysis-unavailable" data-testid="notice-analysis-status-error">We couldn’t confirm Gemini availability. Recommendations still work. Retry the status check from the page refresh.</div>` : "";
  const analysisPanel = !state.requestLoading && top.length ? `<section class="panel analysis-panel"><div class="analysis-head"><div><h3>Understand the options</h3><p>Optional educational context, separate from the recommendation engine.</p></div><span class="ai-label">Gemini · educational only</span></div>${state.analysis ? `<div class="analysis-content" data-testid="text-ai-analysis">${escapeHTML(state.analysis)}</div>` : ""}${state.analysisError ? `<div class="analysis-unavailable" role="alert" data-testid="error-analysis">${escapeHTML(state.analysisError)} <button type="button" class="text-button" data-action="analyze" data-testid="button-retry-analysis">Try again</button></div>` : ""}${unavailableAnalysis}${state.gemini === true ? `<div style="margin-top:17px"><button class="button button-secondary button-small" type="button" data-action="analyze" ${state.analysisLoading ? "disabled" : ""} data-testid="button-analyze">${state.analysisLoading ? "Preparing educational analysis…" : state.analysis ? "Refresh analysis" : "Ask Gemini to explain"}</button><span class="field-hint" style="display:inline-block;margin-left:10px">Does not change match scores.</span></div>` : ""}</section>` : "";
  const chatPanel = !state.requestLoading && top.length ? `<section class="panel chat-panel"><div class="chat-head"><div><h3>Ask a learning question</h3><p>Explore terms, risks, or how the options differ.</p></div><span class="ai-label">Session chat</span></div>${state.gemini !== true ? `<div class="analysis-unavailable" data-testid="notice-chat-unavailable">${state.gemini === false ? "Session chat is unavailable because Gemini is not configured. Recommendations still work." : "Session chat availability could not be confirmed. Recommendations still work."}</div>` : `<div class="chat-messages" id="chat-messages" aria-live="polite" data-testid="list-chat-messages">${state.chatHistory.map((entry, index) => `<div class="chat-message ${entry.role === "user" ? "user" : ""} ${entry.role === "error" ? "error" : ""}" data-testid="message-chat-${index}">${escapeHTML(entry.text)}</div>`).join("")}${state.chatLoading ? `<div class="chat-message" data-testid="status-chat-loading">Preparing an educational response…</div>` : ""}</div><form class="chat-form" id="chat-form" data-testid="form-chat"><input type="text" name="message" maxlength="2000" required placeholder="Ask about a term, risk, or comparison…" aria-label="Your question" data-testid="input-chat-message" /><button class="button button-primary button-small" type="submit" ${state.chatLoading ? "disabled" : ""} data-testid="button-send-chat">Send ${icon("arrow")}</button></form><div class="chat-status ${state.chatError ? "error" : ""}" role="status" data-testid="status-chat">${escapeHTML(state.chatError || (state.chatLoading ? "Waiting for the learning service…" : ""))}</div>`}</section>` : "";
  app.innerHTML = `<div class="results-wrap">
    <div class="results-top"><div><div class="eyebrow">Your exploration</div><h2>Options to understand.</h2><p>${count ? `${count} option${count === 1 ? "" : "s"} returned` : "Results from your profile"} · ranked by profile compatibility</p></div><div class="result-actions"><button class="button button-secondary button-small" type="button" data-action="edit-profile" data-testid="button-edit-profile">Edit profile</button><button class="button button-secondary button-small" type="button" data-action="start-over" data-testid="button-start-over">Start over</button></div></div>
    ${alert}${skeletons}${noResults}${cards}${moreButton}
    ${selectedOptions.length >= 2 ? comparisonView(selectedOptions) : ""}
    ${analysisPanel}${chatPanel}
    <div class="bottom-disclaimer" data-testid="text-disclaimer">${disclaimer}</div>
    ${state.selected.size ? `<div class="compare-bar" role="region" aria-label="Comparison selection"><div><strong>${state.selected.size} selected for comparison</strong><span>${state.selected.size < 2 ? "Select one more option to compare." : "Side-by-side comparison is ready below."}</span></div>${state.selected.size >= 2 ? `<button class="button" type="button" data-action="jump-compare" data-testid="button-view-comparison">View comparison</button>` : ""}</div>` : ""}
  </div>`;
  if (state.chatHistory.length) {
    const messages = document.querySelector("#chat-messages");
    if (messages) messages.scrollTop = messages.scrollHeight;
  }
}
function renderForm() {
  app.innerHTML = formView();
}
function render() {
  if (state.recommendations || state.requestLoading) renderResults();
  else renderForm();
}
function setGeminiStatus(status, error = "") {
  state.gemini = typeof status?.configured === "boolean" ? status.configured : null;
  state.geminiError = error;
  geminiLabel.textContent = state.gemini === true ? "Learning tools available" : state.gemini === false ? "Learning tools unavailable" : "Learning tools status unknown";
  const pill = document.querySelector("[data-testid='status-ai']");
  if (pill) {
    pill.setAttribute("aria-label", geminiLabel.textContent);
    pill.classList.toggle("status-available", state.gemini === true);
    pill.classList.toggle("status-unavailable", state.gemini === false);
    pill.classList.toggle("status-unknown", state.gemini === null);
  }
  if (state.recommendations) renderResults();
}
async function checkGeminiStatus() {
  try {
    const response = await fetch("/api/gemini-status", { method: "GET", headers: { Accept: "application/json" } });
    if (!response.ok) throw new Error(`Status service returned HTTP ${response.status}.`);
    setGeminiStatus(await response.json());
  } catch (error) {
    setGeminiStatus(null, error.message || "Gemini status could not be checked.");
  }
}
function errorMessage(error) {
  const status = error.status;
  if (!status) return "We couldn’t reach the recommendation service. Check your connection and try again.";
  const messages = {
    400: "Some profile information was not accepted. Check the fields and try again.",
    401: "The service needs authorization before it can return options.",
    403: "The service could not process this request. Please try again later.",
    404: "The recommendation service could not be found.",
    429: "The service is busy right now. Please wait a moment and try again.",
    500: "The recommendation service encountered an error. Your profile has not been changed.",
    503: "The recommendation service is temporarily unavailable. Please try again shortly.",
  };
  return messages[status] || `The service returned an unexpected response (HTTP ${status}). Please try again.`;
}
async function apiError(response, fallback) {
  let serverMessage = "";
  try {
    const body = await response.json();
    if (typeof body?.error === "string" && body.error.trim()) serverMessage = body.error.trim();
  } catch {
    // Keep the operation-specific message when the server did not return JSON.
  }
  const error = new Error(serverMessage || fallback);
  error.status = response.status;
  error.serverMessage = serverMessage;
  return error;
}
async function requestRecommendations(profile) {
  state.profile = profile;
  state.requestLoading = true;
  state.requestError = "";
  state.recommendations = null;
  state.additionalVisible = false;
  state.selected.clear();
  state.analysis = "";
  state.analysisError = "";
  state.chatHistory = [];
  state.chatSession = "";
  state.chatError = "";
  render();
  window.scrollTo({ top: 0, behavior: "smooth" });
  try {
    const response = await fetch("/api/recommend", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({ profile }),
    });
    if (!response.ok) throw await apiError(response, "Recommendation request failed.");
    const data = await response.json();
    if (!data || !Array.isArray(data.topOptions) || !Array.isArray(data.additionalOptions)) {
      throw new Error("The recommendation service returned a response we couldn’t read. Please try again.");
    }
    state.recommendations = data;
  } catch (error) {
    state.requestError = error.serverMessage || (error.message?.includes("couldn’t read") ? error.message : errorMessage(error));
    state.recommendations = { topOptions: [], additionalOptions: [], total: 0 };
  } finally {
    state.requestLoading = false;
    render();
  }
}
function profileFromForm(form) {
  const data = new FormData(form);
  return {
    investmentAmount: Number(data.get("investmentAmount")),
    monthlyIncome: Number(data.get("monthlyIncome")),
    monthlyExpenses: Number(data.get("monthlyExpenses")),
    existingSavings: Number(data.get("existingSavings")),
    existingInvestments: Number(data.get("existingInvestments")),
    risk: String(data.get("risk") || ""),
    duration: String(data.get("duration") || ""),
    goal: String(data.get("goal") || ""),
    liquidity: String(data.get("liquidity") || ""),
    style: String(data.get("style") || ""),
    experience: String(data.get("experience") || ""),
    regularIncome: data.get("regularIncome") === "on",
    selectedCategories: data.getAll("selectedCategories").map(String),
  };
}
function validateProfile(form, profile) {
  const error = document.querySelector("#form-error");
  const amounts = ["investmentAmount", "monthlyIncome", "monthlyExpenses", "existingSavings", "existingInvestments"];
  const badAmount = amounts.some((field) => !Number.isFinite(profile[field]) || profile[field] < 0)
    || profile.investmentAmount <= 0;
  if (badAmount || !form.checkValidity()) {
    error.textContent = profile.investmentAmount <= 0
      ? "Enter a positive amount to explore. Other amounts may be zero or higher."
      : badAmount
        ? "Enter zero or a positive amount in each financial field."
        : "Complete each required profile field.";
    error.classList.remove("hidden");
    form.reportValidity();
    return false;
  }
  error.classList.add("hidden");
  return true;
}
function activeItems() {
  const all = [...(state.recommendations?.topOptions || []), ...(state.recommendations?.additionalOptions || [])];
  return all.filter((item) => state.selected.has(String(item.id)));
}
function aiContextItems() {
  const selected = activeItems();
  return selected.length ? selected : displayOptions();
}
function showDetails(id) {
  const item = [...(state.recommendations?.topOptions || []), ...(state.recommendations?.additionalOptions || [])].find((option) => String(option.id) === id);
  if (!item) return;
  const dialog = document.querySelector("#detail-dialog");
  document.querySelector("#detail-title").textContent = item.name || "Investment details";
  document.querySelector("#detail-subtitle").textContent = `${item.category || "Investment option"} · Educational overview`;
  document.querySelector("#detail-content").innerHTML = `<div class="detail-meta"><div><span>Risk level</span><strong>${escapeHTML(item.riskLevel || "Not specified")}</strong></div><div><span>Duration</span><strong>${escapeHTML(item.duration || "Not specified")}</strong></div><div><span>Liquidity</span><strong>${escapeHTML(item.liquidity || "Not specified")}</strong></div><div><span>Illustrative minimum</span><strong>${money(item.minimumInvestment)}</strong></div><div><span>Profile compatibility</span><strong>${Number.isFinite(Number(item.matchPercentage)) ? `${escapeHTML(item.matchPercentage)}%` : "Not provided"}</strong></div><div><span>Experience suitability</span><strong>${escapeHTML(item.experienceSuitability || "Not specified")}</strong></div></div>
  <div class="field-hint" style="margin:0 0 14px">Any starting amount shown is illustrative. Actual minimums vary by product and provider; verify current terms.</div>
  <section class="detail-section"><h3>Educational overview</h3><p>${escapeHTML(item.description || "No educational description was supplied for this option.")}</p></section>
  <section class="detail-section"><h3>Profile context</h3><p><strong>Potentially suitable goals:</strong> ${textOr(item.suitableGoals)}<br><strong>Potentially suitable styles:</strong> ${textOr(item.suitableStyles)}<br><strong>Regular income suitability:</strong> ${item.regularIncomeSuitable === true ? "Listed as suitable" : item.regularIncomeSuitable === false ? "Not listed as suitable" : "Not specified"}</p></section>
  ${detailItems("Why it may align with your profile", item.whyItMatches)}
  ${detailItems("Advantages", item.advantages)}${detailItems("Risks to understand", item.risks)}${detailItems("Limitations", item.limitations)}
  <div class="bottom-disclaimer">${disclaimer}</div>`;
  dialog.showModal();
}
async function requestAnalysis() {
  if (state.gemini !== true || state.analysisLoading || !state.profile || !state.recommendations) return;
  state.analysisLoading = true;
  state.analysisError = "";
  renderResults();
  try {
    const response = await fetch("/api/analyze", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({ profile: state.profile, investments: aiContextItems() }),
    });
    if (!response.ok) throw await apiError(response, "Analysis request failed.");
    const data = await response.json();
    if (typeof data?.text !== "string" || !data.text.trim()) throw new Error("The learning service returned no readable analysis. No fallback answer was generated.");
    state.analysis = data.text;
  } catch (error) {
    state.analysisError = error.serverMessage || (error.status ? errorMessage(error) : (error.message || "Educational analysis could not be loaded."));
  } finally {
    state.analysisLoading = false;
    renderResults();
  }
}
async function sendChat(form) {
  const input = form.elements.message;
  const message = input.value.trim();
  if (!message || state.chatLoading || state.gemini !== true) return;
  state.chatHistory.push({ role: "user", text: message });
  state.chatLoading = true;
  state.chatError = "";
  renderResults();
  try {
    const payload = {
      message,
      profile: state.profile,
      selectedIds: activeItems().map((item) => String(item.id)),
    };
    if (state.chatSession) payload.sessionId = state.chatSession;
    const response = await fetch("/api/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(payload),
    });
    if (!response.ok) throw await apiError(response, "Chat request failed.");
    const data = await response.json();
    if (typeof data?.text !== "string" || !data.text.trim() || typeof data.sessionId !== "string") {
      throw new Error("The learning service returned an incomplete response. No fallback answer was generated.");
    }
    state.chatSession = data.sessionId;
    state.chatHistory.push({ role: "assistant", text: data.text });
  } catch (error) {
    state.chatError = error.serverMessage || (error.status ? errorMessage(error) : (error.message || "Couldn’t reach the learning service."));
  } finally {
    state.chatLoading = false;
    renderResults();
    const chatInput = document.querySelector("#chat-form input");
    if (chatInput) chatInput.focus();
  }
}
function startOver() {
  state.profile = null;
  state.recommendations = null;
  state.requestError = "";
  state.requestLoading = false;
  state.additionalVisible = false;
  state.selected.clear();
  state.analysis = "";
  state.analysisError = "";
  state.chatHistory = [];
  state.chatSession = "";
  state.chatError = "";
  renderForm();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

document.addEventListener("submit", (event) => {
  if (event.target.id === "profile-form") {
    event.preventDefault();
    const profile = profileFromForm(event.target);
    if (validateProfile(event.target, profile)) requestRecommendations(profile);
  }
  if (event.target.id === "chat-form") {
    event.preventDefault();
    sendChat(event.target);
  }
});
document.addEventListener("click", (event) => {
  const control = event.target.closest("[data-action]");
  if (!control) return;
  const action = control.dataset.action;
  if (action === "about") document.querySelector("#about-dialog").showModal();
  if (action === "close-dialog") control.closest("dialog").close();
  if (action === "details") showDetails(control.dataset.id);
  if (action === "edit-profile") {
    state.recommendations = null;
    state.requestError = "";
    renderForm();
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  if (action === "start-over") startOver();
  if (action === "retry-recommend") requestRecommendations(state.profile);
  if (action === "toggle-additional") {
    state.additionalVisible = !state.additionalVisible;
    renderResults();
  }
  if (action === "compare-toggle") {
    const id = control.dataset.id;
    if (control.checked) state.selected.add(id);
    else state.selected.delete(id);
    renderResults();
  }
  if (action === "clear-compare") {
    state.selected.clear();
    renderResults();
  }
  if (action === "remove-compare") {
    state.selected.delete(control.dataset.id);
    renderResults();
  }
  if (action === "jump-compare") document.querySelector("#comparison")?.scrollIntoView({ behavior: "smooth", block: "start" });
  if (action === "analyze") requestAnalysis();
});
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    document.querySelectorAll("dialog[open]").forEach((dialog) => dialog.close());
  }
});
for (const dialog of document.querySelectorAll("dialog")) {
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) dialog.close();
  });
}
renderForm();
checkGeminiStatus();
