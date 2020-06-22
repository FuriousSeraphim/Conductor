package com.bluelinelabs.conductor.demo.controllers;

import android.graphics.PorterDuff.Mode;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bluelinelabs.conductor.ControllerChangeHandler;
import com.bluelinelabs.conductor.ControllerChangeType;
import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler;
import com.bluelinelabs.conductor.demo.R;
import com.bluelinelabs.conductor.demo.controllers.base.BaseController;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ExternalModulesController extends BaseController {

    private enum DemoModel {
        ARCH_LIFECYCLE("Arch Components Lifecycle", R.color.orange_300);

        String title;
        @ColorRes int color;

        DemoModel(String title, @ColorRes int color) {
            this.title = title;
            this.color = color;
        }
    }

    @BindView(R.id.recycler_view) RecyclerView recyclerView;

    @NonNull
    @Override
    protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        return inflater.inflate(R.layout.controller_additional_modules, container, false);
    }

    @Override
    protected void onViewBound(@NonNull View view) {
        super.onViewBound(view);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(new AdditionalModulesAdapter(LayoutInflater.from(view.getContext()), DemoModel.values()));
    }

    @Override
    protected void onChangeStarted(@NonNull ControllerChangeHandler changeHandler, @NonNull ControllerChangeType changeType) {
        if (changeType.isEnter) {
            setTitle();
        }
    }

    @Override
    protected String getTitle() {
        return "External Module Demos";
    }

    void onModelRowClick(DemoModel model) {
        switch (model) {
            case ARCH_LIFECYCLE:
                getRouter().pushController(RouterTransaction.with(new ArchLifecycleController())
                        .pushChangeHandler(new FadeChangeHandler())
                        .popChangeHandler(new FadeChangeHandler()));
                break;
        }
    }

    class AdditionalModulesAdapter extends RecyclerView.Adapter<AdditionalModulesAdapter.ViewHolder> {

        private final LayoutInflater inflater;
        private final DemoModel[] items;

        public AdditionalModulesAdapter(LayoutInflater inflater, DemoModel[] items) {
            this.inflater = inflater;
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(inflater.inflate(R.layout.row_home, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bind(items[position]);
        }

        @Override
        public int getItemCount() {
            return items.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.tv_title) TextView tvTitle;
            @BindView(R.id.img_dot) ImageView imgDot;
            private DemoModel model;

            public ViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            void bind(DemoModel item) {
                model = item;
                tvTitle.setText(item.title);
                imgDot.getDrawable().setColorFilter(ContextCompat.getColor(getActivity(), item.color), Mode.SRC_ATOP);
            }

            @OnClick(R.id.row_root)
            void onRowClick() {
                onModelRowClick(model);
            }

        }
    }

}