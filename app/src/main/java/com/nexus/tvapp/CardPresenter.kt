package com.nexus.tvapp

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide

class CardPresenter : Presenter() {
    private var cardWidthPx = 0
    private var cardHeightPx = 0

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val density = parent.context.resources.displayMetrics.density
        cardWidthPx = (140 * density).toInt() // Um pouco menor
        cardHeightPx = (210 * density).toInt()

        val card = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(cardWidthPx, cardHeightPx)
            setMainImageScaleType(android.widget.ImageView.ScaleType.CENTER_CROP)
            setInfoAreaBackgroundColor(Color.parseColor("#1F1F1F"))
            // Adiciona margem entre as capas
            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(16, 16, 16, 16)
            layoutParams = params
        }
        return ViewHolder(card)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val filme = item as? Filme ?: return
        val card = viewHolder.view as ImageCardView
        card.titleText = filme.title
        card.contentText = null
        card.setMainImageDimensions(cardWidthPx, cardHeightPx)

        Glide.with(card.context)
            .load(filme.poster)
            .placeholder(ColorDrawable(Color.parseColor("#2A2A2A")))
            .error(ColorDrawable(Color.parseColor("#2A2A2A")))
            .centerCrop()
            .into(card.mainImageView)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val card = viewHolder.view as ImageCardView
        card.badgeImage = null
        card.mainImage = null
    }
}
