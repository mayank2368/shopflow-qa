// ShopFlow SPA. Hash routing, talks to the REST API. Every interactive element
// has a stable data-test attribute so the Selenium Page Objects have reliable locators.

const state = { token: localStorage.getItem("sf_token") || null, user: null };
const view = document.getElementById("view");
const header = document.querySelector("[data-test=header]");

// ---------- helpers ----------
async function api(path, { method = "GET", body } = {}) {
  const res = await fetch(`/api${path}`, {
    method,
    headers: { "content-type": "application/json", ...(state.token ? { authorization: `Bearer ${state.token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  // A 401 on a normal request means the session expired — sign out and prompt a
  // re-login. The login request itself can legitimately 401 (bad credentials),
  // so let that fall through to surface the server's actual message.
  if (res.status === 401 && path !== "/login") {
    logout();
    throw new Error("Please log in again");
  }
  if (!res.ok) throw new Error((await res.json().catch(() => ({}))).error || `Error ${res.status}`);
  return res.status === 204 ? null : res.json();
}

function el(tag, attrs = {}, ...children) {
  const node = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (k === "text") node.textContent = v;
    else if (k === "html") node.innerHTML = v;
    else if (k === "dataset") Object.assign(node.dataset, v);
    else if (v !== false && v != null) node.setAttribute(k, v);
  }
  for (const c of children.flat()) node.append(c);
  return node;
}

function toast(message) {
  const t = document.getElementById("toast");
  t.textContent = message;
  t.hidden = false;
  clearTimeout(toast._t);
  toast._t = setTimeout(() => (t.hidden = true), 2500);
}

function money(n) {
  return `€${Number(n).toFixed(2)}`;
}

function navigate(hash) {
  window.location.hash = hash;
}

// ---------- auth / header ----------
async function refreshHeader() {
  if (!state.token) {
    header.hidden = true;
    return;
  }
  try {
    state.user = await api("/account");
  } catch {
    return;
  }
  header.hidden = false;
  header.querySelector("[data-test=account-menu]").textContent = state.user.name;
  header.querySelector("[data-test=nav-admin]").hidden = state.user.role !== "admin";
  await refreshCartBadge();
}

async function refreshCartBadge() {
  if (!state.token) return;
  try {
    const { totals } = await api("/cart");
    const badge = header.querySelector("[data-test=cart-badge]");
    badge.textContent = totals.count;
    badge.hidden = totals.count === 0;
  } catch {
    /* ignore */
  }
}

function logout() {
  if (state.token) api("/logout", { method: "POST" }).catch(() => {});
  state.token = null;
  state.user = null;
  localStorage.removeItem("sf_token");
  header.hidden = true;
  navigate("#/login");
}

header.querySelector("[data-test=logout]").addEventListener("click", logout);

// ---------- views ----------
function requireAuth() {
  if (!state.token) {
    navigate("#/login");
    return false;
  }
  return true;
}

function title(text) {
  return el("h1", { "data-test": "page-title", text });
}

async function loginView() {
  header.hidden = true;
  const error = el("p", { class: "error", "data-test": "login-error", hidden: true });
  const username = el("input", { type: "text", "data-test": "username", placeholder: "Username", autocomplete: "username" });
  const password = el("input", { type: "password", "data-test": "password", placeholder: "Password", autocomplete: "current-password" });
  const form = el(
    "form",
    { class: "card login", "data-test": "login-form" },
    title("Sign in"),
    el("label", { text: "Username" }),
    username,
    el("label", { text: "Password" }),
    password,
    el("button", { type: "submit", "data-test": "login", text: "Login" }),
    error,
    el("p", { class: "hint", html: "Try <code>standard_user / Pass123!</code> or <code>admin_user / Admin123!</code>" }),
  );
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    error.hidden = true;
    try {
      const { token } = await api("/login", { method: "POST", body: { username: username.value, password: password.value } });
      state.token = token;
      localStorage.setItem("sf_token", token);
      await refreshHeader();
      navigate("#/products");
    } catch (err) {
      error.textContent = err.message;
      error.hidden = false;
    }
  });
  view.replaceChildren(form);
}

async function productsView(params) {
  if (!requireAuth()) return;
  const q = params.get("q") ?? "";
  const category = params.get("category") ?? "";
  const sort = params.get("sort") ?? "name-asc";
  const page = Number(params.get("page") ?? 1);

  const categories = await api("/categories");
  const data = await api(`/products?q=${encodeURIComponent(q)}&category=${encodeURIComponent(category)}&sort=${sort}&page=${page}`);

  const search = el("input", { type: "search", "data-test": "search", placeholder: "Search products", value: q });
  const sortSel = el("select", { "data-test": "sort" },
    ...[["name-asc", "Name A–Z"], ["name-desc", "Name Z–A"], ["price-asc", "Price low–high"], ["price-desc", "Price high–low"]]
      .map(([v, label]) => el("option", { value: v, text: label, selected: v === sort })));
  const catSel = el("select", { "data-test": "category-filter" },
    el("option", { value: "", text: "All categories", selected: category === "" }),
    ...categories.map((c) => el("option", { value: c, text: c, selected: c === category })));

  const applyFilters = () => {
    const p = new URLSearchParams({ q: search.value, category: catSel.value, sort: sortSel.value, page: "1" });
    navigate(`#/products?${p}`);
  };
  search.addEventListener("change", applyFilters);
  search.addEventListener("keydown", (e) => { if (e.key === "Enter") { e.preventDefault(); applyFilters(); } });
  sortSel.addEventListener("change", applyFilters);
  catSel.addEventListener("change", applyFilters);

  const cards = data.items.map((p) =>
    el("div", { class: "card product", "data-test": "product-card", dataset: { productId: p.id, category: p.category } },
      el("a", { href: `#/product/${p.id}`, class: "product-name", "data-test": "product-name", text: p.name }),
      el("div", { class: "muted", text: p.category }),
      el("div", { class: "price", "data-test": "product-price", text: money(p.price) }),
      el("button", { type: "button", "data-test": "add-to-cart", dataset: { productId: p.id }, text: "Add to cart" }),
    ),
  );
  for (const card of cards) {
    card.querySelector("[data-test=add-to-cart]").addEventListener("click", async (e) => {
      await api("/cart/items", { method: "POST", body: { productId: Number(e.target.dataset.productId), quantity: 1 } });
      await refreshCartBadge();
      toast("Added to cart");
    });
  }

  const prev = el("button", { type: "button", "data-test": "page-prev", text: "Previous", disabled: data.page <= 1 });
  const next = el("button", { type: "button", "data-test": "page-next", text: "Next", disabled: data.page >= data.pages });
  prev.addEventListener("click", () => navigate(`#/products?${new URLSearchParams({ q, category, sort, page: data.page - 1 })}`));
  next.addEventListener("click", () => navigate(`#/products?${new URLSearchParams({ q, category, sort, page: data.page + 1 })}`));

  view.replaceChildren(
    title("Products"),
    el("div", { class: "toolbar" }, search, sortSel, catSel),
    el("p", { class: "muted", "data-test": "result-count", text: `${data.total} product(s)` }),
    cards.length ? el("div", { class: "grid", "data-test": "product-grid" }, ...cards) : el("p", { "data-test": "no-results", text: "No products match your search." }),
    el("div", { class: "pagination" }, prev, el("span", { "data-test": "page-info", text: `Page ${data.page} of ${data.pages}` }), next),
  );
}

async function productDetailView(id) {
  if (!requireAuth()) return;
  const p = await api(`/products/${id}`);
  const addBtn = el("button", { type: "button", "data-test": "detail-add", text: "Add to cart" });
  addBtn.addEventListener("click", async () => {
    await api("/cart/items", { method: "POST", body: { productId: p.id, quantity: 1 } });
    await refreshCartBadge();
    toast("Added to cart");
  });
  view.replaceChildren(
    el("a", { href: "#/products", class: "back", "data-test": "back-to-products", text: "← Back to products" }),
    el("div", { class: "card" },
      el("h1", { "data-test": "detail-name", text: p.name }),
      el("div", { class: "muted", "data-test": "detail-category", text: p.category }),
      el("p", { "data-test": "detail-description", text: p.description }),
      el("div", { class: "price", "data-test": "detail-price", text: money(p.price) }),
      addBtn,
    ),
  );
}

async function cartView() {
  if (!requireAuth()) return;
  const { items, totals } = await api("/cart");
  const rows = items.map((line) => {
    const qty = el("input", { type: "number", min: "1", max: "10", value: line.quantity, "data-test": "item-qty", dataset: { productId: line.product.id } });
    qty.addEventListener("change", async () => {
      await api(`/cart/items/${line.product.id}`, { method: "PATCH", body: { quantity: Number(qty.value) } });
      await refreshCartBadge();
      cartView();
    });
    const remove = el("button", { type: "button", "data-test": "item-remove", dataset: { productId: line.product.id }, text: "Remove" });
    remove.addEventListener("click", async () => {
      await api(`/cart/items/${line.product.id}`, { method: "DELETE" });
      await refreshCartBadge();
      cartView();
    });
    return el("div", { class: "card cart-item", "data-test": "cart-item", dataset: { productId: line.product.id } },
      el("span", { class: "item-name", "data-test": "item-name", text: line.product.name }),
      el("span", { "data-test": "item-price", text: money(line.product.price) }),
      qty, remove);
  });

  const checkout = el("button", { type: "button", "data-test": "checkout", text: "Checkout", disabled: items.length === 0 });
  checkout.addEventListener("click", () => navigate("#/checkout"));

  view.replaceChildren(
    title("Your cart"),
    items.length ? el("div", {}, ...rows) : el("p", { "data-test": "cart-empty", text: "Your cart is empty." }),
    el("div", { class: "totals" },
      el("div", { "data-test": "cart-subtotal", text: `Subtotal: ${money(totals.subtotal)}` }),
      el("div", { "data-test": "cart-count", text: `Items: ${totals.count}` })),
    el("a", { href: "#/products", "data-test": "continue-shopping", text: "Continue shopping" }),
    checkout,
  );
}

const checkoutState = { firstName: "", lastName: "", postalCode: "" };

async function checkoutView() {
  if (!requireAuth()) return;
  const error = el("p", { class: "error", "data-test": "checkout-error", hidden: true });
  const first = el("input", { type: "text", "data-test": "first-name", placeholder: "First name" });
  const last = el("input", { type: "text", "data-test": "last-name", placeholder: "Last name" });
  const zip = el("input", { type: "text", "data-test": "postal-code", placeholder: "Postal code" });
  const form = el("form", { class: "card", "data-test": "checkout-form" },
    title("Checkout: your information"),
    el("label", { text: "First name" }), first,
    el("label", { text: "Last name" }), last,
    el("label", { text: "Postal code" }), zip,
    el("button", { type: "submit", "data-test": "continue", text: "Continue" }),
    el("a", { href: "#/cart", "data-test": "cancel", text: "Cancel" }),
    error);
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    error.hidden = true;
    const missing = [["First name", first.value], ["Last name", last.value], ["Postal code", zip.value]].find(([, v]) => !v.trim());
    if (missing) {
      error.textContent = `${missing[0]} is required`;
      error.hidden = false;
      return;
    }
    Object.assign(checkoutState, { firstName: first.value, lastName: last.value, postalCode: zip.value });
    navigate("#/checkout/overview");
  });
  view.replaceChildren(form);
}

async function checkoutOverviewView() {
  if (!requireAuth()) return;
  const { totals } = await api("/cart");
  const finish = el("button", { type: "button", "data-test": "finish", text: "Finish order" });
  finish.addEventListener("click", async () => {
    try {
      const order = await api("/orders", { method: "POST", body: checkoutState });
      await refreshCartBadge();
      sessionStorage.setItem("sf_last_order", order.id);
      navigate("#/checkout/complete");
    } catch (err) {
      toast(err.message);
    }
  });
  view.replaceChildren(
    title("Checkout: overview"),
    el("div", { class: "card totals" },
      el("div", { "data-test": "summary-subtotal", text: `Item total: ${money(totals.subtotal)}` }),
      el("div", { "data-test": "summary-tax", text: `Tax (19%): ${money(totals.tax)}` }),
      el("div", { class: "grand", "data-test": "summary-total", text: `Total: ${money(totals.total)}` })),
    el("a", { href: "#/checkout", "data-test": "overview-cancel", text: "Back" }),
    finish);
}

async function checkoutCompleteView() {
  if (!requireAuth()) return;
  const orderId = sessionStorage.getItem("sf_last_order") || "";
  view.replaceChildren(el("div", { class: "card", "data-test": "order-confirmation" },
    title("Thank you for your order!"),
    el("p", { "data-test": "order-id", text: `Order ${orderId}` }),
    el("a", { href: "#/products", "data-test": "back-home", text: "Back to products" })));
}

async function accountView() {
  if (!requireAuth()) return;
  const acc = await api("/account");
  const saved = el("p", { class: "success", "data-test": "account-saved", hidden: true, text: "Saved!" });
  const name = el("input", { type: "text", "data-test": "account-name", value: acc.name });
  const email = el("input", { type: "email", "data-test": "account-email", value: acc.email });
  const error = el("p", { class: "error", "data-test": "account-error", hidden: true });
  const form = el("form", { class: "card", "data-test": "account-form", novalidate: true },
    title("My account"),
    el("label", { text: "Name" }), name,
    el("label", { text: "Email" }), email,
    el("button", { type: "submit", "data-test": "account-save", text: "Save" }),
    saved, error);
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    saved.hidden = true;
    error.hidden = true;
    try {
      await api("/account", { method: "PUT", body: { name: name.value, email: email.value } });
      await refreshHeader();
      saved.hidden = false;
    } catch (err) {
      error.textContent = err.message;
      error.hidden = false;
    }
  });
  view.replaceChildren(form);
}

