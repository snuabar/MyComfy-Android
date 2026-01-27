package com.snuabar.mycomfy.client;

import java.util.List;
import java.util.Map;

public class WorkflowsResponse {
    private Map<String, List<String>> workflows;

    public Map<String, List<String>> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(Map<String, List<String>> workflows) {
        this.workflows = workflows;
    }
}
