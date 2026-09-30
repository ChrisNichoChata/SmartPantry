package com.chataika.smartpantry.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.chataika.smartpantry.model.PantryItem;
import com.chataika.smartpantry.model.Recipe;
import com.chataika.smartpantry.model.RecipeIngredient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * On-device SQLite database (via SQLiteOpenHelper) holding the user's pantry and the
 * recipe collection.
 *
 * Tables:
 *   pantry_items       (_id, name, quantity, unit, expiry_date)
 *   recipes            (_id, name, description, prep_minutes, steps)
 *   recipe_ingredients (_id, recipe_id -> recipes._id, name, quantity, unit)
 *
 * The recipes are seeded in onCreate(), which Android calls only the first time the
 * database file is created, i.e. on first run of the app.
 */
public class PantryDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smart_pantry.db";
    private static final int DATABASE_VERSION = 1;

    // pantry_items
    public static final String TABLE_PANTRY = "pantry_items";
    public static final String COL_ID = "_id";
    public static final String COL_NAME = "name";
    public static final String COL_QUANTITY = "quantity";
    public static final String COL_UNIT = "unit";
    public static final String COL_EXPIRY = "expiry_date";

    // recipes
    public static final String TABLE_RECIPES = "recipes";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_PREP_MINUTES = "prep_minutes";
    public static final String COL_STEPS = "steps";

    // recipe_ingredients
    public static final String TABLE_RECIPE_INGREDIENTS = "recipe_ingredients";
    public static final String COL_RECIPE_ID = "recipe_id";

    /** Steps are stored in one TEXT column, separated by newlines. */
    private static final String STEP_SEPARATOR = "\n";

    private static PantryDbHelper instance;

    /** One shared helper for the whole app, so every screen uses the same connection. */
    public static synchronized PantryDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new PantryDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private PantryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true); // needed for ON DELETE CASCADE
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PANTRY + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NAME + " TEXT NOT NULL, "
                + COL_QUANTITY + " REAL NOT NULL CHECK (" + COL_QUANTITY + " > 0), "
                + COL_UNIT + " TEXT NOT NULL, "
                + COL_EXPIRY + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NAME + " TEXT NOT NULL UNIQUE, "
                + COL_DESCRIPTION + " TEXT, "
                + COL_PREP_MINUTES + " INTEGER NOT NULL DEFAULT 0, "
                + COL_STEPS + " TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPE_INGREDIENTS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_RECIPE_ID + " INTEGER NOT NULL REFERENCES "
                + TABLE_RECIPES + "(" + COL_ID + ") ON DELETE CASCADE, "
                + COL_NAME + " TEXT NOT NULL, "
                + COL_QUANTITY + " REAL NOT NULL, "
                + COL_UNIT + " TEXT NOT NULL)");

        db.execSQL("CREATE INDEX idx_ingredients_recipe ON "
                + TABLE_RECIPE_INGREDIENTS + "(" + COL_RECIPE_ID + ")");

        seedRecipes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is the only schema so far. A future version would migrate here
        // with ALTER TABLE statements instead of dropping the user's pantry.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPE_INGREDIENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PANTRY);
        onCreate(db);
    }

    /** Inserts the built-in recipes in a single transaction (fast, and all-or-nothing). */
    private void seedRecipes(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            for (Recipe recipe : SeedData.recipes()) {
                ContentValues rv = new ContentValues();
                rv.put(COL_NAME, recipe.getName());
                rv.put(COL_DESCRIPTION, recipe.getDescription());
                rv.put(COL_PREP_MINUTES, recipe.getPrepMinutes());
                rv.put(COL_STEPS, TextUtils.join(STEP_SEPARATOR, recipe.getSteps()));
                long recipeId = db.insertOrThrow(TABLE_RECIPES, null, rv);

                for (RecipeIngredient ing : recipe.getIngredients()) {
                    ContentValues iv = new ContentValues();
                    iv.put(COL_RECIPE_ID, recipeId);
                    iv.put(COL_NAME, ing.getName());
                    iv.put(COL_QUANTITY, ing.getQuantity());
                    iv.put(COL_UNIT, ing.getUnit());
                    db.insertOrThrow(TABLE_RECIPE_INGREDIENTS, null, iv);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ------------------------------------------------------------------
    // Pantry CRUD
    // ------------------------------------------------------------------

    /** CREATE - returns the new row id, or -1 on failure. */
    public long insertPantryItem(PantryItem item) {
        long id = getWritableDatabase().insert(TABLE_PANTRY, null, toValues(item));
        item.setId(id);
        return id;
    }

    /** READ - all pantry items, alphabetically. */
    public List<PantryItem> getAllPantryItems() {
        List<PantryItem> items = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE_PANTRY, null, null, null,
                null, null, COL_NAME + " COLLATE NOCASE ASC")) {
            while (c.moveToNext()) {
                items.add(readPantryItem(c));
            }
        }
        return items;
    }

    /** READ - one item by id, or null if it no longer exists. */
    public PantryItem getPantryItem(long id) {
        try (Cursor c = getReadableDatabase().query(TABLE_PANTRY, null,
                COL_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? readPantryItem(c) : null;
        }
    }

    /** UPDATE - returns true if a row was changed. */
    public boolean updatePantryItem(PantryItem item) {
        int rows = getWritableDatabase().update(TABLE_PANTRY, toValues(item),
                COL_ID + " = ?", new String[]{String.valueOf(item.getId())});
        return rows > 0;
    }

    /** DELETE - returns true if a row was removed. */
    public boolean deletePantryItem(long id) {
        int rows = getWritableDatabase().delete(TABLE_PANTRY,
                COL_ID + " = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    public void clearPantry() {
        getWritableDatabase().delete(TABLE_PANTRY, null, null);
    }

    /** Adds the demo pantry from {@link SeedData#samplePantry(LocalDate)}. */
    public void insertSamplePantry(LocalDate today) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (PantryItem item : SeedData.samplePantry(today)) {
                db.insert(TABLE_PANTRY, null, toValues(item));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Applies the result of cooking a recipe: each pantry item gets its new quantity,
     * and items that are used up (quantity 0) are deleted. Runs as one transaction so
     * the pantry is never left half-updated.
     */
    public void applyConsumption(Map<Long, Double> newQuantities) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Map.Entry<Long, Double> entry : newQuantities.entrySet()) {
                String[] args = {String.valueOf(entry.getKey())};
                if (entry.getValue() <= 0) {
                    db.delete(TABLE_PANTRY, COL_ID + " = ?", args);
                } else {
                    ContentValues v = new ContentValues();
                    v.put(COL_QUANTITY, entry.getValue());
                    db.update(TABLE_PANTRY, v, COL_ID + " = ?", args);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static ContentValues toValues(PantryItem item) {
        ContentValues v = new ContentValues();
        v.put(COL_NAME, item.getName().trim());
        v.put(COL_QUANTITY, item.getQuantity());
        v.put(COL_UNIT, item.getUnit());
        if (item.getExpiryDate() == null) {
            v.putNull(COL_EXPIRY);
        } else {
            v.put(COL_EXPIRY, item.getExpiryDate());
        }
        return v;
    }

    private static PantryItem readPantryItem(Cursor c) {
        int expiryIndex = c.getColumnIndexOrThrow(COL_EXPIRY);
        return new PantryItem(
                c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                c.getString(c.getColumnIndexOrThrow(COL_NAME)),
                c.getDouble(c.getColumnIndexOrThrow(COL_QUANTITY)),
                c.getString(c.getColumnIndexOrThrow(COL_UNIT)),
                c.isNull(expiryIndex) ? null : c.getString(expiryIndex));
    }

    // ------------------------------------------------------------------
    // Recipes (read-only for the user)
    // ------------------------------------------------------------------

    /** All recipes with their ingredients. Uses two queries rather than one query per recipe. */
    public List<Recipe> getAllRecipes() {
        SQLiteDatabase db = getReadableDatabase();

        Map<Long, List<RecipeIngredient>> ingredientsByRecipe = new HashMap<>();
        try (Cursor c = db.query(TABLE_RECIPE_INGREDIENTS, null, null, null,
                null, null, COL_ID + " ASC")) {
            while (c.moveToNext()) {
                long recipeId = c.getLong(c.getColumnIndexOrThrow(COL_RECIPE_ID));
                List<RecipeIngredient> list = ingredientsByRecipe.get(recipeId);
                if (list == null) {
                    list = new ArrayList<>();
                    ingredientsByRecipe.put(recipeId, list);
                }
                list.add(readIngredient(c));
            }
        }

        List<Recipe> recipes = new ArrayList<>();
        try (Cursor c = db.query(TABLE_RECIPES, null, null, null,
                null, null, COL_NAME + " COLLATE NOCASE ASC")) {
            while (c.moveToNext()) {
                long id = c.getLong(c.getColumnIndexOrThrow(COL_ID));
                List<RecipeIngredient> ingredients = ingredientsByRecipe.get(id);
                recipes.add(readRecipe(c, ingredients != null ? ingredients : new ArrayList<RecipeIngredient>()));
            }
        }
        return recipes;
    }

    /** One recipe with its ingredients, or null if not found. */
    public Recipe getRecipe(long recipeId) {
        SQLiteDatabase db = getReadableDatabase();
        String[] args = {String.valueOf(recipeId)};

        List<RecipeIngredient> ingredients = new ArrayList<>();
        try (Cursor c = db.query(TABLE_RECIPE_INGREDIENTS, null, COL_RECIPE_ID + " = ?", args,
                null, null, COL_ID + " ASC")) {
            while (c.moveToNext()) {
                ingredients.add(readIngredient(c));
            }
        }

        try (Cursor c = db.query(TABLE_RECIPES, null, COL_ID + " = ?", args, null, null, null)) {
            return c.moveToFirst() ? readRecipe(c, ingredients) : null;
        }
    }

    /** Distinct ingredient names used by recipes - offered as suggestions when adding items. */
    public List<String> getKnownIngredientNames() {
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (Cursor c = getReadableDatabase().query(true, TABLE_RECIPE_INGREDIENTS,
                new String[]{COL_NAME}, null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                names.add(c.getString(0));
            }
        }
        return new ArrayList<>(names);
    }

    private static RecipeIngredient readIngredient(Cursor c) {
        return new RecipeIngredient(
                c.getString(c.getColumnIndexOrThrow(COL_NAME)),
                c.getDouble(c.getColumnIndexOrThrow(COL_QUANTITY)),
                c.getString(c.getColumnIndexOrThrow(COL_UNIT)));
    }

    private static Recipe readRecipe(Cursor c, List<RecipeIngredient> ingredients) {
        String steps = c.getString(c.getColumnIndexOrThrow(COL_STEPS));
        return new Recipe(
                c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                c.getString(c.getColumnIndexOrThrow(COL_NAME)),
                c.getString(c.getColumnIndexOrThrow(COL_DESCRIPTION)),
                c.getInt(c.getColumnIndexOrThrow(COL_PREP_MINUTES)),
                Arrays.asList(steps.split(STEP_SEPARATOR)),
                ingredients);
    }
}
