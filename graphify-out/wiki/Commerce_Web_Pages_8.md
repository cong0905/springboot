# Commerce Web Pages

> 5 nodes · cohesion 0.40

## Key Concepts

- **cart Thymeleaf template** (6 connections) — `src/main/resources/templates/cart.html`
- **Versioned cart quantity update and item deletion** (2 connections) — `src/main/resources/templates/cart.html`
- **Availability warnings and canCheckout gate** (2 connections) — `src/main/resources/templates/cart.html`
- **POST orders with checkoutKey, cartVersion, expectedTotal and pricing hash** (2 connections) — `src/main/resources/templates/checkout.html`
- **Cart does not reserve stock; prices and quantities rechecked at placement** (1 connections) — `src/main/resources/templates/cart.html`

## Relationships

- [Commerce Web Pages](Commerce_Web_Pages.md) (3 shared connections)

## Source Files

- `src/main/resources/templates/cart.html`
- `src/main/resources/templates/checkout.html`

## Audit Trail

- EXTRACTED: 6 (86%)
- INFERRED: 1 (14%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*