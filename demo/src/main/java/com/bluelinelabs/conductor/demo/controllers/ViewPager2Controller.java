package com.bluelinelabs.conductor.demo.controllers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.demo.R;
import com.bluelinelabs.conductor.demo.controllers.base.BaseController;
import com.bluelinelabs.conductor.viewpager2.RouterStateAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Locale;

import butterknife.BindView;

public class ViewPager2Controller extends BaseController {

    private int[] PAGE_COLORS = new int[]{R.color.green_300, R.color.cyan_300, R.color.deep_purple_300, R.color.lime_300, R.color.red_300};

    @BindView(R.id.tab_layout) TabLayout tabLayout;
    @BindView(R.id.view_pager) ViewPager2 viewPager;

    private final RouterStateAdapter pagerAdapter;

    public ViewPager2Controller() {
        pagerAdapter = new RouterStateAdapter(this) {
            @Override
            public void configureRouter(@NonNull Router router, int position) {
                if (!router.hasRootController()) {
                    Controller page = new ChildController(String.format(Locale.getDefault(), "Child #%d (Swipe to see more)", position), PAGE_COLORS[position], true);
                    router.setRoot(RouterTransaction.with(page));
                }
            }

            @Override
            public int getItemCount() {
                return PAGE_COLORS.length;
            }
        };
    }

    @NonNull
    @Override
    protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        return inflater.inflate(R.layout.controller_view_pager2, container, false);
    }

    @Override
    protected void onViewBound(@NonNull View view) {
        super.onViewBound(view);
        viewPager.setAdapter(pagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText("Page " + position)).attach();
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        if (!getActivity().isChangingConfigurations()) {
            viewPager.setAdapter(null);
        }
        tabLayout.setupWithViewPager(null);
        super.onDestroyView(view);
    }

    @Override
    protected String getTitle() {
        return "ViewPager2 Demo";
    }

}
