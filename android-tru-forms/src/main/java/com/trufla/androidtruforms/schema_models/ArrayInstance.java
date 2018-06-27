package com.trufla.androidtruforms.schema_models;

import com.google.gson.annotations.SerializedName;
import com.trufla.androidtruforms.schema_views.TruArrayView;

/**
 * Created by ohefny on 6/26/18.
 */

public class ArrayInstance extends SchemaInstance {
    @SerializedName("items")
    protected SchemaInstance items;
    @SerializedName("maxItems")
    protected int maxItems;
    @SerializedName("minItems")
    protected int minItems;
    @SerializedName("uniqueItems")
    protected boolean uniqueItems;
    @Override
    public TruArrayView getViewBuilder() {
        return new TruArrayView(this);
    }
}
