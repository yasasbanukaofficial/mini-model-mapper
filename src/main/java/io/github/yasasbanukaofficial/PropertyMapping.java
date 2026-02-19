package io.github.yasasbanukaofficial;

import java.lang.reflect.Field;

public class PropertyMapping {
    private final Field sourceField;
    private final Field destinationField;
    private final MappingStrategy strategy;
    public PropertyMapping(Field sourceField, Field destinationField, MappingStrategy strategy) {
        this.sourceField = sourceField;
        this.destinationField = destinationField;
        this.strategy = strategy;
    }
    public static Field getSourceField(PropertyMapping pm) { return pm.sourceField; }
    public static Field getDestinationField(PropertyMapping pm) { return pm.destinationField; }
    public static MappingStrategy getStrategy(PropertyMapping pm) { return pm.strategy; }
}
