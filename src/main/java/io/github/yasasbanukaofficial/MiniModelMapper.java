package io.github.yasasbanukaofficial;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MiniModelMapper {
    private final Map<Class<?>, Field[]> sCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String, Field>> dCache = new ConcurrentHashMap<>();

    public <S, D> D map(S source, Class<D> destination) {
        try {
            var constructor = destination.getDeclaredConstructor();
            constructor.setAccessible(true);
            D destinationObj = constructor.newInstance();

            Field[] sFields = sCache.computeIfAbsent(source.getClass(), this::prepareFields);
            Map<String, Field> dFields = dCache.computeIfAbsent(destination, this::prepareMapFields);
            for (Field sField : sFields) {
                Field dField = dFields.get(sField.getName());
                if (dField != null) {
                    Class<?> sType = sField.getType();
                    Class<?> dType = dField.getType();

                    if (Iterable.class.isAssignableFrom(sType) || Map.class.isAssignableFrom(sType)) {
                        continue;
                    }

                    if (dType.isAssignableFrom(sType)) {
                        dField.set(destinationObj, sField.get(source));
                    } else {
                        throw new RuntimeException("Type mismatch where source field name " + sField.getName() + " is not matching with the destination field name " + dField.getName() + ", Source Field Type: " + sType + ", Destination Field Type: " + dType);
                    }
                }
            }
            return destinationObj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Field[] prepareFields(Class<?> clz) {
        Field[] fields = clz.getDeclaredFields();
        for (Field field : fields) field.setAccessible(true);
        return fields;
    }

    private Map<String, Field> prepareMapFields(Class<?> clz) {
        Map<String, Field> map = new ConcurrentHashMap<>();
        for (Field field: clz.getDeclaredFields()) {
            field.setAccessible(true);
            map.put(field.getName(), field);
        }
        return map;
    }
}