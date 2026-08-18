package com.dewevrei.aikanban.aitask;

import java.util.List;

public interface AiTaskGenerator {
    List<AiTaskItem> generate(String title, String description);
}
