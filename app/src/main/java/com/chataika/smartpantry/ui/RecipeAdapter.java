package com.chataika.smartpantry.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.chataika.smartpantry.R;
import com.chataika.smartpantry.logic.MatchResult;
import com.chataika.smartpantry.model.Recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for recipe lists. Supports two row types - section headers and
 * recipe cards - so the Suggestions screen can show "Ready to cook" and "Almost there"
 * as clearly separated sections in a single scrolling list.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_RECIPE = 1;

    /** One list row: either a header title or a recipe match result. */
    public static final class Row {
        final String header;
        final MatchResult result;

        private Row(String header, MatchResult result) {
            this.header = header;
            this.result = result;
        }

        public static Row header(String title) { return new Row(title, null); }
        public static Row recipe(MatchResult result) { return new Row(null, result); }
    }

    private final OnRecipeClickListener listener;
    private final List<Row> rows = new ArrayList<>();

    public RecipeAdapter(OnRecipeClickListener listener) {
        this.listener = listener;
    }

    public void setRows(List<Row> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).header != null ? TYPE_HEADER : TYPE_RECIPE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_section_header, parent, false));
        }
        return new RecipeHolder(inflater.inflate(R.layout.item_recipe, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).title.setText(row.header);
        } else {
            bindRecipe((RecipeHolder) holder, row.result);
        }
    }

    private void bindRecipe(RecipeHolder holder, MatchResult result) {
        Context context = holder.itemView.getContext();
        Recipe recipe = result.getRecipe();

        holder.name.setText(recipe.getName());
        holder.description.setText(recipe.getDescription());
        holder.meta.setText(context.getString(R.string.recipe_meta,
                recipe.getPrepMinutes(), recipe.getIngredients().size()));

        int missing = result.getMissingCount();
        if (missing == 0) {
            holder.status.setText(R.string.status_ready);
            holder.status.setTextColor(ContextCompat.getColor(context, R.color.status_ok));
        } else {
            holder.status.setText(context.getResources().getQuantityString(R.plurals.status_missing, missing, missing));
            holder.status.setTextColor(ContextCompat.getColor(context,
                    missing == 1 ? R.color.status_warning : R.color.status_bad));
        }

        int expiring = result.getExpiringIngredientCount();
        if (expiring > 0 && missing == 0) {
            holder.expiring.setText(context.getResources().getQuantityString(R.plurals.uses_expiring, expiring, expiring));
            holder.expiring.setVisibility(View.VISIBLE);
        } else {
            holder.expiring.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onRecipeClick(recipe));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_header);
        }
    }

    static class RecipeHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView description;
        final TextView status;
        final TextView meta;
        final TextView expiring;

        RecipeHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_recipe_name);
            description = itemView.findViewById(R.id.text_recipe_description);
            status = itemView.findViewById(R.id.text_recipe_status);
            meta = itemView.findViewById(R.id.text_recipe_meta);
            expiring = itemView.findViewById(R.id.text_recipe_expiring);
        }
    }
}
