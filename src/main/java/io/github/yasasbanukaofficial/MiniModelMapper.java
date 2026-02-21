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
            if (dField != null && isCompatible(sField.getType(), dField.getType())) {
                MappingStrategy strategy = (srcVal) -> {
                    if (srcVal == null) return null;
                    return convertType(srcVal, dField);
                };
                MappingPlan.addMapping(plan, new PropertyMapping(sField, dField, strategy));
            }
        }
        return plan;
    }

    private static boolean isCompatible(Class<?> source, Class<?> dest) {
        if (dest.isAssignableFrom(source)) return true;
        if (Number.class.isAssignableFrom(source)
                && Number.class.isAssignableFrom(dest))
            return true;

        if (Collection.class.isAssignableFrom(source)
                && Collection.class.isAssignableFrom(dest))
            return true;

        if (!source.getName().startsWith("java")
                && !dest.getName().startsWith("java"))
            return true;

        return false;
    }

    private static Field findMatchingField(Field sField, Map<String, Field> destFields) {
        Field exact = destFields.get(sField.getName());
        if (exact != null) return exact;
        List<String> sTokens = Tokenizer.tokenize(sField.getName());
        for (Field dField : destFields.values()) {
            List<String> dTokens = Tokenizer.tokenize(dField.getName());
            if (!Collections.disjoint(sTokens, dTokens)) return dField;
        }
        return null;
    }

    private static Object convertType(Object value, Field dField) {

        Class<?> targetType = dField.getType();
        Class<?> sourceType = value.getClass();

        if (value instanceof Collection<?> srcCollection
                && Collection.class.isAssignableFrom(targetType)) {

            try {
                Collection<Object> newCollection =
                        new ArrayList<>();

                ParameterizedType generic =
                        (ParameterizedType) dField.getGenericType();

                Class<?> destGenericType =
                        (Class<?>) generic
                                .getActualTypeArguments()[0];

                for (Object item : srcCollection) {

                    if (item == null) continue;

                    Object mappedItem =
                            map(item, destGenericType);

                    newCollection.add(mappedItem);
                }

                return newCollection;

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (targetType.isAssignableFrom(sourceType))
            return value;

        if (!sourceType.getName().startsWith("java")
                && !targetType.getName().startsWith("java")) {

            return map(value, targetType);
        }

        throw new RuntimeException(
                "Cannot convert "
                        + sourceType
                        + " to "
                        + targetType);
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
