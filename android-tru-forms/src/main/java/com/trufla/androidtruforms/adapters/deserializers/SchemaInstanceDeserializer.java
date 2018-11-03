package com.trufla.androidtruforms.adapters.deserializers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.trufla.androidtruforms.models.ArrayInstance;
import com.trufla.androidtruforms.models.BooleanInstance;
import com.trufla.androidtruforms.models.EnumInstance;
import com.trufla.androidtruforms.models.NumericInstance;
import com.trufla.androidtruforms.models.ObjectInstance;
import com.trufla.androidtruforms.models.SchemaKeywords;
import com.trufla.androidtruforms.models.SchemaInstance;
import com.trufla.androidtruforms.models.StringInstance;
import com.trufla.androidtruforms.utils.TruUtils;
import com.trufla.androidtruforms.utils.ValueToSchemaMapper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.trufla.androidtruforms.models.SchemaKeywords.InstanceTypes.NUMBER;
import static com.trufla.androidtruforms.models.SchemaKeywords.InstanceTypes.STRING;


/**
 * Created by ohefny on 6/26/18.
 */

public class SchemaInstanceDeserializer implements JsonDeserializer<SchemaInstance> {

    HashMap<String, Object> constValues;
    Class arrayInstanceClass;
    Class booleanInstanceClass;
    Class stringInstanceClass;
    Class numericInstanceClass;
    Class objectInstanceClass;

    public SchemaInstanceDeserializer(Class arrayInstanceClass, Class booleanInstanceClass, Class stringInstanceClass, Class numericInstanceClass, Class objectInstanceClass) {
        this.arrayInstanceClass = arrayInstanceClass;
        this.booleanInstanceClass = booleanInstanceClass;
        this.stringInstanceClass = stringInstanceClass;
        this.numericInstanceClass = numericInstanceClass;
        this.objectInstanceClass = objectInstanceClass;
    }

    public SchemaInstanceDeserializer(Class<ArrayInstance> arrayInstanceClass, Class<BooleanInstance> booleanInstanceClass, Class<StringInstance> stringInstanceClass, Class<NumericInstance> numericInstanceClass, Class<ObjectInstance> objectInstanceClass, String values) throws IOException {
        this(arrayInstanceClass, booleanInstanceClass, stringInstanceClass, numericInstanceClass, objectInstanceClass);
        constValues = ValueToSchemaMapper.flattenJson(values);
    }


    @Override
    public SchemaInstance deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonPrimitive prim = (JsonPrimitive) jsonObject.get(SchemaKeywords.TYPE_KEY);
        String type = TruUtils.getText(prim.getAsString(), "No_Type");
        Class<?> klass;
        if (jsonObject.has(SchemaKeywords.ENUM_KEY) || jsonObject.has(SchemaKeywords.TruVocabulary.DATA)) {
            klass = EnumInstance.class;
            return getProperEnumInstance(json, context, type, klass);

        }
        klass = getInstanceClass(type);
        SchemaInstance instance = context.deserialize(json, klass);
        if (!(instance instanceof ObjectInstance) && constValues != null) {
            if (instance instanceof ArrayInstance)
                instance.setConstItem(getArrayConst(instance.getKey()));
            else
                instance.setConstItem(getPrimitiveConst(instance.getKey()));
        }
        if (instance instanceof ObjectInstance)
            setObjectRequiredFields((ObjectInstance) instance);
        return instance;
    }

    private ArrayList getArrayConst(String key) {
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<String, Object> entry : constValues.entrySet()) {
            int lastDotIdx = entry.getKey().lastIndexOf('.');
            int firstBraket = entry.getKey().indexOf('[');
            if (firstBraket < 0)
                continue;
            String lastPathSegment = entry.getKey().substring(lastDotIdx + 1, firstBraket);
            if (lastPathSegment.equals(key)) {
                arrayList.add(entry.getValue());
            }
        }
        return arrayList;
    }

    private Object getPrimitiveConst(String key) {
        for (Map.Entry<String, Object> entry : constValues.entrySet()) {
            if (entry.getKey().endsWith(key)) {
                return entry.getValue();
            }
        }
        return "";
    }

    private Class<?> getInstanceClass(String type) {
        Class<?> klass;
        switch (type) {
            case SchemaKeywords.InstanceTypes.ARRAY:
                klass = arrayInstanceClass;
                break;
            case SchemaKeywords.InstanceTypes.BOOLEAN:
                klass = booleanInstanceClass;
                break;
            case STRING:
                klass = stringInstanceClass;
                break;
            case SchemaKeywords.InstanceTypes.OBJECT:
                klass = objectInstanceClass;
                break;
            case SchemaKeywords.InstanceTypes.NUMBER:
                klass = numericInstanceClass;

                break;
            default:
                throw new JsonParseException(String.format("this type is not supported %s", type));

        }
        return klass;
    }

    private SchemaInstance getProperEnumInstance(JsonElement json, JsonDeserializationContext context, String type, Class<?> klass) {
        if (type.equals(STRING))
            return (EnumInstance<String>) context.deserialize(json, klass);
        else if (type.equals(NUMBER))
            return (EnumInstance<Double>) context.deserialize(json, klass);
        return context.deserialize(json, klass);
    }

    private void setObjectRequiredFields(ObjectInstance objInstance) {
        if (!(objInstance.getRequired() == null || objInstance.getRequired().isEmpty())) {
            for (SchemaInstance instance : objInstance.getProperties().getVals()) {
                instance.setRequiredField(objInstance.getRequired().contains(instance.getKey()));
            }
        }
    }

}
