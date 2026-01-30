package com.snuabar.mycomfy.main;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.client.Parameters;
import com.snuabar.mycomfy.common.Common;
import com.snuabar.mycomfy.databinding.LayoutReceivedMsgItemBinding;
import com.snuabar.mycomfy.databinding.LayoutSentMsgItemBinding;
import com.snuabar.mycomfy.main.data.AbstractMessageModel;
import com.snuabar.mycomfy.main.data.DataIO;
import com.snuabar.mycomfy.main.model.ReceivedMessageModel;
import com.snuabar.mycomfy.main.model.SentMessageModel;
import com.snuabar.mycomfy.utils.ImageUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private final static int VIEW_TYPE_SENT = 0;
    private final static int VIEW_TYPE_RECEIVED = 1;

    private final Handler mHandler;
    private final List<AbstractMessageModel> models;
//    private final Map<Integer, String> indexToPromptIdMap;
    private final OnElementClickListener listener;
    private WeakReference<RecyclerView> mRecyclerViewRef = null;
    private final Context context;
    private final Set<Integer> selections;
    private boolean isEditMode = false;

    public MessageAdapter(Context context, OnElementClickListener listener) {
        this.context = context.getApplicationContext();
        this.mHandler = new Handler(Looper.getMainLooper());
        this.models = Collections.synchronizedList(DataIO.copyMessageModels(context));
        this.listener = listener;
//        this.indexToPromptIdMap = new HashMap<>();
        this.models.sort(Comparator.comparingLong(AbstractMessageModel::getUTCTimestamp));
        this.selections = new HashSet<>();
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            return new SentViewHolder(LayoutSentMsgItemBinding.inflate(inflater, parent, false));
        }
        if (viewType == VIEW_TYPE_RECEIVED) {
            return new ReceivedViewHolder(LayoutReceivedMsgItemBinding.inflate(inflater, parent, false));
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        holder.tvDate.setText(Common.formatTimestamp(models.get(position).getUTCTimestamp()));
        holder.checkBox.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selections.contains(position));

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
            holder.binding.textView.setText(param.getPrompt());
            holder.binding.textView0.setText(String.format(Locale.getDefault(),
                    "%s\n%s\n%dx%s %d %d %.01f %.01f",
                    param.getWorkflow(),
                    param.getModel(),
                    param.getImg_width(), param.getImg_height(),
                    param.getSeed(),
                    param.getStep(), param.getCfg(),
                    param.getUpscale_factor()
                    ));
        }
        holder.setTip(model.getFailureMessage() != null ? model.getFailureMessage() : "", model.getFailureMessage() != null);
        holder.binding.btnResent.setVisibility(model.getFailureMessage() != null ? View.VISIBLE : View.GONE);
    }

    public void onBindReceivedViewHolder(@NonNull ReceivedViewHolder holder, int position) {
        ReceivedMessageModel model = (ReceivedMessageModel) models.get(position);
        if (ImageUtils.getThumbnail(model)) {
            holder.binding.imageView.setImageBitmap(BitmapFactory.decodeFile(model.getThumbnailFile().getAbsolutePath()));
            holder.binding.textView.setVisibility(View.VISIBLE);
            int[] size = ImageUtils.getImageSize(model.getImageFile());
            holder.binding.textView.setText(String.format(Locale.getDefault(), "%d x %d", size[0], size[1]));
        } else {
            holder.binding.imageView.setImageBitmap(null);
            holder.binding.textView.setVisibility(View.INVISIBLE);
            float width = holder.itemView.getContext().getResources().getDimension(R.dimen.thumbnail_width);
            float height = holder.itemView.getContext().getResources().getDimension(R.dimen.thumbnail_height);
            ImageUtils.makeThumbnailAsync(model, width, height, this::onThumbnailMake);
        }
        holder.setTip(model.getMessage(), model.getCode() != 200);
        holder.binding.btnInterrupt.setVisibility(model.getCode() == 200 || model.getInterruptionFlag() ? View.GONE : View.VISIBLE);
        holder.binding.tvDateCompletion.setText(model.isFinished() ? Common.formatTimestamp(model.getUTCTimestampCompletion()) : "");
    }

    private void onThumbnailMake(AbstractMessageModel model) {
        int index = models.indexOf(model);
        if (index != -1) {
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

    RecyclerView getRecyclerView() {
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

    private void updateIndexToPromptIdMap() {
//        indexToPromptIdMap.clear();
//        for (int i = 0; i < models.size(); i++) {
//            indexToPromptIdMap.put(i, models.get(i).getPromptId());
//        }
    }

    public void add(AbstractMessageModel model) {
        models.add(model);
//        indexToPromptIdMap.put(models.size() - 1, model.getPromptId());

        DataIO.writeModelFile(context, model);

        mHandler.post(() -> notifyItemInserted(getItemCount()));

        if (getRecyclerView() != null) {
            getRecyclerView().scrollToPosition(getItemCount() - 1);
        }
    }

//    public void setFinished(String promptId, File imageFile, int code, String message) {
//        Set<Integer> indices = indexToPromptIdMap.keySet();
//        for (int index : indices) {
//            if (promptId != null && promptId.equals(indexToPromptIdMap.get(index))) {
//                if (index >= 0 && index < getItemCount()) {
//                    AbstractMessageModel model = models.get(index);
//                    model.setImageFile(imageFile);
//                    model.setImageResponseCode(code);
//                    model.setImageResponseMessage(message);
//                    model.setFinished(, , );
//                    DataIO.writeModelFile(context, model);
//                    mHandler.post(() -> notifyItemChanged(index));
//                }
//            }
//        }
//    }

    public AbstractMessageModel get(int position) {
        if (position < 0 || position >= models.size()) {
            return null;
        }
        return models.get(position);
    }

    public void remove(AbstractMessageModel sentMessageModel) {
        remove(sentMessageModel, true);
    }

    public void remove(AbstractMessageModel sentMessageModel, boolean includeFile) {
        int index = models.lastIndexOf(sentMessageModel);
        if (index >= 0 && index < models.size()) {
            notifyItemRemoved(index);
            models.remove(index);
            if (includeFile) {
                DataIO.deleteModelFile(context, sentMessageModel);
            }
            updateIndexToPromptIdMap();
        }
    }

    public void setSentFailureMessage(AbstractMessageModel sentMessageModel, String message) {
        int index = models.lastIndexOf(sentMessageModel);
        if (index >= 0 && index < models.size()) {
            sentMessageModel = models.get(index);
            sentMessageModel.setFailureMessage(message);
            DataIO.writeModelFile(context, sentMessageModel);
            notifyItemChanged(index);
        }
    }

    public void notifyModelChanged(AbstractMessageModel model) {
        int index = models.lastIndexOf(model);
        if (index >= 0 && index < models.size()) {
            mHandler.post(() -> notifyItemChanged(index));
        }
    }

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

    public List<AbstractMessageModel> deleteSelection() {
        List<Integer> indices = getSelectedIndices();
        indices.sort((o1, o2) -> o2 - o1);

        List<AbstractMessageModel> deletedModels = new ArrayList<>();
        for (int i : indices) {
            AbstractMessageModel model = models.get(i);
            if (DataIO.deleteModelFile(context, model)) {
                deletedModels.add(model);
                models.remove(i);
                notifyItemRemoved(i);
            }
        }
        updateIndexToPromptIdMap();
        return deletedModels;
    }

    public void interrupt(AbstractMessageModel model) {
        int index = models.indexOf(model);
        if (model != null && index != -1) {
            model = models.get(index);
            model.setInterruptionFlag(true);
            DataIO.writeModelFile(context, model);
            notifyItemChanged(index);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvTip;
        public TextView tvDate;
        public CheckBox checkBox;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setLongClickable(true);
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onClick(getAbsoluteAdapterPosition(), OnElementClickListener.OPE_LONG_CLICK);
                }
                return false;
            });
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(getAbsoluteAdapterPosition(), OnElementClickListener.OPE_NONE);
                }
            });
        }

        void setTip(String text, boolean isErr) {
            if (tvTip != null) {
                tvTip.setText(text);
                tvTip.setTextColor(isErr ? android.R.color.holo_red_light : R.color.gray_83);
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
            this.checkBox = binding.checkbox;

            binding.btnResent.setOnClickListener(V -> {
                if (listener != null) {
                    listener.onClick(getAbsoluteAdapterPosition(), OnElementClickListener.OPE_RESENT);
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
            this.checkBox = binding.checkbox;
            binding.btnInterrupt.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(getAbsoluteAdapterPosition(), OnElementClickListener.OPE_INTERRUPT);
                }
            });
            binding.btnSave.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(getAbsoluteAdapterPosition(), OnElementClickListener.OPE_SAVE);
                }
            });
            binding.btnShare.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(getAbsoluteAdapterPosition(), OnElementClickListener.OPE_SHARE);
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
        void onClick(int index, int ope);
    }
}
