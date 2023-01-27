package com.healthmetrix.myscience.feature.login

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.doOnNextLayout
import androidx.recyclerview.widget.RecyclerView
import com.healthmetrix.myscience.commons.ui.ViewBindingViewHolder
import com.healthmetrix.s4h.myscience.databinding.ViewHolderPdfPageBinding
import java.io.Closeable
import java.util.concurrent.Executor

// candidate for librarification
/**
 * @param executor Executor to do PDF rendering with, for example, Dispatchers.Default.asExecutor
 */
class PdfAdapter(
    private val executor: Executor,
) :
    RecyclerView.Adapter<ViewBindingViewHolder<ViewHolderPdfPageBinding>>(),
    Closeable {

    private val pageCache = mutableMapOf<Int, Bitmap>()

    private var pdfRenderer: PdfRenderer? = null

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    fun submit(parcelFileDescriptor: ParcelFileDescriptor) {
        pdfRenderer = PdfRenderer(parcelFileDescriptor)
        pageCache.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewBindingViewHolder<ViewHolderPdfPageBinding> =
        LayoutInflater.from(parent.context)
            .let { ViewHolderPdfPageBinding.inflate(it, parent, false) }
            .let(::ViewBindingViewHolder)

    override fun getItemCount(): Int = pdfRenderer?.pageCount ?: 0

    override fun onBindViewHolder(
        holder: ViewBindingViewHolder<ViewHolderPdfPageBinding>,
        position: Int,
    ) {
        pageCache[position]?.let {
            holder.setImage(it)
        } ?: run {
            // cache miss
            holder.binding.root.doOnNextLayout { v ->
                val targetWidth = v.width

                executor.execute {
                    /**
                     * Null dereference safety:
                     *
                     * ViewHolders will only be bound if item count > 0, which
                     * can only happen if pdfRenderer is not null
                     */
                    val page = pdfRenderer!!.openPage(position)
                    val scaleRatio = targetWidth.toFloat() / page.width.toFloat()
                    val targetHeight = (page.height * scaleRatio).toInt()

                    val bitmap = Bitmap.createBitmap(
                        targetWidth,
                        targetHeight,
                        Bitmap.Config.ARGB_8888,
                    )

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    pageCache[position] = bitmap

                    holder.binding.pdfPageImageView.post {
                        holder.setImage(bitmap)
                    }
                }
            }
        }
    }

    override fun close() {
        pdfRenderer?.close()
        pdfRenderer = null
    }

    private fun ViewBindingViewHolder<ViewHolderPdfPageBinding>.setImage(bitmap: Bitmap) {
        binding.pdfPageImageView.setImageDrawable(
            BitmapDrawable(
                binding.root.resources,
                bitmap,
            ),
        )
    }

    interface Renderer {
        fun render(targetWidth: Int, targetHeight: Int)
    }
}