async function adminView() {
  if (!requireAuth()) return;
  if (state.user?.role !== "admin") {
    view.replaceChildren(el("div", { class: "card" }, title("Access denied"),
      el("p", { "data-test": "admin-denied", text: "You need an admin account to manage products." })));
    return;
  }
  const data = await api("/products?limit=100");
  const name = el("input", { type: "text", "data-test": "admin-add-name", placeholder: "Product name" });
  const price = el("input", { type: "number", "data-test": "admin-add-price", placeholder: "Price", step: "0.01" });
  const category = el("input", { type: "text", "data-test": "admin-add-category", placeholder: "Category", value: "Misc" });
  const error = el("p", { class: "error", "data-test": "admin-error", hidden: true });
  const addForm = el("form", { class: "card", "data-test": "admin-add-form" },
    el("h2", { text: "Add product" }), name, price, category,
    el("button", { type: "submit", "data-test": "admin-create", text: "Create" }), error);
  addForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    error.hidden = true;
    try {
      await api("/admin/products", { method: "POST", body: { name: name.value, price: Number(price.value), category: category.value } });
      toast("Product created");
      adminView();
    } catch (err) {
      error.textContent = err.message;
      error.hidden = false;
    }
  });

  const rows = data.items.map((p) => {
    const del = el("button", { type: "button", "data-test": "admin-delete", dataset: { productId: p.id }, text: "Delete" });
    del.addEventListener("click", async () => {
      await api(`/admin/products/${p.id}`, { method: "DELETE" });
      toast("Product deleted");
      adminView();
    });
    return el("tr", { "data-test": "admin-row", dataset: { productId: p.id } },
      el("td", { "data-test": "admin-row-name", text: p.name }),
      el("td", { text: p.category }),
      el("td", { text: money(p.price) }),
      el("td", {}, del));
  });

  view.replaceChildren(
    title("Admin — products"),
    addForm,
    el("table", { class: "admin-table", "data-test": "admin-table" },
      el("thead", {}, el("tr", {}, el("th", { text: "Name" }), el("th", { text: "Category" }), el("th", { text: "Price" }), el("th", { text: "" }))),
      el("tbody", {}, ...rows)),
  );
}

// ---------- router ----------
const routes = [
  [/^#\/login$/, loginView],
  [/^#\/products/, (m, params) => productsView(params)],
  [/^#\/product\/(\d+)$/, (m) => productDetailView(m[1])],
  [/^#\/cart$/, cartView],
  [/^#\/checkout$/, checkoutView],
  [/^#\/checkout\/overview$/, checkoutOverviewView],
  [/^#\/checkout\/complete$/, checkoutCompleteView],
  [/^#\/account$/, accountView],
  [/^#\/admin$/, adminView],
];

async function render() {
  const raw = window.location.hash || "#/login";
  const [hashPart, queryPart] = raw.split("?");
  const params = new URLSearchParams(queryPart || "");
  for (const [re, handler] of routes) {
    const m = re.exec(hashPart);
    if (m) {
      try {
        await handler(m, params);
      } catch (err) {
        view.replaceChildren(el("div", { class: "card" }, el("p", { class: "error", "data-test": "page-error", text: err.message })));
      }
      return;
    }
  }
  navigate(state.token ? "#/products" : "#/login");
}

window.addEventListener("hashchange", render);
(async function start() {
  await refreshHeader();
  if (!window.location.hash) navigate(state.token ? "#/products" : "#/login");
  else render();
})();
