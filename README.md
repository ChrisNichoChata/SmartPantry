# Smart Pantry Manager

A Java Android app that helps reduce food waste. You record the ingredients you have at home, and the app suggests recipes you can cook **using only those ingredients**. A recipe is only suggested when you already have every ingredient it needs.

Built for Mobile App Development 700 (Richfield).

## Features

- **Pantry management (full CRUD):** add, view, edit and delete ingredients with a name, quantity, unit and optional expiry date. Every form field is validated.
- **Pantry list:** a `RecyclerView` with a custom adapter, bound to SQLite. Items that expire soon are highlighted, and a warning banner appears when items are close to expiry.
- **20 built-in recipes:** seeded into the database the first time the app runs.
- **Suggested Recipes ("Cook Now"):** runs the strict-matching rule and lists only recipes you can cook right now. Recipes that use up expiring items are listed first.
- **"Almost there" list (optional stretch):** recipes missing exactly one ingredient. They appear in a separate, clearly labelled section and are never mixed in with the suggestions. You can turn this list off in Settings.
- **Recipe detail:** shows each ingredient with a ✓ or ✗ against your pantry, plus the method. The **"I cooked this"** button subtracts the ingredients from your pantry.
- **Settings:** expiring-soon alerts on/off, how many days' warning, ignore expired items, show/hide "Almost there", load a sample pantry, clear the pantry.
- **Empty states:** when nothing matches you see a friendly message, never a blank screen.
- No maps, GPS, location or network access: the app works entirely on the device.

## The strict-matching rule

Implemented in [`RecipeMatcher`](app/src/main/java/com/chataika/smartpantry/logic/RecipeMatcher.java). A recipe is suggested only if **every** ingredient is in the pantry in **at least** the required quantity. Matching copes with real-world messiness:

| Messiness | How it's handled | Example |
|---|---|---|
| Plurals, case, extra spaces | [`IngredientNormalizer`](app/src/main/java/com/chataika/smartpantry/logic/IngredientNormalizer.java) singularises and lower-cases names | `"Tomatoes"` = `"tomato"` |
| Descriptive words | Words like *fresh, large, chopped* are ignored | `"Large free-range eggs"` = `"egg"` |
| Synonyms | A synonym table | `"scallions"` = `"spring onion"`, `"cake flour"` = `"flour"` |
| Different units | [`UnitConverter`](app/src/main/java/com/chataika/smartpantry/logic/UnitConverter.java) converts mass and volume | 1 kg covers 500 g; 1 l covers 250 ml |
| Mass vs volume | Converted using known densities for common dry goods | 1 kg of flour covers 2 cups |
| Duplicate entries | Quantities of the same ingredient are added together | 2 eggs + 3 eggs = 5 eggs |
| Units that can't be compared | Treated as **not** available, so the app never suggests a recipe by mistake | 500 g of tomato vs 1 tomato |

The logic is plain Java with no Android dependencies, so it is covered by JUnit tests in [`RecipeMatcherTest`](app/src/test/java/com/chataika/smartpantry/logic/RecipeMatcherTest.java). The tests include the brief's own example: a recipe needing 5 ingredients with only 4 in the pantry is **not** suggested.

## Database choice: SQLite (`SQLiteOpenHelper`)

I chose on-device SQLite because:

1. **It fits the data.** A pantry belongs to one person on one phone, so there is nothing to sync and no need for accounts or a server.
2. **It works offline and loads instantly.** You can check what to cook in the kitchen with no signal, and there is no network delay.
3. **It's relational.** Recipes have many ingredients, which maps naturally onto a `recipes` table and a `recipe_ingredients` table joined by a foreign key with `ON DELETE CASCADE`.
4. **It needs no extra setup.** It is built into Android, needs no API keys or hosting, and is the approach covered in the module's persistent-data chapter.
5. **Data persists.** Everything is stored in `smart_pantry.db` on the device, so it survives closing and reopening the app.

### Data model

```
pantry_items                  recipes                      recipe_ingredients
------------                  -------                      ------------------
_id  INTEGER PK               _id          INTEGER PK      _id        INTEGER PK
name TEXT NOT NULL            name         TEXT UNIQUE     recipe_id  INTEGER FK -> recipes._id
quantity REAL (> 0)           description  TEXT            name       TEXT
unit TEXT                     prep_minutes INTEGER         quantity   REAL
expiry_date TEXT (ISO, null)  steps        TEXT            unit       TEXT
                                    1 ────────────────< many
```

Settings are stored separately in `SharedPreferences`.

## Screens

| Screen | Type | Purpose |
|---|---|---|
| Pantry | Fragment (bottom nav) | List, delete, and open items to edit; + button to add |
| Add / Edit Ingredient | Activity (Intent with `EXTRA_ITEM_ID`) | Create or update an item, with validation; delete |
| Cook Now (Suggested Recipes) | Fragment (bottom nav) | Strict matches, plus the separate "Almost there" list |
| Recipes | Fragment (bottom nav) | The full collection, with what each recipe is missing |
| Recipe Detail | Activity (Intent with `EXTRA_RECIPE_ID`) | Ingredients ✓/✗, method, "I cooked this" |
| Settings | Fragment (bottom nav) | Preferences and data tools |

## Project structure

```
app/src/main/java/com/chataika/smartpantry/
├── data/    PantryDbHelper (SQLite CRUD + seeding), SeedData, AppSettings, PantryMatching
├── logic/   RecipeMatcher, IngredientNormalizer, UnitConverter, MatchResult, IngredientValidator, Quantities
├── model/   PantryItem, Recipe, RecipeIngredient
└── ui/      MainActivity, the fragments, the adapters, AddEditIngredientActivity, RecipeDetailActivity
```

## Setup and running

**Requirements:** Android Studio (Hedgehog or newer) with its bundled JDK 17, and Android SDK 34.

1. Clone the repository:
   ```
   git clone <repository-url>
   ```
2. In Android Studio choose **File → Open** and select the project folder. Wait for the Gradle sync to finish; it downloads Gradle 8.7 and the dependencies.
3. Create an emulator (**Device Manager → Create device**, any phone with API 26 or higher) or connect an Android phone with USB debugging enabled.
4. Press **Run ▶**.
5. For a quick demo, go to **Settings → Load sample pantry**, then open **Cook Now**.

Command line (with `ANDROID_HOME` set):

```
./gradlew assembleDebug        # build app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # run the matching-logic unit tests
```

**Minimum Android version:** 8.0 (API 26). **Target:** API 34.

## Demonstrating the strict rule

1. Go to **Settings → Load sample pantry**. **Cook Now** now lists recipes such as *French Toast* and *Garlic Butter Rice*. *Pancakes* appears under **Almost there**, because it is missing flour.
2. Add **Flour, 1 kg**. *Pancakes* moves up into the **Ready to cook** list, because 1 kg of flour covers the 1 cup the recipe needs.
3. Delete the flour. *Pancakes* drops out of **Ready to cook** and goes back to **Almost there**.
4. Open *Cheese & Tomato Toastie* and tap **I cooked this**. The bread, cheese, butter and tomato quantities go down on the Pantry screen, and any recipe that no longer has enough drops out of the suggestions.
