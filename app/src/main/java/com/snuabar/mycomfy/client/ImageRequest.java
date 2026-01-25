package com.snuabar.mycomfy.client;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

// 请求模型
public class ImageRequest {
    private String workflow;
    private String prompt;
    private Integer seed;
    private int img_width = 512;
    private int img_height = 512;
    private int num_images = 1;
    private String style = "realistic";
    private String negative_prompt;

    // 构造函数
    public ImageRequest(String workflow, String prompt, Integer seed, int img_width, int img_height) {
        this.workflow = workflow;
        this.prompt = prompt;
        this.seed = seed;
        this.img_width = img_width;
        this.img_height = img_height;
    }

    // Getters and Setters

    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public Integer getSeed() { return seed; }
    public void setSeed(Integer seed) { this.seed = seed; }

    public int getImg_width() { return img_width; }
    public void setImg_width(int img_width) { this.img_width = img_width; }

    public int getImg_height() { return img_height; }
    public void setImg_height(int img_height) { this.img_height = img_height; }

    public int getNum_images() { return num_images; }
    public void setNum_images(int num_images) { this.num_images = num_images; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getNegative_prompt() { return negative_prompt; }
    public void setNegative_prompt(String negative_prompt) { this.negative_prompt = negative_prompt; }

    public JSONObject toJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt("workflow", getWorkflow());
            jsonObject.putOpt("prompt", getPrompt());
            jsonObject.putOpt("seed", getSeed());
            jsonObject.putOpt("img_width", getImg_width());
            jsonObject.putOpt("img_height", getImg_height());
            jsonObject.putOpt("num_images", getNum_images());
            jsonObject.putOpt("style", getStyle());
            jsonObject.putOpt("negative_prompt", getNegative_prompt());
            return jsonObject;
        } catch (JSONException e) {
            Log.e("ImageRequest", "toJson. exception thrown.", e);
        }
        return null;
    }

    public void loadJson(JSONObject jsonObject) {
        setWorkflow(jsonObject.optString("workflow"));
        setPrompt(jsonObject.optString("prompt"));
        setSeed(jsonObject.optInt("seed"));
        setImg_width(jsonObject.optInt("img_width"));
        setImg_height(jsonObject.optInt("img_height"));
        setNum_images(jsonObject.optInt("num_images"));
        setStyle(jsonObject.optString("style"));
        setNegative_prompt(jsonObject.optString("negative_prompt"));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ImageRequest that = (ImageRequest) o;
        return img_width == that.img_width && img_height == that.img_height && num_images == that.num_images && Objects.equals(workflow, that.workflow) && Objects.equals(prompt, that.prompt) && Objects.equals(seed, that.seed) && Objects.equals(style, that.style) && Objects.equals(negative_prompt, that.negative_prompt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workflow, prompt, seed, img_width, img_height, num_images, style, negative_prompt);
    }
}
