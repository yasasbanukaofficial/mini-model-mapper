package io.github.yasasbanukaofficial;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MiniModelMapper {
    private final Map<Class<?>, Field[]> sCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String, Field>> dCache = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = Map.of(
            long.class, Long.class,
            int.class, Integer.class,
            double.class, Double.class,
            float.class, Float.class,
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            short.class, Short.class,
            char.class, Character.class
    );

    public <S, D> D map(S source, Class<D> destination) {
        if (source == null) return null;
        try {
            var constructor = destination.getDeclaredConstructor();
            constructor.setAccessible(true);
            D destinationObj = constructor.newInstance();

            Field[] sFields = sCache.computeIfAbsent(source.getClass(), this::prepareFieldsRecursive);
            Map<String, Field> dFields = dCache.computeIfAbsent(destination, this::prepareMapFieldsRecursive);

            for (Field sField : sFields) {
                Object sValue = sField.get(source);
                if (sValue == null) continue;

                String sName = sField.getName();
                Field dField = dFields.get(sName);

                if (dField == null) {
                    dField = dFields.get(sName + "Id");
                    if (dField != null) {
                        mapFlattenedField(sValue, dField, destinationObj);
                        continue;
                    }
                }

                if (dField != null) {
                    Class<?> sType = sField.getType();
                    Class<?> dType = dField.getType();

                    if (Iterable.class.isAssignableFrom(sType) || Map.class.isAssignableFrom(sType)) {
                        continue;
                    }

                    if (isCompatible(dType, sType)) {
                        dField.set(destinationObj, sValue);
                    } else {
                        throw new RuntimeException("Type mismatch for field '" + sField.getName() +
                                "'. Source: " + sType.getSimpleName() +
                                ", Destination: " + dType.getSimpleName());
                    }
                }
            }
            return destinationObj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mapFlattenedField(Object sValue, Field dField, Object dObj) {
        try {
            Field idField = sValue.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object idValue = idField.get(sValue);
            if (idValue != null && isCompatible(dField.getType(), idValue.getClass())) {
                dField.set(dObj, idValue);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.out.println("Failed to map flattened field. The source object does not contain 'id' field or access was denied: " + e.getMessage());
        }
    }

    private boolean isCompatible(Class<?> target, Class<?> source) {
        if (target.isAssignableFrom(source)) return true;
        if (target.isPrimitive()) {
            return PRIMITIVE_WRAPPER_MAP.get(target) == source;
        } else {
            return PRIMITIVE_WRAPPER_MAP.entrySet().stream()
                    .anyMatch(entry -> entry.getValue().equals(target) && entry.getKey().equals(source));
        }
    }

    private Field[] prepareFieldsRecursive(Class<?> clz) {
        List<Field> allFields = new ArrayList<>();
        Class<?> current = clz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                field.setAccessible(true);
                allFields.add(field);
            }
            current = current.getSuperclass();
        }
        return allFields.toArray(new Field[0]);
    }

    private Map<String, Field> prepareMapFieldsRecursive(Class<?> clz) {
        Map<String, Field> map = new ConcurrentHashMap<>();
        for (Field field : prepareFieldsRecursive(clz)) {
            map.put(field.getName(), field);
        }
        return map;
    }
}