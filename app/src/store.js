// In-memory data store for ShopFlow. Resettable and seedable so functional tests
// start from a known state (in a real app this would be a database).
import crypto from "node:crypto";
import { SEED_USERS, seedProducts } from "./data.js";

export class ValidationError extends Error {}

/** Carries an HTTP status so the server returns the right code for auth failures. */
export class AuthError extends Error {
  constructor(message, status) {
    super(message);
    this.name = "AuthError";
    this.status = status;
  }
}

export class Store {
  constructor() {
    this.reset();
  }

  reset() {
    this.users = SEED_USERS.map((u) => ({ ...u }));
    this.products = seedProducts();
    this.nextProductId = this.products.length + 1;
    this.sessions = new Map(); // token -> userId
    this.carts = new Map(); // userId -> [{productId, quantity}]
    this.orders = [];
    this.nextOrderId = 1;
  }

  /** Test seed hook: override specific slices of state for a scenario. */
  seed({ products, carts } = {}) {
    if (Array.isArray(products)) {
      this.products = products.map((p, i) => ({ id: i + 1, stock: 10, category: "Misc", description: "", ...p }));
      this.nextProductId = this.products.length + 1;
    }
    if (carts) {
      for (const [username, items] of Object.entries(carts)) {
        const user = this.userByName(username);
        if (user) this.carts.set(user.id, items.map((it) => ({ productId: it.productId, quantity: it.quantity ?? 1 })));
      }
    }
  }

  // ---- auth ----
  userByName(username) {
    return this.users.find((u) => u.username === username);
  }
  userByToken(token) {
    const id = this.sessions.get(token);
    return id ? this.users.find((u) => u.id === id) : null;
  }
  login(username, password) {
    const user = this.userByName(username);
    if (!user || user.password !== password) throw new AuthError("Username and password do not match any user", 401);
    if (user.locked) throw new AuthError("Sorry, this user account is locked", 403);
    const token = crypto.randomBytes(16).toString("hex");
    this.sessions.set(token, user.id);
    return { token, user };
  }
  logout(token) {
    this.sessions.delete(token);
  }

  // ---- products ----
  listProducts({ q = "", category = "", sort = "name-asc", page = 1, limit = 6 } = {}) {
    let items = this.products.filter(
      (p) => (!q || p.name.toLowerCase().includes(q.toLowerCase())) && (!category || p.category === category),
    );
    const sorters = {
      "name-asc": (a, b) => a.name.localeCompare(b.name),
      "name-desc": (a, b) => b.name.localeCompare(a.name),
      "price-asc": (a, b) => a.price - b.price,
      "price-desc": (a, b) => b.price - a.price,
    };
    items = [...items].sort(sorters[sort] ?? sorters["name-asc"]);
    const total = items.length;
    const pages = Math.max(1, Math.ceil(total / limit));
    const safePage = Math.min(Math.max(1, page), pages);
    return { items: items.slice((safePage - 1) * limit, safePage * limit), total, page: safePage, pages, limit };
  }
  getProduct(id) {
    return this.products.find((p) => p.id === Number(id)) ?? null;
  }
  categories() {
    return [...new Set(this.products.map((p) => p.category))].sort();
  }

  // ---- cart ----
  getCart(userId) {
    const items = this.carts.get(userId) ?? [];
    return items
      .map((it) => {
        const product = this.getProduct(it.productId);
        return product ? { product, quantity: it.quantity, lineTotal: round(product.price * it.quantity) } : null;
      })
      .filter(Boolean);
  }
  cartTotals(userId) {
    const lines = this.getCart(userId);
    const subtotal = round(lines.reduce((s, l) => s + l.lineTotal, 0));
    const tax = round(subtotal * 0.19);
    return { count: lines.reduce((s, l) => s + l.quantity, 0), subtotal, tax, total: round(subtotal + tax) };
  }
  addToCart(userId, productId, quantity = 1) {
    const product = this.getProduct(productId);
    if (!product) throw new ValidationError("Product not found");
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 10) throw new ValidationError("quantity must be 1-10");
    const cart = this.carts.get(userId) ?? [];
    const existing = cart.find((it) => it.productId === product.id);
    if (existing) existing.quantity = Math.min(10, existing.quantity + quantity);
    else cart.push({ productId: product.id, quantity });
    this.carts.set(userId, cart);
    return this.cartTotals(userId);
  }
  updateCartItem(userId, productId, quantity) {
    const cart = this.carts.get(userId) ?? [];
    const item = cart.find((it) => it.productId === Number(productId));
    if (!item) throw new ValidationError("Item not in cart");
    if (quantity < 1 || quantity > 10) throw new ValidationError("quantity must be 1-10");
    item.quantity = quantity;
    this.carts.set(userId, cart);
    return this.cartTotals(userId);
  }
  removeFromCart(userId, productId) {
    const cart = (this.carts.get(userId) ?? []).filter((it) => it.productId !== Number(productId));
    this.carts.set(userId, cart);
    return this.cartTotals(userId);
  }

  // ---- checkout ----
  checkout(userId, { firstName, lastName, postalCode } = {}) {
    const lines = this.getCart(userId);
    if (lines.length === 0) throw new ValidationError("Cart is empty");
    for (const [field, value] of [["First name", firstName], ["Last name", lastName], ["Postal code", postalCode]]) {
      if (!value || !String(value).trim()) throw new ValidationError(`${field} is required`);
    }
    const totals = this.cartTotals(userId);
    const order = { id: `ORD-${this.nextOrderId++}`, userId, lines, totals, createdAt: new Date().toISOString() };
    this.orders.push(order);
    this.carts.delete(userId);
    return order;
  }

  // ---- account ----
  updateAccount(userId, { name, email } = {}) {
    const user = this.users.find((u) => u.id === userId);
    if (name !== undefined) {
      if (!name.trim()) throw new ValidationError("Name is required");
      user.name = name.trim();
    }
    if (email !== undefined) {
      if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) throw new ValidationError("A valid email is required");
      user.email = email.trim();
    }
    return { id: user.id, name: user.name, email: user.email, role: user.role };
  }

  // ---- admin CRUD ----
  createProduct({ name, category, price, stock = 10 } = {}) {
    if (!name || !name.trim()) throw new ValidationError("Name is required");
    if (!(Number(price) > 0)) throw new ValidationError("Price must be greater than 0");
    const product = { id: this.nextProductId++, name: name.trim(), category: category || "Misc", price: round(Number(price)), stock: Number(stock), description: "" };
    this.products.push(product);
    return product;
  }
  updateProduct(id, patch) {
    const product = this.getProduct(id);
    if (!product) return null;
    if (patch.name !== undefined) {
      if (!patch.name.trim()) throw new ValidationError("Name is required");
      product.name = patch.name.trim();
    }
    if (patch.price !== undefined) {
      if (!(Number(patch.price) > 0)) throw new ValidationError("Price must be greater than 0");
      product.price = round(Number(patch.price));
    }
    if (patch.category !== undefined) product.category = patch.category;
    if (patch.stock !== undefined) product.stock = Number(patch.stock);
    return product;
  }
  deleteProduct(id) {
    const i = this.products.findIndex((p) => p.id === Number(id));
    if (i === -1) return false;
    this.products.splice(i, 1);
    return true;
  }
}

function round(n) {
  return Math.round(n * 100) / 100;
}
