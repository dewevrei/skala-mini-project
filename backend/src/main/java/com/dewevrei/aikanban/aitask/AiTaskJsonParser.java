package com.dewevrei.aikanban.aitask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dewevrei.aikanban.common.validation.UserInputValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class AiTaskJsonParser {
    private static final Set<String> ROOT_FIELDS = Set.of("tasks");
    private static final Set<String> ITEM_FIELDS = Set.of("title", "description", "priority");
    private final ObjectMapper mapper = new ObjectMapper();

    List<AiTaskItem> parse(String content, String originalTitle) {
        try {
            JsonNode root = mapper.readTree(content);
            requireObjectWithExactFields(root, ROOT_FIELDS);
            JsonNode tasks = root.get("tasks");
            if (tasks == null || !tasks.isArray() || tasks.isEmpty()) invalid("tasks must be nonempty");

            List<AiTaskItem> result = new ArrayList<>();
            for (JsonNode item : tasks) {
                requireObjectWithExactFields(item, ITEM_FIELDS);
                String title = requiredText(item.get("title"), 200);
                String description = requiredText(item.get("description"), Integer.MAX_VALUE);
                JsonNode priorityNode = item.get("priority");
                if (priorityNode == null || !priorityNode.isIntegralNumber() || !priorityNode.canConvertToInt()) {
                    invalid("priority must be an integer");
                }
                int priority = priorityNode.intValue();
                if (priority < 1 || priority > 5) invalid("priority out of range");
                String storedDescription = originalTitle + " - " + description;
                if (UserInputValidator.exceeds(storedDescription, 5000)) invalid("description too long");
                result.add(new AiTaskItem(title, description, priority));
            }
            return List.copyOf(result);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new InvalidAiResponseException("invalid AI response", exception);
        }
    }

    private String requiredText(JsonNode node, int maxCodePoints) {
        if (node == null || !node.isTextual()) invalid("required text missing");
        String value;
        try {
            value = UserInputValidator.normalize(node.textValue());
        } catch (IllegalArgumentException exception) {
            throw new InvalidAiResponseException("invalid text", exception);
        }
        if (value == null || value.isEmpty() || UserInputValidator.exceeds(value, maxCodePoints)) {
            invalid("invalid text length");
        }
        return value;
    }

    private void requireObjectWithExactFields(JsonNode node, Set<String> expected) {
        if (node == null || !node.isObject()) invalid("object required");
        Set<String> actual = new HashSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(expected)) invalid("unexpected object shape");
    }

    private static void invalid(String message) { throw new InvalidAiResponseException(message); }
}
