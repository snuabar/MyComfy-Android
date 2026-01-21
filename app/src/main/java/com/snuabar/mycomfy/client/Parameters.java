package com.snuabar.mycomfy.client;

public class Parameters {
    private String prompt;
    private Integer seed;
    private int img_width;
    private int img_height;
    private int num_images;
    private String style;
    private String negative_prompt;

    // Getters and Setters
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
}
