// Seed data for ShopFlow (the system under test). Deterministic so tests are reproducible.
import crypto from "node:crypto";

export function hash(password) {
  return crypto.createHash("sha256").update(`shopflow:${password}`).digest("hex");
}

export const SEED_USERS = [
  { id: 1, username: "standard_user", password: "Pass123!", role: "customer", name: "Sam Standard", email: "sam@shopflow.test", locked: false },
  { id: 2, username: "admin_user", password: "Admin123!", role: "admin", name: "Alex Admin", email: "alex@shopflow.test", locked: false },
  { id: 3, username: "locked_user", password: "Pass123!", role: "customer", name: "Lee Locked", email: "lee@shopflow.test", locked: true },
];

const CATEGORIES = ["Sofas", "Tables", "Chairs", "Lamps"];
const NAMES = {
  Sofas: ["Nordic Sofa", "Cozy Loveseat", "Grand Couch"],
  Tables: ["Oak Table", "Glass Desk", "Round Bistro"],
  Chairs: ["Office Chair", "Dining Chair", "Lounge Chair"],
  Lamps: ["Arc Lamp", "Desk Lamp", "Paper Lantern"],
};

export function seedProducts() {
  const products = [];
  let id = 1;
  for (const category of CATEGORIES) {
    NAMES[category].forEach((name, i) => {
      products.push({
        id: id++,
        name,
        category,
        price: Math.round((49 + i * 70 + CATEGORIES.indexOf(category) * 30) * 100) / 100,
        stock: 5 + ((id * 7) % 20),
        description: `A quality ${name.toLowerCase()} from the ${category} collection.`,
      });
    });
  }
  return products; // 12 products across 4 categories
}
