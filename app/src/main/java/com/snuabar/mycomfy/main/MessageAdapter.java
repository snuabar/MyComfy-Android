package com.snuabar.mycomfy.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
import com.snuabar.mycomfy.utils.VideoUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    public MessageAdapter(OnElementClickListener listener) {
        this.mHandler = new Handler(Looper.getMainLooper());
        this.listener = listener;
        this.idToIndexMap = new HashMap<>();
        this.selections = new HashSet<>();
        this.models = new ArrayList<>();
        updateIdToIndexMap();
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
        holder.tvDate.setText(Common.formatTimestamp(models.get(position).getUTCTimestamp()));
        if (selections.contains(position)) {
            holder.itemView.setBackgroundColor(holder.itemView.getResources().getColor(R.color.black_overlay, null));
        } else {
            holder.itemView.setBackgroundResource(R.drawable.ripple_effect);
        }

        AbstractMessageModel model = models.get(position);
        holder.setTip(model.getStatus(), model.getCode(), model.getMessage());

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
                    holder.binding.imageView.setImageBitmap(BitmapFactory.decodeFile(model.getThumbnailFile().getAbsolutePath()));
                } else {
                    holder.binding.imageView.setImageBitmap(null);
                    float width = holder.itemView.getContext().getResources().getDimension(R.dimen.thumbnail_width);
                    float height = holder.itemView.getContext().getResources().getDimension(R.dimen.thumbnail_height);
                    ImageUtils.makeThumbnailAsync(model, width, height, this::onThumbnailMake);
                }
            } else {
                holder.binding.layoutImageView.setVisibility(View.GONE);
                holder.binding.textView.setText(param.getPrompt());
                holder.binding.textView0.setText(String.format(Locale.getDefault(),
                        "%s\n%s\n%dx%d %d %d %.01f %.01f %s",
                        param.getWorkflow(),
                        param.getModel(),
                        param.getImg_width(), param.getImg_height(),
                        param.getSeed(),
                        param.getStep(), param.getCfg(),
                        param.getUpscale_factor(),
                        (model.isVideo() ? model.getParameters().getSeconds() + "s" : "")
                ));
            }
        }
        holder.binding.btnResent.setVisibility(model.getMessage() != null && !isEditMode ? View.VISIBLE : View.GONE);
    }

    public void onBindReceivedViewHolder(@NonNull ReceivedViewHolder holder, int position) {
        ReceivedMessageModel model = (ReceivedMessageModel) models.get(position);
        boolean upscaled = model.getParameters().getUpscale_factor() > 1.0;
        if (ImageUtils.getThumbnail(model)) {
            holder.binding.imageView.setImageBitmap(BitmapFactory.decodeFile(model.getThumbnailFile().getAbsolutePath()));
            holder.binding.textView.setVisibility(View.VISIBLE);
            if (upscaled) {
                holder.binding.tvScaleFactor.setVisibility(View.VISIBLE);
                holder.binding.tvScaleFactor.setText(String.format(Locale.getDefault(),
                        "%.01fx",
                        model.getParameters().getUpscale_factor()));
            } else {
                holder.binding.tvScaleFactor.setVisibility(View.GONE);
            }
            int[] size;
            if (model.isVideo()) {
                VideoUtils.VideoSize videoSize = VideoUtils.INSTANCE.getVideoSize(model.getImageFile());
                size = new int[]{videoSize.getWidth(), videoSize.getHeight()};
            } else {
                size = ImageUtils.getImageSize(model.getImageFile());
            }
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
        holder.binding.layoutUpscale.setVisibility(!model.isVideo() && model.getCode() == 200 && !model.getInterruptionFlag() && !upscaled && !isEditMode ? View.VISIBLE : View.GONE);
    }

    private void onThumbnailMake(AbstractMessageModel model) {
        Integer index = idToIndexMap.get(model.getId());
        if (index != null && index >= 0 && index < models.size()) {
            mHandler.post(() -> notifyItemChanged(index));
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
        if (list == null) {
            models = new ArrayList<>();
        } else {
            models = list;
        }

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

    public void toggleSelection(int position) {
        if (selections.contains(position)) {
            selections.remove(position);
        } else {
            selections.add(position);
        }
        notifyItemChanged(position);
    }

    public List<Integer> getSelectedIndices() {
        return new ArrayList<>(selections);
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
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_LONG_CLICK, downLocation);
                }
                return false;
            });
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_NONE, null);
                }
            });// 设置触摸监听器
            itemView.setOnTouchListener(new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {// 记录按下时的坐标和时间
                        downLocation[0] = event.getX();
                        downLocation[1] = event.getY();
                        return false;
                    }
                    return false;
                }
            });
        }

        void setTip(String status, int code, String message) {
            if (tvTip != null) {
                boolean isErr = MessageModel.STATUS_FAILED.equals(status) && code != 200;
                tvTip.setText(message);
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
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_RESENT, null);
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
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_INTERRUPT, null);
                }
            });
            binding.btnSave.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_SAVE, null);
                }
            });
            binding.btnShare.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_SHARE, null);
                }
            });
            binding.btnX2.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_X2, null);
                }
            });
            binding.btnX4.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_X4, null);
                }
            });
            binding.btnXN.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v, getAbsoluteAdapterPosition(), OnElementClickListener.OPE_XN, null);
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
        void onClick(View view, int index, int ope, float[] downLocation);
    }
}
