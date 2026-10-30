// ShopFlow backend: REST API + static frontend on one port.
// Zero dependencies (Node built-ins only).
import fs from "node:fs";
import http from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { Store, ValidationError } from "./store.js";

const PUBLIC = path.join(path.dirname(fileURLToPath(import.meta.url)), "..", "public");
const TYPES = { ".html": "text/html; charset=utf-8", ".js": "text/javascript", ".css": "text/css", ".svg": "image/svg+xml" };
const ALLOW_TEST = process.env.ALLOW_TEST_RESET !== "false"; // on by default for the demo

export function createServer() {
  const store = new Store();

  const json = (res, status, body) => {
    res.writeHead(status, { "content-type": "application/json", "cache-control": "no-store" });
    res.end(body === undefined ? "" : JSON.stringify(body));
  };
  const body = (req) =>
    new Promise((resolve, reject) => {
      let raw = "";
      req.on("data", (c) => {
        raw += c;
        if (raw.length > 20_000) reject(Object.assign(new Error("Body too large"), { status: 413 }));
      });
      req.on("end", () => {
        try {
          resolve(raw ? JSON.parse(raw) : {});
        } catch {
          reject(Object.assign(new Error("Invalid JSON"), { status: 400 }));
        }
      });
    });
  const auth = (req) => {
    const token = (req.headers.authorization ?? "").replace(/^Bearer /, "");
    const user = store.userByToken(token);
    if (!user) throw Object.assign(new Error("Authentication required"), { status: 401 });
    return user;
  };

  async function api(req, res, url) {
    const p = url.pathname;
    const m = req.method;

    if (m === "GET" && (p === "/api/health")) return json(res, 200, { ok: true });
    if (ALLOW_TEST && m === "POST" && p === "/api/test/reset") {
      store.reset();
      return json(res, 204);
    }
    if (ALLOW_TEST && m === "POST" && p === "/api/test/seed") {
      store.seed(await body(req));
      return json(res, 204);
    }

    if (m === "POST" && p === "/api/login") {
      const { username, password } = await body(req);
      const { token, user } = store.login(username, password);
      return json(res, 200, { token, user: { id: user.id, name: user.name, role: user.role } });
    }
    if (m === "POST" && p === "/api/logout") {
      store.logout((req.headers.authorization ?? "").replace(/^Bearer /, ""));
      return json(res, 204);
    }

    if (m === "GET" && p === "/api/products") {
      return json(res, 200, store.listProducts({
        q: url.searchParams.get("q") ?? "",
        category: url.searchParams.get("category") ?? "",
        sort: url.searchParams.get("sort") ?? "name-asc",
        page: Number(url.searchParams.get("page") ?? 1),
        limit: Number(url.searchParams.get("limit") ?? 6),
      }));
    }
    if (m === "GET" && p === "/api/categories") return json(res, 200, store.categories());
    const prodMatch = /^\/api\/products\/(\d+)$/.exec(p);
    if (m === "GET" && prodMatch) {
      const product = store.getProduct(Number(prodMatch[1]));
      return product ? json(res, 200, product) : json(res, 404, { error: "Product not found" });
    }

    // cart (auth)
    if (p === "/api/cart") {
      const user = auth(req);
      if (m === "GET") return json(res, 200, { items: store.getCart(user.id), totals: store.cartTotals(user.id) });
    }
    if (m === "POST" && p === "/api/cart/items") {
      const user = auth(req);
      const { productId, quantity = 1 } = await body(req);
      return json(res, 201, store.addToCart(user.id, Number(productId), Number(quantity)));
    }
    const cartItem = /^\/api\/cart\/items\/(\d+)$/.exec(p);
    if (cartItem) {
      const user = auth(req);
      if (m === "PATCH") return json(res, 200, store.updateCartItem(user.id, Number(cartItem[1]), Number((await body(req)).quantity)));
      if (m === "DELETE") return json(res, 200, store.removeFromCart(user.id, Number(cartItem[1])));
    }

    if (m === "POST" && p === "/api/orders") {
      const user = auth(req);
      return json(res, 201, store.checkout(user.id, await body(req)));
    }

    if (p === "/api/account") {
      const user = auth(req);
      if (m === "GET") return json(res, 200, { id: user.id, name: user.name, email: user.email, role: user.role });
      if (m === "PUT") return json(res, 200, store.updateAccount(user.id, await body(req)));
    }

    // admin CRUD (admin only)
    if (p.startsWith("/api/admin/products")) {
      const user = auth(req);
      if (user.role !== "admin") throw Object.assign(new Error("Admins only"), { status: 403 });
      if (m === "POST" && p === "/api/admin/products") return json(res, 201, store.createProduct(await body(req)));
      const adminItem = /^\/api\/admin\/products\/(\d+)$/.exec(p);
      if (adminItem && m === "PUT") {
        const updated = store.updateProduct(Number(adminItem[1]), await body(req));
        return updated ? json(res, 200, updated) : json(res, 404, { error: "Product not found" });
      }
      if (adminItem && m === "DELETE") return store.deleteProduct(Number(adminItem[1])) ? json(res, 204) : json(res, 404, { error: "Product not found" });
    }

    return json(res, 404, { error: "Not found" });
  }

  return http.createServer(async (req, res) => {
    const url = new URL(req.url, "http://localhost");
    try {
      if (url.pathname.startsWith("/api/")) return await api(req, res, url);

      // Static files; unknown non-file routes fall back to the matching page or index
      let file = path.join(PUBLIC, path.normalize(url.pathname === "/" ? "/index.html" : url.pathname));
      if (!file.startsWith(PUBLIC)) return json(res, 404, { error: "Not found" });
      if (!fs.existsSync(file)) {
        const asHtml = `${file}.html`;
        file = fs.existsSync(asHtml) ? asHtml : path.join(PUBLIC, "index.html");
      }
      res.writeHead(200, { "content-type": TYPES[path.extname(file)] ?? "application/octet-stream" });
      fs.createReadStream(file).pipe(res);
    } catch (error) {
      const status = error instanceof ValidationError ? 400 : error.status ?? 500;
      if (status === 500) console.error(error);
      json(res, status, { error: status === 500 ? "Internal server error" : error.message });
    }
  });
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  const port = Number(process.env.PORT ?? 5000);
  createServer().listen(port, () => console.log(`ShopFlow running on http://localhost:${port}`));
}
