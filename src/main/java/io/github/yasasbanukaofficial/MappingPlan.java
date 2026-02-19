package io.github.yasasbanukaofficial;

import java.util.ArrayList;
import java.util.List;

public class MappingPlan {
    private final List<PropertyMapping> mappings = new ArrayList<>();
    public static void addMapping(MappingPlan plan, PropertyMapping pm) { plan.mappings.add(pm); }
    public static List<PropertyMapping> getMappings(MappingPlan plan) { return plan.mappings; }
}
