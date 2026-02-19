package io.github.yasasbanukaofficial;

@FunctionalInterface
public interface MappingStrategy {
    Object map(Object sourceValue) throws Exception;
}