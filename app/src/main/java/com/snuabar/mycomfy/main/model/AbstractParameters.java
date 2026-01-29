package com.snuabar.mycomfy.main.model;

import org.json.JSONObject;

import java.io.Serializable;

public abstract class AbstractParameters implements Serializable {
    public abstract String getWorkflow();

    public abstract void setWorkflow(String workflow);

    public abstract String getModel();

    public abstract void setModel(String model);

    public abstract String getPrompt();

    public abstract void setPrompt(String prompt);

    public abstract Integer getSeed();

    public abstract void setSeed(Integer seed);

    public abstract int getImg_width();

    public abstract void setImg_width(int img_width);

    public abstract int getImg_height();

    public abstract void setImg_height(int img_height);

    public abstract int getNum_images();

    public abstract void setNum_images(int num_images);

    public abstract String getStyle();

    public abstract void setStyle(String style);

    public abstract String getNegative_prompt();

    public abstract void setNegative_prompt(String negative_prompt);

    public abstract double getUpscale_factor();

    public abstract void setUpscale_factor(double upscale_factor);

    public abstract int getStep();

    public abstract void setStep(int step);

    public abstract double getCfg();

    public abstract void setCfg(double cfg);

    public abstract long getTimestamp();

    public abstract JSONObject toJson();

    public abstract void loadJson(JSONObject jsonObject);
}
