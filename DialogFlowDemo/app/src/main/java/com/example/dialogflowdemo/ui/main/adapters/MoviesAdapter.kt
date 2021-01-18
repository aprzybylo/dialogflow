package com.example.dialogflowdemo.ui.main.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.dialogflowdemo.R
import com.squareup.picasso.Picasso
import com.example.dialogflowdemo.ui.main.MovieViewModel

import kotlinx.android.synthetic.main.dialog_info.view.cast
import kotlinx.android.synthetic.main.dialog_info.view.duration
import kotlinx.android.synthetic.main.dialog_info.view.seasons
import kotlinx.android.synthetic.main.listview_item.view.cover
import kotlinx.android.synthetic.main.listview_item.view.description
import kotlinx.android.synthetic.main.listview_item.view.title

class MoviesAdapter(val moviesList: List<MovieViewModel>) :
    RecyclerView.Adapter<MoviesAdapter.MovieHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieHolder {
        return MovieHolder(parent.inflate(R.layout.listview_item))
    }

    fun ViewGroup.inflate(layoutRes: Int): View {
        return LayoutInflater.from(context).inflate(layoutRes, this, false)
    }

    override fun getItemCount(): Int {
        return moviesList.size
    }

    override fun onBindViewHolder(holder: MovieHolder, position: Int) {
        holder.bind(moviesList.get(position))
    }

    class MovieHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(item: MovieViewModel) = with(itemView) {
            title.text = item.title
            description.text = item.description
            cover.loadUrl(item.imgUrl)

            cover.setOnClickListener {
                    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_info, null)
                    dialogView.title.text = item.title
                    dialogView.duration.text = item.duration
                    dialogView.cast.text = item.cast
                    dialogView.seasons.text = item.sesonsNumber.toString()

                    val builder = AlertDialog.Builder(context)
                    builder.setView(dialogView)
                    builder.setPositiveButton(android.R.string.yes) { dialog, which -> dialog.dismiss() }
                    builder.show()
            }
        }

        fun ImageView.loadUrl(url: String?) {
            Picasso.with(context).load(url).into(this)
        }
    }
}