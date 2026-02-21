package io.github.yasasbanukaofficial;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MiniModelMapper {
    private static final Map<Class<?>, Field[]> sCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Field>> dCache = new ConcurrentHashMap<>();
    private static final Map<String, MappingPlan> planCache = new ConcurrentHashMap<>();

    public static <S, D> D map(S source, Class<D> destClass) {
        if (source == null) return null;
        try {
            String key = source.getClass().getName() + "->" + destClass.getName();
            MappingPlan plan = planCache.computeIfAbsent(key, k -> buildMappingPlan(source.getClass(), destClass));
            return executePlan(source, destClass, plan);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static MappingPlan buildMappingPlan(Class<?> sourceClass, Class<?> destClass) {
        Field[] sFields = prepareFields(sourceClass);
        Map<String, Field> dFields = prepareMapFields(destClass);
        MappingPlan plan = new MappingPlan();
        for (Field sField : sFields) {
            Field dField = findMatchingField(sField, dFields);
            if (dField == null) continue;
            MappingStrategy strategy = (srcVal) -> {
                if (srcVal == null)
                    return null;
                return convertType(srcVal, dField);
            };
            MappingPlan.addMapping(plan, new PropertyMapping(sField, dField, strategy));
        }
        return plan;
    }

    private static Field findMatchingField(Field sField, Map<String, Field> destFields) {
        Field exact = destFields.get(sField.getName());
        if (exact != null)
            return exact;
        List<String> sTokens = Tokenizer.tokenize(sField.getName());
        Field best = null;
        int bestScore = 0;
        for (Field dField : destFields.values()) {
            List<String> dTokens = Tokenizer.tokenize(dField.getName());
            int score = 0;
            for (String token : primitiveTokens(sTokens)) {
                if (dTokens.contains(token))
                    score++;
            }
            if (score > bestScore && score > 1) {
                bestScore = score;
                best = dField;
            }
        }
        return best;
    }

    private static List<String> primitiveTokens(List<String> tokens) {
        return tokens;
    }

    private static Object convertType(Object value, Field dField) {
        Class<?> targetType = dField.getType();
        Class<?> sourceType = value.getClass();

        if (value instanceof Collection<?> srcCollection && Collection.class.isAssignableFrom(targetType)) {
            Collection<Object> newCollection = createCollection(targetType);
            Class<?> genericType = extractGenericType(dField);
            for (Object item : srcCollection) {
                if (item == null) continue;
                Object mappedItem = convertSingleValue(item, genericType);
                newCollection.add(mappedItem);
            }
            return newCollection;
        }

        return convertSingleValue(value, targetType);
    }

    private static Object convertSingleValue(Object value, Class<?> targetType) {
        Class<?> sourceType = value.getClass();

        if (targetType.isAssignableFrom(sourceType))
            return value;

        if (targetType.isPrimitive())
            return primitiveConvert(value, targetType);

        if (value instanceof Number num) {
            if (targetType == Integer.class)
                return num.intValue();
            if (targetType == Long.class)
                return num.longValue();
            if (targetType == Double.class)
                return num.doubleValue();
            if (targetType == Float.class)
                return num.floatValue();
        }

        if (value instanceof java.util.Date date && java.util.Date.class.isAssignableFrom(targetType)) {
            return date;
        }

        if (!isJavaLang(sourceType) && !isJavaLang(targetType)) {
            return map(value, targetType);
        }

        throw new RuntimeException("Cannot convert " + sourceType + " to " + targetType);
    }

    private static Object primitiveConvert(Object value, Class<?> primitive) {
        if (value instanceof Number n) {
            if (primitive == int.class)
                return n.intValue();
            if (primitive == long.class)
                return n.longValue();
            if (primitive == double.class)
                return n.doubleValue();
            if (primitive == float.class)
                return n.floatValue();
            if (primitive == short.class)
                return n.shortValue();
            if (primitive == byte.class)
                return n.byteValue();
        }
        if (primitive == boolean.class && value instanceof Boolean b)
            return b;
        if (primitive == char.class && value instanceof Character c)
            return c;
        throw new RuntimeException("Primitive convert failed");
    }

    private static Class<?> extractGenericType(Field field) {
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType pt) {
            Type arg = pt.getActualTypeArguments()[0];
            if (arg instanceof Class<?> clz)
                return clz;
        }
        return Object.class;
    }

    private static Collection<Object> createCollection(Class<?> type) {
        if (type.isInterface()) {
            if (List.class.isAssignableFrom(type))
                return new ArrayList<>();
            if (Set.class.isAssignableFrom(type))
                return new HashSet<>();
        }
        try {
            return (Collection<Object>) type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static <S, D> D executePlan(S source, Class<D> destClass, MappingPlan plan) throws Exception {
        D dest = destClass.getDeclaredConstructor().newInstance();
        for (PropertyMapping pm : MappingPlan.getMappings(plan)) {
            Object srcValue = PropertyMapping.getSourceField(pm).get(source);
            Object mapped = PropertyMapping.getStrategy(pm).map(srcValue);
            PropertyMapping.getDestinationField(pm).set(dest, mapped);
        }
        return dest;
    }

    private static Field[] prepareFields(Class<?> clz) {
        return sCache.computeIfAbsent(clz, c -> {
            Field[] fields = c.getDeclaredFields();
            for (Field f : fields)
                f.setAccessible(true);
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

    private static boolean isJavaLang(Class<?> clz) {
        return clz.getName().startsWith("java");
    }
}