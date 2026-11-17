# BetterGrocer Backend and Database schema (V1 - bettergrocer.org - no longer active)
BetterGrocer's goal is to help NZ shoppers find the cheapest groceries. It compares prices across New World, Paknsave and Woolworth
Allowing the user to build shoppinglists and work out which stores to visit by incorporting travel costs and grocery prices

## Team
Tomuri Keane, Frank Quigley, Archer Railton, Dhruv Lal

## Features
- **Product searching** across all three supermarket franchises, with per-store prices
- **Shopping list builder** that saves lists to your account
- **Store selection and map view** pick the stores near you from a list (offered a search bar) and create a map view for the travel path  
- **Plan calculator** that compares the cost of items against travel cost to suggest where to shop
- **Nearest store and route planning** powered by a maps service
- **Accounts and profiles** with sign-up and sign-in, password policy enforcement, and multiple profiles per account (PIN-protected)
- **Guest mode** so you can try the app without an account
- **Scrapers and parsers** that collect and load product and store data for each chain

## Tech Stack
| Layer | Technology |
|---|---|
| Frontend | React, Vite, ESLint |
| Backend | Java 17, Spring Boot, Spring Security, Spring Data JPA, Maven |
| Database | PostgreSQL; Two schemas: app data and product data |
| Scraping | Python, Docker, Shell |
| Performance testing | Python scripts |

## Repository Structure

```
├── backend/            Spring Boot API (Maven)
├── frontend_react/     React + Vite web app
├── Database/           SQL schemas (app_schema.sql, products_schema.sql)
├── Scraping/           Scrapers, parsers and Docker setup for product data
├── perf-tests/         Load and timing tests for search and plan calculation
```

### Backend
| Package | Purpose |
|---|---|
| `algorithms` | Nearest store, route planning, travel cost, plan calculation and maps integration |
| `backend.app` | Application data: auth, profiles, shopping lists (controllers, entities, repositories, services) |
| `backend.products` | Scraped product and store data: search, per-chain entities, barcode and SKU mappings |
| `backend.config` | Security config and the two data source configs (app DB and products DB) |

The backend uses two data sources; one for user and app data, and one for the scraped product data
Schemas live in `Database/` (copies are also kept next to the code in each `util` package)

### Frontend
| Folder | Purpose |
|---|---|
| `src/pages` | Home, Search, List Builder, Sign In / Sign Up, Account, Profile |
| `src/components` | Nav bar, account and profile cards, PIN input, store map, grocery list context |
| `src/services` | API clients (auth, lists, products), guest-mode services, protected routes and session guard |
| `src/styles` | Per-page CSS |

### Scraping
| Folder | Purpose |
|---|---|
| `newWorld/`, `paknsave/`, `woolworths/` | One scraper per chain, each with its own Dockerfile and requirements |
| `parser/` | Parses raw scraped data and loads it into the products database |
| `run_scraper.py` | Entry point for running a scraper |

Barcode-to-product mapping is handled by `mapped_pid_bc.py` (New World, Paknsave)