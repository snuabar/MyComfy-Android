package com.snuabar.mycomfy.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.snuabar.mycomfy.databinding.FragmentHistoryBinding;
import com.snuabar.mycomfy.main.data.AbstractMessageModel;
import com.snuabar.mycomfy.main.data.MainViewModel;
import com.snuabar.mycomfy.main.data.livedata.DeletionData;
import com.snuabar.mycomfy.main.model.ReceivedMessageModel;
import com.snuabar.mycomfy.preview.FullScreenImageActivity;
import com.snuabar.mycomfy.main.data.DataIO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A fragment representing a list of Items.
 */
public class HistoryFragment extends Fragment {

    private MainViewModel mViewModel;
    private FragmentHistoryBinding binding;
    private final List<AbstractMessageModel> messageModels = new ArrayList<>();
    private final Set<String> matchedIDs = new HashSet<>();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public HistoryFragment() {
    }

    @SuppressWarnings("unused")
    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        binding.list.setAdapter(new HistoryAdapter(requireContext(), this::onItemClick));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel.getDeletionHasPressLiveData().observe(getViewLifecycleOwner(), aBoolean -> {
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                if (aBoolean) {
                    doDelete(false);
                }
            }
        });
        mViewModel.getAssociatedDeletionHasPressedLiveData().observe(getViewLifecycleOwner(), aBoolean -> {
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                if (aBoolean) {
                    doDelete(true);
                }
            }
        });
        mViewModel.getDeletionModeLiveData().observe(getViewLifecycleOwner(), aBoolean -> {
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                if (binding.list.getAdapter() instanceof HistoryAdapter) {
                    ((HistoryAdapter) binding.list.getAdapter()).setEditMode(aBoolean);
                }
            }
        });
        mViewModel.getModelListChangeLiveData().observe(getViewLifecycleOwner(), aBoolean -> {
            if (aBoolean) {
                loadImageContents();
            }
        });
        mViewModel.getClickedTabLiveData().observe(getViewLifecycleOwner(), tab -> {
            if (tab == 1 && Objects.equals(tab, mViewModel.getSelectedTabLiveData().getValue())) {
                loadImageContents();
            }
        });
        mViewModel.getMatchedIDsLiveData().observe(getViewLifecycleOwner(), ids -> {
            if (binding.list.getAdapter() instanceof HistoryAdapter) {
                if (matchedIDs.isEmpty() && (ids == null || ids.isEmpty())) {
                    return;
                }
                matchedIDs.clear();
                if (ids != null) {
                    matchedIDs.addAll(ids);
                }
                loadImageContents();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mViewModel.changeDeletionMode(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadImageContents();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        binding.list.configureLayout();
    }

    private void onItemClick(int position, boolean longClick) {
        if (position == RecyclerView.NO_POSITION) {
            return;
        }

        if (longClick) {
            mViewModel.changeDeletionMode(true);
            if (binding.list.getAdapter() instanceof HistoryAdapter) {
                ((HistoryAdapter) binding.list.getAdapter()).toggleSelection(position);
            }
        } else {
            if (Boolean.TRUE.equals(mViewModel.getDeletionModeLiveData().getValue())) {
                if (binding.list.getAdapter() instanceof HistoryAdapter) {
                    ((HistoryAdapter) binding.list.getAdapter()).toggleSelection(position);
                }
            } else {
                AbstractMessageModel model = messageModels.get(position);
//                if (model.getImageFile() != null) {
//                    Intent intent = new Intent(requireActivity(), FullScreenImageActivity.class);
//                    intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_PATH, model.getImageFile().getAbsolutePath());
//                    startActivity(intent);
//                }
                ArrayList<String> files = messageModels.stream().map(AbstractMessageModel::getId).collect(Collectors.toCollection(ArrayList::new));
                Intent intent = new Intent(requireActivity(), FullScreenImageActivity.class);
                intent.putStringArrayListExtra(FullScreenImageActivity.EXTRA_ID_LIST, files);
                intent.putExtra(FullScreenImageActivity.EXTRA_CURRENT_ID, model.getId());
                startActivity(intent);
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadImageContents() {
        if (getContext() == null) {
            return;
        }

        List<AbstractMessageModel> models = DataIO.getInstance().copyMessageModels();
        models.removeIf(m -> !(m instanceof ReceivedMessageModel) || m.getImageFile() == null || !m.getImageFile().exists());
        models.removeIf(m -> !matchedIDs.isEmpty() && !matchedIDs.contains(m.getId()));
        models.sort((o1, o2) -> Long.compare(o2.getUTCTimestamp(), o1.getUTCTimestamp()));

        if (binding.list.getAdapter() != null) {
            messageModels.clear();
            messageModels.addAll(models);
            ((HistoryAdapter) binding.list.getAdapter()).setValues(messageModels);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void doDelete(boolean deleteAssociated) {
        if (binding.list.getAdapter() instanceof HistoryAdapter) {
            List<Integer> indices = ((HistoryAdapter) binding.list.getAdapter()).getSelectedIndices();
            if (indices.isEmpty()) {
                return;
            }

            Set<String> deletionIdSet = new HashSet<>();
            for (int i : indices) {
                deletionIdSet.add(messageModels.get(i).getId());
            }

            mViewModel.changeDeletionData(new DeletionData(deletionIdSet, deleteAssociated));
        }
    }
}