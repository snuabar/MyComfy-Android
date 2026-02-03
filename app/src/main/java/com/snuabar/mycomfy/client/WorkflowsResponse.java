package com.snuabar.mycomfy.client;

import java.util.List;
import java.util.Map;

public class WorkflowsResponse {

    public static class DefaultParameters {
        private int width = 512;
        private int height = 512;
        private int seed = 0;
        private int step = 20;
        private double cfg = 8.0;
        private double upscale_factor = 1.0;
        private int seconds = 5;

        public static final DefaultParameters Default = new DefaultParameters();

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getSeed() {
            return seed;
        }

        public void setSeed(int seed) {
            this.seed = seed;
        }

        public int getStep() {
            return step;
        }

        public void setStep(int step) {
            this.step = step;
        }

        public double getCfg() {
            return cfg;
        }

        public void setCfg(double cfg) {
            this.cfg = cfg;
        }

        public double getUpscale_factor() {
            return upscale_factor;
        }

        public void setUpscale_factor(double upscale_factor) {
            this.upscale_factor = upscale_factor;
        }

        public int getSeconds() {
            return seconds;
        }

        public void setSeconds(int seconds) {
            this.seconds = seconds;
        }
    }

    public static class Workflow {
        public static final String OUTPUT_IMAGE = "image";
        public static final String OUTPUT_VIDEO = "video";
        private String displayName;
        private String outputType;
        private List<String> modelTypes;
        private List<String> excludeModels;
        private DefaultParameters defaultParameters;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getOutputType() {
            return outputType;
        }

        public void setOutputType(String outputType) {
            this.outputType = outputType;
        }

        public List<String> getModelTypes() {
            return modelTypes;
        }

        public void setModelTypes(List<String> modelTypes) {
            this.modelTypes = modelTypes;
        }

        public List<String> getExcludeModels() {
            return excludeModels;
        }

        public void setExcludeModels(List<String> excludeModels) {
            this.excludeModels = excludeModels;
        }

        public DefaultParameters getDefaultParameters() {
            return defaultParameters;
        }

        public void setDefaultParameters(DefaultParameters defaultParameters) {
            this.defaultParameters = defaultParameters;
        }
    }

    private Map<String, Workflow> workflows;

    public Map<String, Workflow> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(Map<String, Workflow> workflows) {
        this.workflows = workflows;
    }
}
