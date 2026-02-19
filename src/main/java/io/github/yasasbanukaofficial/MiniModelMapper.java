package io.github.yasasbanukaofficial;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MiniModelMapper {
    private static final Map<Class<?>, Field[]> sCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Field>> dCache = new ConcurrentHashMap<>();
    private static final Map<String, MappingPlan> planCache = new ConcurrentHashMap<>();

    public static <S, D> D map(S source, Class<D> destClass) {
        try {
            String key = source.getClass().getName() + "->" + destClass.getName();
            MappingPlan plan = planCache.computeIfAbsent(key,
                    k -> buildMappingPlan(source.getClass(), destClass));
            return executePlan(source, destClass, plan);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static MappingPlan buildMappingPlan(Class<?> sourceClass, Class<?> destClass) {
        Field[] sFields = prepareFields(sourceClass);
        Map<String, Field> dFields = prepareMapFields(destClass);

        MappingPlan plan = new MappingPlan();

        for (Field sField : sFields) {
            Field dField = findMatchingField(sField, dFields);
            if (dField != null) {
                MappingStrategy strategy = (srcVal) -> {
                    if (srcVal == null) return null;
                    return convertType(srcVal, dField.getType());
                };
                MappingPlan.addMapping(plan, new PropertyMapping(sField, dField, strategy));
            }
        }
        return plan;
    }

    private static Field findMatchingField(Field sField, Map<String, Field> destFields) {
        List<String> sTokens = Tokenizer.tokenize(sField.getName());
        for (Field dField : destFields.values()) {
            List<String> dTokens = Tokenizer.tokenize(dField.getName());
            if (!Collections.disjoint(sTokens, dTokens)) return dField;
        }
        return null;
    }

    private static Object convertType(Object value, Class<?> targetType) {
        Class<?> sourceType = value.getClass();
        if (targetType.isAssignableFrom(sourceType)) return value;

        if (value instanceof Number) {
            Number num = (Number) value;
            if (targetType == Byte.class || targetType == byte.class) return num.byteValue();
            if (targetType == Short.class || targetType == short.class) return num.shortValue();
            if (targetType == Integer.class || targetType == int.class) return num.intValue();
            if (targetType == Long.class || targetType == long.class) return num.longValue();
            if (targetType == Float.class || targetType == float.class) return num.floatValue();
            if (targetType == Double.class || targetType == double.class) return num.doubleValue();
        }

        if (!sourceType.isPrimitive() && !targetType.isPrimitive() && !sourceType.getName().startsWith("java")) {
            return map(value, targetType);
        }

        throw new RuntimeException("Cannot convert " + sourceType + " to " + targetType);
    }

    public static <S, D> D executePlan(S source, Class<D> destClass, MappingPlan plan) throws Exception {
        D dest = destClass.getDeclaredConstructor().newInstance();
        for (PropertyMapping pm : MappingPlan.getMappings(plan)) {
            Object value = PropertyMapping.getStrategy(pm).map(PropertyMapping.getSourceField(pm).get(source));
            PropertyMapping.getDestinationField(pm).set(dest, value);
        }
        return dest;
    }

    private static Field[] prepareFields(Class<?> clz) {
        return sCache.computeIfAbsent(clz, c -> {
            Field[] fields = c.getDeclaredFields();
            for (Field f : fields) f.setAccessible(true);
            return fields;
        });
    }

    private static Map<String, Field> prepareMapFields(Class<?> clz) {
        return dCache.computeIfAbsent(clz, c -> {
            Map<String, Field> map = new ConcurrentHashMap<>();
            for (Field f : c.getDeclaredFields()) {
                f.setAccessible(true);
                map.put(f.getName(), f);
            }
            return map;
        });
    }
}
