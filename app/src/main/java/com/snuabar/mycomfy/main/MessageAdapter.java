package com.snuabar.mycomfy.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.client.Parameters;
import com.snuabar.mycomfy.common.Common;
import com.snuabar.mycomfy.databinding.LayoutReceivedMsgItemBinding;
import com.snuabar.mycomfy.databinding.LayoutSentMsgItemBinding;
import com.snuabar.mycomfy.main.data.AbstractMessageModel;
import com.snuabar.mycomfy.main.model.MessageModel;
import com.snuabar.mycomfy.main.model.ReceivedMessageModel;
import com.snuabar.mycomfy.main.model.SentMessageModel;
import com.snuabar.mycomfy.main.model.UpscaleSentMessageModel;
import com.snuabar.mycomfy.utils.ImageUtils;
import com.snuabar.mycomfy.utils.ThumbnailCacheManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private final static int VIEW_TYPE_SENT = 0;
    private final static int VIEW_TYPE_RECEIVED = 1;

    private final Handler mHandler;
    private List<AbstractMessageModel> models;
    private final Map<String, Integer> idToIndexMap;
    private final OnElementClickListener listener;
    private WeakReference<RecyclerView> mRecyclerViewRef = null;
    private final Set<Integer> selections;
    private boolean isEditMode = false;
    private final Set<String> matchedIDs;
    private final Map<Integer, long[]> progressMap;

    public MessageAdapter(OnElementClickListener listener) {
        this.mHandler = new Handler(Looper.getMainLooper());
        this.listener = listener;
        this.idToIndexMap = new HashMap<>();
        this.selections = new HashSet<>();
        this.models = new ArrayList<>();
        updateIdToIndexMap();
        this.matchedIDs = new HashSet<>();
        this.progressMap = new HashMap<>();
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            return new SentViewHolder(LayoutSentMsgItemBinding.inflate(inflater, parent, false));
        }
        return new ReceivedViewHolder(LayoutReceivedMsgItemBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        if (!matchedIDs.isEmpty() && !matchedIDs.contains(models.get(position).getId())) {
            holder.itemView.getLayoutParams().height = 0;
            return;
        }
        holder.itemView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

        holder.tvDate.setText(Common.formatTimestamp(models.get(position).getUTCTimestamp()));
        if (selections.contains(position)) {
            holder.itemView.setBackgroundColor(holder.itemView.getResources().getColor(R.color.black_overlay, null));
        } else {
            holder.itemView.setBackgroundResource(R.drawable.ripple_effect);
        }

        AbstractMessageModel model = models.get(position);
        holder.setTip(model.getStatusResourceString(holder.itemView.getContext()), model.getCode(), model.getMessage());

        if (holder instanceof SentViewHolder) {
            onBindSentViewHolder((SentViewHolder) holder, position);
        } else if (holder instanceof ReceivedViewHolder) {
            onBindReceivedViewHolder((ReceivedViewHolder) holder, position);
        }
    }

    public void onBindSentViewHolder(@NonNull SentViewHolder holder, int position) {
        SentMessageModel model = (SentMessageModel) models.get(position);

        Parameters param = model.getParameters();
        if (param != null) {
            if (model instanceof UpscaleSentMessageModel) {
                holder.binding.layoutImages.setVisibility(View.GONE);
                holder.binding.textView.setText(String.format(Locale.getDefault(),
                        "放大%.01f倍",
                        param.getUpscale_factor()
                ));
                int destWidth = Common.calcScale(model.getParameters().getImg_width(), param.getUpscale_factor());
                int destHeight = Common.calcScale(model.getParameters().getImg_height(), param.getUpscale_factor());
                holder.binding.textView0.setText(String.format(Locale.getDefault(),
                        "%dx%d -> %dx%d",
                        param.getImg_width(), param.getImg_height(),
                        destWidth, destHeight
                ));
                holder.binding.layoutImageView.setVisibility(View.VISIBLE);
                if (ImageUtils.getThumbnail(model)) {
                    Bitmap thumb = ThumbnailCacheManager.Companion.getInstance().getThumbnail(model.getThumbnailFile().getAbsolutePath());
                    holder.binding.imageView.setImageBitmap(thumb);
                    if (thumb == null) {
                        ThumbnailCacheManager.Companion.getInstance().getThumbnailAsync(
                                model.getThumbnailFile().getAbsolutePath(),
                                model.getId(), this::onThumbnailLoad);
                    }
                } else {
                    holder.binding.imageView.setImageBitmap(null);
                    float width = holder.itemView.getContext().getResources().getDimension(R.dimen.thumbnail_width);
                    float height = holder.itemView.getContext().getResources().getDimension(R.dimen.thumbnail_height);
                    ImageUtils.makeThumbnailAsync(model, width, height, this::onThumbnailMake);
                }
            } else {
                holder.binding.layoutImageView.setVisibility(View.GONE);
                holder.binding.textView.setText(param.getPrompt());
                if (model.isI2I()) {
                    holder.binding.layoutImages.setVisibility(View.VISIBLE);
                    displayI2ISentImages(holder, model);
                } else {
                    holder.binding.layoutImages.setVisibility(View.GONE);
                }
                displayDetailedParams(holder, model);
            }
        }
        holder.binding.btnResent.setVisibility(MessageModel.STATUS_FAILED.equals(model.getStatus()) && !isEditMode ? View.VISIBLE : View.GONE);
    }

    public void onBindReceivedViewHolder(@NonNull ReceivedViewHolder holder, int position) {
        ReceivedMessageModel model = (ReceivedMessageModel) models.get(position);
        boolean upscaled = model.getParameters().getUpscale_factor() > 1.0;
        if (ImageUtils.getThumbnail(model)) {
            Bitmap thumb = ThumbnailCacheManager.Companion.getInstance().getThumbnail(model.getThumbnailFile().getAbsolutePath());
            if (thumb != null) {
                holder.binding.imageView.setImageBitmap(thumb);
            } else {
                ThumbnailCacheManager.Companion.getInstance().getThumbnailAsync(
                        model.getThumbnailFile().getAbsolutePath(),
                        model.getId(), this::onThumbnailLoad);
            }
            holder.binding.textView.setVisibility(View.VISIBLE);
            if (upscaled) {
                holder.binding.tvScaleFactor.setVisibility(View.VISIBLE);
                holder.binding.tvScaleFactor.setText(String.format(Locale.getDefault(),
                        "%.01fx",
                        model.getParameters().getUpscale_factor()));
            } else {
                holder.binding.tvScaleFactor.setVisibility(View.GONE);
            }
            int[] size = model.getImageSize();
            holder.binding.textView.setText(String.format(Locale.getDefault(), "%d x %d", size[0], size[1]));
        } else {
            holder.binding.tvScaleFactor.setVisibility(View.GONE);
            holder.binding.textView.setVisibility(View.INVISIBLE);
            holder.binding.imageView.setImageBitmap(null);
            float width = holder.itemView.getContext().getResources().getDimension(R.dimen.thumbnail_width);
            float height = holder.itemView.getContext().getResources().getDimension(R.dimen.thumbnail_height);
            ImageUtils.makeThumbnailAsync(model, width, height, this::onThumbnailMake);
        }
        holder.binding.ivPlay.setVisibility(model.isVideo() ? View.VISIBLE : View.GONE);
        holder.binding.btnInterrupt.setVisibility(model.getCode() == 200 || model.getInterruptionFlag() || isEditMode ? View.GONE : View.VISIBLE);
        holder.binding.tvDateCompletion.setText(model.isFinished() ? Common.formatTimestamp(model.getUTCTimestampCompletion()) : "");
        holder.binding.btnSave.setVisibility(isEditMode ? View.INVISIBLE : View.VISIBLE);
        holder.binding.btnShare.setVisibility(isEditMode ? View.INVISIBLE : View.VISIBLE);
        holder.binding.layoutUpscale.setVisibility(canBeUpscaled(model) ? View.VISIBLE : View.GONE);
        updateProgress(holder.binding.pgsBar, position, model);
    }

    private void displayI2ISentImages(SentViewHolder holder, SentMessageModel model) {
        File[] imageFiles = model.getParameters().getImageFiles();
        if (imageFiles != null) {
            Context context = holder.itemView.getContext();
            ThumbnailCacheManager thumbnailCacheManager = ThumbnailCacheManager.Companion.getInstance();
            ImageView[] imageViews = new ImageView[]{holder.binding.imageView1, holder.binding.imageView2, holder.binding.imageView3};
            for (int i = 0; i < imageFiles.length; i++) {
                File imageFile = imageFiles[i];
                if (imageFile != null) {
                    File thumbnailFile = ImageUtils.getThumbnailFileInCacheDir(context, imageFile);
                    Bitmap thumb = thumbnailCacheManager.getThumbnail(thumbnailFile.getAbsolutePath());
                    imageViews[i].setImageBitmap(thumb);
                    if (thumb == null) {
                        thumbnailCacheManager.getThumbnailAsync(
                                thumbnailFile.getAbsolutePath(),
                                model.getId(), this::onThumbnailLoad);
                    }
                } else {
                    imageViews[i].setImageBitmap(null);
                }
            }
        }
    }

    private boolean canBeUpscaled(ReceivedMessageModel model) {
        boolean upscaled = model.getParameters().getUpscale_factor() > 1.0;
        return !model.isI2I() && !model.isVideo() && model.getCode() == 200 && !model.getInterruptionFlag() && !upscaled && !isEditMode;
    }

    private void onThumbnailMake(AbstractMessageModel model) {
        Integer index = idToIndexMap.get(model.getId());
        if (index != null && index >= 0 && index < models.size()) {
            mHandler.post(() -> notifyItemChanged(index));
        }
    }

    private void onThumbnailLoad(Bitmap bmp, Object passBack) {
        if (passBack instanceof String) {
            Integer index = idToIndexMap.get(passBack);
            if (index != null && index >= 0 && index < models.size()) {
                mHandler.post(() -> notifyItemChanged(index));
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerViewRef = new WeakReference<>(recyclerView);
        if (getItemCount() > 0) {
            recyclerView.scrollToPosition(getItemCount() - 1);
        }
    }

    private RecyclerView getRecyclerView() {
        return mRecyclerViewRef != null ? mRecyclerViewRef.get() : null;
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (models.get(position) instanceof SentMessageModel) {
            return VIEW_TYPE_SENT;
        }
        if (models.get(position) instanceof ReceivedMessageModel) {
            return VIEW_TYPE_RECEIVED;
        }
        return super.getItemViewType(position);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<AbstractMessageModel> list) {
        models = Objects.requireNonNullElseGet(list, ArrayList::new);

        updateIdToIndexMap();

        notifyDataSetChanged();

        mHandler.post(() -> {
            // 滚动到最底总
            if (getRecyclerView() != null) {
                getRecyclerView().scrollToPosition(getItemCount() - 1);
            }
        });
    }

    private void updateIdToIndexMap() {
        idToIndexMap.clear();
        for (int i = 0; i < models.size(); i++) {
            idToIndexMap.put(models.get(i).getId(), i);
        }
    }

    public void notifyItemAdded(int index) {
        updateIdToIndexMap();

        mHandler.post(() -> {
            // 插入项
            notifyItemInserted(index);

            if (getRecyclerView() != null) {
                getRecyclerView().scrollToPosition(getItemCount() - 1);
            }
        });

    }

    public void notifyItemDeleted(int index) {
        notifyItemRemoved(index);
        updateIdToIndexMap();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        selections.clear();
        notifyDataSetChanged();
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public void toggleSelection(int position) {
        if (selections.contains(position)) {
            selections.remove(position);
        } else {
            selections.add(position);
        }
        notifyItemChanged(position);
    }

    /**
     * 给搜索用
     *
     * @param ids 不在这个列表中的项会被隐藏。
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setMatchedIDs(Set<String> ids) {
        if (matchedIDs.isEmpty() && (ids == null || ids.isEmpty())) {
            return;
        }
        matchedIDs.clear();
        if (ids != null) {
            matchedIDs.addAll(ids);
        }
        notifyDataSetChanged();
    }

    public ArrayList<Integer> getSelectedIndices() {
        return new ArrayList<>(selections);
    }

    private void displayDetailedParams(SentViewHolder holder, AbstractMessageModel model) {
        Parameters param = model.getParameters();
        if (model.isI2I()) {
            holder.binding.textView0.setText(String.format(Locale.getDefault(),
                    "%s\n%s\n%s %d %.01f %.01f",
                    param.getWorkflow(),
                    param.getModel(),
                    param.getSeed(),
                    param.getStep(), param.getCfg(),
                    param.getMegapixels()
            ));
        } else if (model.isVideo()){
            holder.binding.textView0.setText(String.format(Locale.getDefault(),
                    "%s\n%s\n%dx%d %s %d %.01f %.01f %s",
                    param.getWorkflow(),
                    param.getModel(),
                    param.getImg_width(), param.getImg_height(),
                    param.getSeed(),
                    param.getStep(), param.getCfg(),
                    param.getUpscale_factor(),
                    model.getParameters().getSeconds() + "s"
            ));
        } else {
            holder.binding.textView0.setText(String.format(Locale.getDefault(),
                    "%s\n%s\n%dx%d %s %d %.01f %.01f",
                    param.getWorkflow(),
                    param.getModel(),
                    param.getImg_width(), param.getImg_height(),
                    param.getSeed(),
                    param.getStep(), param.getCfg(),
                    param.getUpscale_factor()
            ));
        }
    }

    public void notifyItemProgress(int index, long max, long current) {
        if (max > current) {
            progressMap.put(index, new long[]{max, current});
        } else {
            progressMap.remove(index);
        }
        notifyItemChanged(index);
    }

    private void updateProgress(LinearProgressIndicator pgs, int position, AbstractMessageModel model) {
        long[] progress = progressMap.get(position);
        if (progress != null && progress.length == 2) {
            pgs.setVisibility(View.VISIBLE);
            pgs.setIndeterminate(false);
            pgs.setMax((int) progress[0]);
            pgs.setProgress((int) progress[1], true);
        } else {
            pgs.setIndeterminate(true);
            pgs.setVisibility(model.isFinished() ? View.INVISIBLE : View.VISIBLE);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvTip;
        public TextView tvDate;
        private final float[] downLocation = new float[2];
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setLongClickable(true);
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_LONG_CLICK, downLocation, null);
                }
                return true;
            });
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_NONE, null, null);
                }
            });// 设置触摸监听器
            itemView.setOnTouchListener(new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {// 记录按下时的坐标和时间
                        downLocation[0] = event.getX();
                        downLocation[1] = event.getY();
                    }
                    return false;
                }
            });
        }

        void setTip(String status, int code, String message) {
            if (tvTip != null) {
                boolean isErr = MessageModel.STATUS_FAILED.equals(status) || ((code < 200 || code > 299) && code != 0);
                tvTip.setText(isErr ? message : status);
                tvTip.setTextColor(isErr ?
                        itemView.getResources().getColor(android.R.color.holo_red_light, null) :
                        itemView.getResources().getColor(R.color.gray_83, null));
            }
        }
    }

    public class SentViewHolder extends ViewHolder {
        LayoutSentMsgItemBinding binding;

        public SentViewHolder(@NonNull LayoutSentMsgItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.tvDate = binding.tvDate;
            this.tvTip = binding.tvTip;

            binding.btnResent.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_RESENT, null, null);
                }
            });

            binding.imageView1.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_I2I_IMAGES, null, 0);
                }
            });

            binding.imageView2.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_I2I_IMAGES, null, 1);
                }
            });

            binding.imageView3.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_I2I_IMAGES, null, 2);
                }
            });
        }
    }

    public class ReceivedViewHolder extends ViewHolder {
        LayoutReceivedMsgItemBinding binding;

        public ReceivedViewHolder(@NonNull LayoutReceivedMsgItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.tvDate = binding.tvDate;
            this.tvTip = binding.tvTip;
            binding.btnInterrupt.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_INTERRUPT, null, null);
                }
            });
            binding.btnSave.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_SAVE, null, null);
                }
            });
            binding.btnShare.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_SHARE, null, null);
                }
            });
            binding.btnX2.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_X2, null, null);
                }
            });
            binding.btnX4.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_X4, null, null);
                }
            });
            binding.btnXN.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_XN, null, null);
                }
            });
        }
    }

    public interface OnElementClickListener {
        int OPE_NONE = -1;
        int OPE_LONG_CLICK = 0;
        int OPE_INTERRUPT = 1;
        int OPE_SAVE = 2;
        int OPE_SHARE = 3;
        int OPE_RESENT = 4;
        int OPE_X2 = 5;
        int OPE_X4 = 6;
        int OPE_XN = 7;
        int OPE_I2I_IMAGES = 8;
        void onClick(View view, int index, int ope, float[] downLocation, Object obj);
    }
}
