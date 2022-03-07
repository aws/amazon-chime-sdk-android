package com.amazonaws.services.chime.sdkdemo.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.CheckedTextView
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.data.SpinnerItem
import com.amazonaws.services.chime.sdkdemo.data.TranscribeLanguageOption
import com.amazonaws.services.chime.sdkdemo.utils.inflate

class LanguageOptionsAdapter(
    private val context: Context,
    private val languageOptions: Map<String, List<SpinnerItem?>>,
    private val languageGroups: List<String>,
    private val languageOptionsSelected: Set<TranscribeLanguageOption>
) : BaseExpandableListAdapter() {

    override fun getGroupCount(): Int {
        return languageOptions.size
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return languageOptions[languageGroups[listPosition]]?.size ?: 0
    }

    override fun getGroup(listPosition: Int): Any {
        return languageGroups[listPosition]
    }

    override fun getChild(listPosition: Int, expandedListPosition: Int): SpinnerItem? {
        return languageOptions[languageGroups[listPosition]]?.getOrNull(expandedListPosition)
    }

    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroupView(listPosition: Int, isExpanded: Boolean, convertView_: View?, parent: ViewGroup?): View? {
        var convertView: View? = convertView_
        val listTitle = getGroup(listPosition) as String
        if (convertView == null) {
            convertView = parent?.inflate(R.layout.row_language_option, false)
        }
        if (convertView != null) {
            val listTitleTextView: CheckedTextView = convertView.findViewById(R.id.checkedTextViewLanguageOptionRow)
            listTitleTextView.setTypeface(null, Typeface.BOLD)
            listTitleTextView.checkMarkDrawable = null
            listTitleTextView.text = listTitle
        }
        return convertView
    }

    override fun getChildView(listPosition: Int, expandedListPosition: Int, isLastChild: Boolean, convertView_: View?, ViewGroup: ViewGroup?): View? {
        val expandedListText = (getChild(listPosition, expandedListPosition) as SpinnerItem).value
        var convertView = convertView_
        if (convertView == null) {
            val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.row_language_option, null)
        }
        if (convertView != null) {
            val textView: CheckedTextView = convertView.findViewById(R.id.checkedTextViewLanguageOptionRow)
            val cell = languageOptions[languageGroups[listPosition]]?.get(expandedListPosition)?.let {
                TranscribeLanguageOption(listPosition, expandedListPosition, it)
            }
            textView.isChecked = languageOptionsSelected.contains(cell)
            textView.text = expandedListText
        }

        return convertView
    }

    override fun isChildSelectable(p0: Int, p1: Int): Boolean {
        return true
    }
}
