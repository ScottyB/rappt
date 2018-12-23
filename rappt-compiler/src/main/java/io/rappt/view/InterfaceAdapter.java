package io.rappt.view;

import com.google.gson.*;
import java.lang.reflect.Type;

// Copied from Stack Overflow:
// http://stackoverflow.com/questions/4795349/how-to-serialize-a-class-with-an-interface/9550086#9550086
// cc by-sa 3.0 Maciek Makowski Mar 3 '12
// Author says that he considers it as "public domain"

// Adds type field to JSON in order to allow de-serialization of fields with abstract/interface types
final public class InterfaceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {
    public JsonElement serialize(T object, Type interfaceType, JsonSerializationContext context) {
        final JsonObject wrapper = new JsonObject();
        wrapper.addProperty("type", object.getClass().getName());
        wrapper.add("data", context.serialize(object));
        return wrapper;
    }

    public T deserialize(JsonElement elem, Type interfaceType, JsonDeserializationContext context) throws JsonParseException {
        final JsonObject wrapper = (JsonObject) elem;
        final JsonElement typeName = get(wrapper, "type");
        final JsonElement data = get(wrapper, "data");
        final Type actualType = typeForName(typeName);
        return context.deserialize(data, actualType);
    }

    private Type typeForName(final JsonElement typeElem) {
        try {
            return Class.forName(typeElem.getAsString());
        } catch (ClassNotFoundException e) {
            throw new JsonParseException(e);
        }
    }

    private JsonElement get(final JsonObject wrapper, String memberName) {
        final JsonElement elem = wrapper.get(memberName);
        if (elem == null) throw new JsonParseException("no '" + memberName + "' member found in what was expected to be an interface wrapper");
        return elem;
    }
}