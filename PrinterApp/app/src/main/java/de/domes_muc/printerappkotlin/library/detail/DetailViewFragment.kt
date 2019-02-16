package de.domes_muc.printerappkotlin.library.detail

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.library.LibraryController
import de.domes_muc.printerappkotlin.model.ModelFile
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView

import java.io.File
import java.util.ArrayList

/**
 * This class will create the detail view for every Project.
 * it will contain a list of the files inside the project and a set of options.
 *
 * @author alberto-baeza
 */
class DetailViewFragment : Fragment() {

    private var mFile: ModelFile? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Reference to View
        var rootView: View? = null

        //If is not new
        if (savedInstanceState == null) {

            val args = arguments

            //Show custom option menu
            setHasOptionsMenu(true)

            //Inflate the fragment
            rootView = inflater.inflate(
                R.layout.library_model_detail_right_panel,
                container, false
            )

            args?.let {
                mFile = LibraryController.fileList.get(it.getInt("index")) as ModelFile
            }

            //Share button
            val shareButton = rootView!!.findViewById<View>(R.id.detail_share_button) as ImageButton
            shareButton.setColorFilter(resources.getColor(R.color.body_text_2), PorterDuff.Mode.MULTIPLY)
            shareButton.setOnClickListener {
                //Sharing intent
                val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                val shareBody = getString(R.string.share_content_text)
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_subject_text))
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody)
                sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(mFile!!.stl)))
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via_text)))
            }

            //Favorite button
            val favButton = rootView.findViewById<View>(R.id.detail_fav_button) as ImageButton
            if (DatabaseController.isPreference(DatabaseController.TAG_FAVORITES, mFile!!.name))
                favButton.setImageResource(R.drawable.ic_action_star)
            else
                favButton.setImageResource(R.drawable.ic_action_star_outline)
            favButton.setColorFilter(resources.getColor(R.color.body_text_2), PorterDuff.Mode.MULTIPLY)
            favButton.setOnClickListener { addToFavorite(favButton) }

            //Close button
            val closeButton = rootView.findViewById<View>(R.id.detail_close_button) as ImageButton
            closeButton.setColorFilter(resources.getColor(R.color.body_text_2), PorterDuff.Mode.MULTIPLY)
            closeButton.setOnClickListener { removeRightPanel() }

            val tv = rootView.findViewById<View>(R.id.detail_tv_name) as TextView
            tv.text = mFile!!.name

            val arrayFiles = ArrayList<File>()

            //Create a file adapter with every gcode
            if (mFile!!.gcodeList != null) {

                val listFiles = File(mFile!!.gcodeList).parentFile.listFiles()

                for (i in listFiles.indices) {
                    arrayFiles.add(listFiles[i])
                }
            }

            try {
                //add also de the stl
                arrayFiles.add(File(mFile!!.stl))

            } catch (e: Exception) {
                e.printStackTrace()
            }


            val adapter = DetailViewAdapter(activity!!, R.layout.detailview_list_element, arrayFiles, mFile!!.snapshot!!)
            val lv = rootView.findViewById<View>(R.id.detail_lv) as ListView

            //Set header and footer of the listview to allow all the view to scrolling
            val lheader = View.inflate(activity, R.layout.detailview_list_header, null)
            val iv = lheader.findViewById<View>(R.id.detail_iv_preview) as ImageView
            iv.setImageDrawable(mFile!!.snapshot)
            lv.addHeaderView(lheader)

            //FIXME Uncomment this in the final version
            //            View lfooter = View.inflate(getActivity(), R.layout.detailview_list_footer, null);
            //            TextView footertv = (TextView) lfooter.findViewById(R.id.detail_tv_description);
            //            footertv.setText(getResources().getString(R.string.lorem_ipsum));
            //            lv.addFooterView(lfooter);

            lv.adapter = adapter
        }

        return rootView
    }

    //    @Override
    //    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    //        super.onCreateOptionsMenu(menu, inflater);
    //
    //        inflater.inflate(R.menu.detailview_menu, menu);
    //
    //        if (DatabaseController.isPreference(DatabaseController.TAG_FAVORITES, mFile.getName())) {
    //            menu.findItem(R.id.menu_favorite).setIcon(R.drawable.ic_action_star);
    //        } else menu.findItem(R.id.menu_favorite).setIcon(R.drawable.ic_action_star_outline);
    //
    //    }

    private fun addToFavorite(button: ImageButton) {
        if (DatabaseController.isPreference(DatabaseController.TAG_FAVORITES, mFile!!.name)) {
            DatabaseController.handlePreference(DatabaseController.TAG_FAVORITES, mFile!!.name, null, false)
            button.setImageResource(R.drawable.ic_action_star_outline)
        } else {
            DatabaseController.handlePreference(
                DatabaseController.TAG_FAVORITES,
                mFile!!.name,
                mFile!!.absolutePath,
                true
            )
            button.setImageResource(R.drawable.ic_action_star)
        }
    }

    fun removeRightPanel() {
        val fragmentTransaction = activity!!.supportFragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(R.animator.fragment_slide_in_right, R.animator.fragment_slide_out_right)
        fragmentTransaction.remove(this).commit()
    }

    //Option menu
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {

        when (item.itemId) {

            R.id.menu_favorite //Add a new printer
            -> {

                if (DatabaseController.isPreference(DatabaseController.TAG_FAVORITES, mFile!!.name)) {
                    DatabaseController.handlePreference(DatabaseController.TAG_FAVORITES, mFile!!.name, null, false)
                    item.icon = resources.getDrawable(R.drawable.ic_action_star_outline)
                } else {
                    DatabaseController.handlePreference(
                        DatabaseController.TAG_FAVORITES,
                        mFile!!.name,
                        mFile!!.absolutePath,
                        true
                    )
                    item.icon = resources.getDrawable(R.drawable.ic_action_star)
                }

                return true
            }
            android.R.id.home -> {
                activity!!.onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


}
