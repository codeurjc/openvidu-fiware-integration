package io.openvidu.fiware.integration.models.orion;

import com.google.gson.*;
import io.openvidu.fiware.integration.commons.GSMACommons;
import io.openvidu.fiware.integration.commons.JsonManager;
import io.openvidu.fiware.integration.commons.MediaSource;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * JsonSerializer and JsonUnserializer for Orion Entity {@link MediaEvent}
 */
public class MediaEventJsonManager extends JsonManager<MediaEvent> {
    private static final Logger log = getLogger(MediaEventJsonManager.class);

    private static JsonElement serializeMediaSource(MediaSource ms, JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();

        for (Field f : MediaSource.class.getDeclaredFields()) {
            if (!f.getName().equals("serialVersionUID")) {
                try {
                    f.setAccessible(true);
                    if (f.getType() == MediaSource.class) {
                        Object obj = null;
                        obj = f.get(ms);
                        if (obj != null) {
                            jsonObject.add(f.getName(), serializeMediaSource((MediaSource) obj, context));
                        }

                    } else {
                        Object obj = null;
                        obj = f.get(ms);
                        if (obj != null) {
                            jsonObject.add(f.getName(), context.serialize(obj));
                        }
                    }

                } catch (IllegalAccessException iae) {
                    log.debug("do something");
                }
            }
        }

        return jsonObject;

    }

    private static boolean set(Object object, String fieldName, Object fieldValue) {
        Class<?> clazz = object.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(object, fieldValue);
                return true;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return false;
    }

    @Override
    public JsonElement serialize(MediaEvent event, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        // event elements

        for (Field f : MediaEvent.class.getDeclaredFields()) {
            if (!(f.getName().equals("serialVersionUID") || f.getName().equals("TYPE"))) {
                try {
                    f.setAccessible(true);
                    if (f.getType() == String.class) {
                        Object obj = f.get(event);
                        if (obj != null) {
                            jsonObject.add(f.getName(), context.serialize(obj));
                        }
                    } else if (f.getType() == String[].class) {
                        Object obj = f.get(event);
                        if (obj != null) {
                            jsonObject.add(f.getName(), context.serialize(obj));
                        }
                    } else if (f.getType() == Float.class) {
                        Object obj = f.get(event);
                        if (obj != null) {
                            jsonObject.add(f.getName(), context.serialize(obj));
                        }
                    } else if (f.getType() == Boolean.class) {
                        Object obj = f.get(event);
                        if (obj != null) {
                            jsonObject.add(f.getName(), context.serialize(obj));
                        }
                    } else if (f.getType() == MediaSource.class) {
                        Object obj = f.get(event);
                        if (obj != null) {
                            jsonObject.add(f.getName(), serializeMediaSource((MediaSource) obj, context));
                        }
                    } else {
                        Object so = f.get(event);

                        if (so instanceof String) {
                            jsonObject.addProperty(f.getName(), (String) so);
                        } else if (so instanceof JsonElement) {
                            jsonObject.add(f.getName(), (JsonElement) so);
                        } else {
                            for (Field sf : f.getType().getDeclaredFields()) {
                                if (!(sf.getName().equals("serialVersionUID") || sf.getName().equals("TYPE"))) {
                                    sf.setAccessible(true);
                                    Object obj = sf.get(so);
                                    if (obj != null) {
                                        jsonObject.add(sf.getName(), context.serialize(obj));
                                    }
                                }
                            }
                        }
                    }

                } catch (IllegalAccessException iae) {
                    log.debug("do something");
                }
            }
        }

        return jsonObject;
    }

    @Override
    public MediaEvent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        // MediaEvent to return
        MediaEvent event = new MediaEvent();

        // JsonObject
        final JsonObject jsonObject = json.getAsJsonObject();

        // for each element of the Json
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            Object selected = null;
            Class<?> clazz;
            try {
                if (isFieldInClass(key, MediaEvent.class)) {
                    selected = event;
                    clazz = MediaEvent.class.getDeclaredField(key).getType();
                } else if (isFieldInClass(key, GSMACommons.class)) {
                    selected = event._gsmaCommons;
                    clazz = GSMACommons.class.getDeclaredField(key).getType();
                } else if (isFieldInClass(key, MediaSource.class)) {
                    clazz = MediaSource.class.getDeclaredField(key).getType();
                    selected = event.mediasource;
                } else {
                    throw new JsonParseException("Field not found: " + key);
                }
            } catch (NoSuchFieldException e) {
                throw new JsonParseException("NoSuchFieldException in any class: " + key);
            }
            try {
                set(selected, key, context.deserialize(entry.getValue(), clazz));
            } catch (IllegalStateException ise) {
                throw new JsonParseException("IllegalStateException in set: " + key + " value: " + entry.getValue());
            }
        }

        return event;
    }

    private boolean isFieldInClass(String field, Class<?> clazz) {
        try {
            clazz.getDeclaredField(field);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
