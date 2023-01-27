package com.healthmetrix.myscience.feature.login.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.healthmetrix.myscience.commons.ui.ViewBindingViewHolder
import com.healthmetrix.myscience.conductor.ViewLifecycleController
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.login.Event
import com.healthmetrix.myscience.feature.login.di.LoginEntryPoint
import com.healthmetrix.s4h.myscience.BuildConfig
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.ControllerLoginWelcomeBinding
import com.healthmetrix.s4h.myscience.databinding.ItemLoginWelcomeBinding
import kotlinx.coroutines.launch

/**
 * Step 1
 *
 * First screen on a new installation
 */
class WelcomeController : ViewLifecycleController() {

    private val entryPoint by entryPoint<LoginEntryPoint>()

    private val images = listOf(
        R.drawable.ic_all_nobg_01,
        R.drawable.ic_all_nobg_02,
        R.drawable.ic_all_nobg_03,
        R.drawable.ic_all_nobg_04,
        R.drawable.ic_all_nobg_05,
        R.drawable.ic_all_nobg_06,
        R.drawable.ic_all_nobg_07,
        R.drawable.ic_all_nobg_08,
        R.drawable.ic_all_nobg_09,
        R.drawable.ic_all_nobg_10,
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerLoginWelcomeBinding.inflate(inflater, container, false).apply {
            welcomeNext.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint.continueLoginUseCase(Event.FORWARD)
                }
            }

            welcomeTitle.setOnLongClickListener {
                val info =
                    "noflavor:${BuildConfig.BUILD_TYPE}:${BuildConfig.VERSION_NAME}:${BuildConfig.VERSION_CODE}"
                Toast.makeText(it.context, info, Toast.LENGTH_LONG).show()
                true
            }

            viewPager.adapter = root.resources
                .getStringArray(R.array.welcome_carousel_text)
                .toList()
                .zip(images)
                .let(::WelcomeViewPagerAdapter)

            viewPager.registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        viewPagerNext.isEnabled =
                            position != viewPager.adapter?.itemCount?.minus(1) ?: 0

                        viewPagerPrevious.isEnabled = position != 0
                    }
                },
            )

            viewPagerNext.setOnClickListener {
                viewPager.currentItem = viewPager.currentItem + 1
            }

            viewPagerPrevious.setOnClickListener {
                viewPager.currentItem = viewPager.currentItem - 1
            }
        }.root
    }
}

class WelcomeViewPagerAdapter(
    private val items: List<Pair<String, Int>>,
) : RecyclerView.Adapter<ViewBindingViewHolder<ItemLoginWelcomeBinding>>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewBindingViewHolder<ItemLoginWelcomeBinding> {
        return LayoutInflater.from(parent.context)
            .let { ItemLoginWelcomeBinding.inflate(it, parent, false) }
            .let(::ViewBindingViewHolder)
    }

    override fun onBindViewHolder(
        holder: ViewBindingViewHolder<ItemLoginWelcomeBinding>,
        position: Int,
    ) {
        holder.binding.welcomeImage.setImageResource(items[position].second)
        holder.binding.welcomeImageDescription.text = items[position].first
    }

    override fun getItemCount(): Int = items.size
}
