package ch.kup.foodnomads.acceptance.dsl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Farley-style DSL parameters: {@code "name: value"} strings, each declared as required or
 * optional with a default. Typos and missing values fail the test right at the DSL call.
 */
public final class Params {

    public record Param(String name, boolean required, String defaultValue) {
    }

    private final Map<String, String> values;

    private Params(Map<String, String> values) {
        this.values = values;
    }

    public static Param required(String name) {
        return new Param(name, true, null);
    }

    public static Param optional(String name, String defaultValue) {
        return new Param(name, false, defaultValue);
    }

    public static Params parse(String[] args, Param... declared) {
        Map<String, Param> declaredByName = Arrays.stream(declared)
                .collect(Collectors.toMap(Param::name, p -> p));

        Map<String, String> given = new HashMap<>();
        for (String arg : args) {
            int colon = arg.indexOf(':');
            if (colon < 0) {
                throw new IllegalArgumentException("Expected 'name: value' but got '" + arg + "'");
            }
            String name = arg.substring(0, colon).trim();
            if (!declaredByName.containsKey(name)) {
                throw new IllegalArgumentException(
                        "Unknown parameter '" + name + "', expected one of " + declaredByName.keySet());
            }
            if (given.put(name, arg.substring(colon + 1).trim()) != null) {
                throw new IllegalArgumentException("Parameter '" + name + "' given twice");
            }
        }

        Map<String, String> values = new HashMap<>();
        for (Param param : declared) {
            String value = given.get(param.name());
            if (value == null && param.required()) {
                throw new IllegalArgumentException("Missing required parameter '" + param.name() + "'");
            }
            values.put(param.name(), value != null ? value : param.defaultValue());
        }
        return new Params(values);
    }

    public String value(String name) {
        if (!values.containsKey(name)) {
            throw new IllegalArgumentException("Parameter '" + name + "' was not declared");
        }
        return values.get(name);
    }
}
