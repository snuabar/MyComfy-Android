package com.snuabar.mycomfy.client;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

// 请求模型
public class QueueRequest {
    private String workflow;
    private String model;
    private String prompt;
    private String seed;
    private int img_width;
    private int img_height;
    private int num_images = 1;
    private String style = "realistic";
    private String negative_prompt = null;
    private double upscale_factor;
    private int step;
    private double cfg;
    private int seconds;
    private double megapixels;
    private String[] images;
    private File[] imageFiles;

    // 构造函数
    public QueueRequest(String workflow, String model, String prompt, String seed, int img_width, int img_height, int step, double cfg, double upscale_factor) {
        this.workflow = workflow;
        this.model = model;
        this.prompt = prompt;
        this.seed = seed;
        this.img_width = img_width;
        this.img_height = img_height;
        this.upscale_factor = upscale_factor;
        this.step = step;
        this.cfg = cfg;
    }

    public QueueRequest(Parameters parameters) {
        JSONObject object = parameters.toJson();
        loadJson(object);
    }
    // Getters and Setters

    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSeed() {
        return seed;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }

    public int getImg_width() {
        return img_width;
    }

    public void setImg_width(int img_width) {
        this.img_width = img_width;
    }

    public int getImg_height() {
        return img_height;
    }

    public void setImg_height(int img_height) {
        this.img_height = img_height;
    }

    public int getNum_images() {
        return num_images;
    }

    public void setNum_images(int num_images) {
        this.num_images = num_images;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getNegative_prompt() {
        return negative_prompt;
    }

    public void setNegative_prompt(String negative_prompt) {
        this.negative_prompt = negative_prompt;
    }

    public double getUpscale_factor() {
        return upscale_factor;
    }

    public void setUpscale_factor(double upscale_factor) {
        this.upscale_factor = upscale_factor;
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

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public int getSeconds() {
        return seconds;
    }

    public double getMegapixels() {
        return megapixels;
    }

    public void setMegapixels(double megapixels) {
        this.megapixels = megapixels;
    }

    public String[] getImages() {
        return images;
    }

    public void setImages(String[] images) {
        this.images = images;
    }

    public File[] getImageFiles() {
        return imageFiles;
    }

    public void setImageFiles(File[] imageFiles) {
        this.imageFiles = imageFiles;
    }

    public boolean fetchingImagesIsNeeded() {
        if (imageFiles != null) {
            if (images == null || imageFiles.length != images.length) {
                return true;
            }

            return (imageFiles[0] != null && images[0] == null) ||
                    (imageFiles[1] != null && images[1] == null) ||
                    (imageFiles[2] != null && images[2] == null);
        }

        return false;
    }

    public JSONObject toJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt("workflow", getWorkflow());
            jsonObject.putOpt("model", getModel());
            jsonObject.putOpt("prompt", getPrompt());
            jsonObject.putOpt("seed", getSeed());
            jsonObject.putOpt("img_width", getImg_width());
            jsonObject.putOpt("img_height", getImg_height());
            jsonObject.putOpt("num_images", getNum_images());
            jsonObject.putOpt("style", getStyle());
            jsonObject.putOpt("negative_prompt", getNegative_prompt());
            jsonObject.putOpt("upscale_factor", getUpscale_factor());
            jsonObject.putOpt("step", getStep());
            jsonObject.putOpt("cfg", getCfg());
            jsonObject.putOpt("seconds", getSeconds());
            jsonObject.putOpt("megapixels", getMegapixels());
            String[] images = getImages();
            if (images != null) {
                if (images.length > 0) {
                    jsonObject.putOpt("image1", images[0]);
                }
                if (images.length > 1) {
                    jsonObject.putOpt("image2", images[1]);
                }
                if (images.length > 2) {
                    jsonObject.putOpt("image2", images[2]);
                }
            }
            File[] imageFiles = getImageFiles();
            if (imageFiles != null) {
                if (imageFiles.length > 0) {
                    jsonObject.putOpt("imageFile1", imageFiles[0]);
                }
                if (imageFiles.length > 1) {
                    jsonObject.putOpt("imageFile2", imageFiles[1]);
                }
                if (imageFiles.length > 2) {
                    jsonObject.putOpt("imageFile3", imageFiles[2]);
                }
            }
            return jsonObject;
        } catch (JSONException e) {
            Log.e("ImageRequest", "toJson. exception thrown.", e);
        }
        return null;
    }

    public void loadJson(JSONObject jsonObject) {
        setWorkflow(jsonObject.optString("workflow"));
        setModel(jsonObject.optString("model", null));
        setPrompt(jsonObject.optString("prompt", null));
        setSeed(jsonObject.optString("seed"));
        setImg_width(jsonObject.optInt("img_width"));
        setImg_height(jsonObject.optInt("img_height"));
        setNum_images(jsonObject.optInt("num_images"));
        setStyle(jsonObject.optString("style"));
        setNegative_prompt(jsonObject.optString("negative_prompt", null));
        setUpscale_factor(jsonObject.optDouble("upscale_factor"));
        setStep(jsonObject.optInt("step"));
        setCfg(jsonObject.optDouble("cfg"));
        setSeconds(jsonObject.optInt("seconds"));
        setMegapixels(jsonObject.optInt("megapixels"));
        String[] images = new String[3];
        if (jsonObject.has("image1")) {
            images[0] = jsonObject.optString("image1");
        }
        if (jsonObject.has("image2")) {
            images[1] = jsonObject.optString("image2");
        }
        if (jsonObject.has("image3")) {
            images[2] = jsonObject.optString("image3");
        }
        setImages(images);
        File[] imageFiles = new File[3];
        if (jsonObject.has("imageFile1")) {
            imageFiles[0] = new File(jsonObject.optString("imageFile1"));
        }
        if (jsonObject.has("imageFile2")) {
            imageFiles[1] = new File(jsonObject.optString("imageFile2"));
        }
        if (jsonObject.has("imageFile3")) {
            imageFiles[2] = new File(jsonObject.optString("imageFile3"));
        }
        setImageFiles(imageFiles);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QueueRequest that = (QueueRequest) o;
        return img_width == that.img_width && img_height == that.img_height && num_images == that.num_images && Double.compare(upscale_factor, that.upscale_factor) == 0 && step == that.step && Double.compare(cfg, that.cfg) == 0 && seconds == that.seconds && Double.compare(megapixels, that.megapixels) == 0 && Objects.equals(workflow, that.workflow) && Objects.equals(model, that.model) && Objects.equals(prompt, that.prompt) && Objects.equals(seed, that.seed) && Objects.equals(style, that.style) && Objects.equals(negative_prompt, that.negative_prompt) && Objects.deepEquals(images, that.images) && Objects.deepEquals(imageFiles, that.imageFiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workflow, model, prompt, seed, img_width, img_height, num_images, style, negative_prompt, upscale_factor, step, cfg, seconds, megapixels, Arrays.hashCode(images), Arrays.hashCode(imageFiles));
    }
}
