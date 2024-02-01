/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.own.exoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import com.google.android.exoplayer2.RendererCapabilities
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.FixedTrackSelection
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.SelectionOverride
import com.google.android.exoplayer2.trackselection.RandomTrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.own.exoplayer.DemoUtil.buildTrackName
import java.util.Arrays

/**
 * Helper class for displaying track selection dialogs.
 */
/* package */
class TrackSelectionHelper /*    *
   * @param selector                      The track selector.
   * @param adaptiveTrackSelectionFactory A factory for adaptive {@link TrackSelection}s, or null
   *                                      if the selection helper should not support adaptive tracks.
   */(
    private val selector: MappingTrackSelector,
    private val adaptiveTrackSelectionFactory: TrackSelection.Factory?
) : View.OnClickListener, DialogInterface.OnClickListener {
    private var trackInfo: MappedTrackInfo? = null
    private var rendererIndex = 0
    private var trackGroups: TrackGroupArray? = null
    private var trackGroupsAdaptive: BooleanArray?=null
    private var isDisabled = false
    private var override: SelectionOverride? = null

    // private CheckedTextView disableView;
    private var defaultView: CheckedTextView? = null
    private var enableRandomAdaptationView: CheckedTextView? = null
    private var trackViews: Array<Array<CheckedTextView?>?>?=null

    /*    *
   * Shows the selection dialog for a given renderer.
   *
   * @param activity      The parent activity.
   * @param title         The dialog's title.
   * @param trackInfo     The current track information.
   * @param rendererIndex The index of the renderer.*/
    fun showSelectionDialog(
        activity: Activity,
        title: CharSequence?,
        trackInfo: MappedTrackInfo,
        rendererIndex: Int
    ) {
        this.trackInfo = trackInfo
        this.rendererIndex = rendererIndex
        trackGroups = trackInfo.getTrackGroups(rendererIndex)
        trackGroupsAdaptive = BooleanArray(trackGroups!!.length)
        for (i in 0 until trackGroups!!.length) {
            trackGroupsAdaptive!![i] =
                adaptiveTrackSelectionFactory != null && (trackInfo.getAdaptiveSupport(
                    rendererIndex,
                    i,
                    false
                )
                        != RendererCapabilities.ADAPTIVE_NOT_SUPPORTED) && trackGroups!!.get(i).length > 1
        }
        isDisabled = selector.getRendererDisabled(rendererIndex)
        override = selector.getSelectionOverride(rendererIndex, trackGroups)
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
            .setView(buildView(builder.context))
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, null)
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(activity.resources.getColor(R.color.blue_4788f4))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(activity.resources.getColor(R.color.blue_4788f4))
        }
        dialog.show()
    }

    // DialogInterface.OnClickListener
    @SuppressLint("InflateParams")
    private fun buildView(context: Context): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.track_selection_dialog, null)
        val root = view.findViewById<View>(R.id.root) as ViewGroup
        val attributeArray =
            context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
        val selectableItemBackgroundResourceId = attributeArray.getResourceId(0, 0)
        attributeArray.recycle()

        // View for disabling the renderer.
//        disableView = (CheckedTextView) inflater.inflate(
//                android.R.layout.simple_list_item_single_choice, root, false);
//        disableView.setBackgroundResource(selectableItemBackgroundResourceId);
//        disableView.setText(R.string.selection_disabled);
//        disableView.setFocusable(true);
//        disableView.setOnClickListener(this);
        // root.addView(disableView);

        // View for clearing the override to allow the selector to use its default selection logic.
        defaultView = inflater.inflate(
            android.R.layout.simple_list_item_single_choice, root, false
        ) as CheckedTextView
        defaultView!!.setBackgroundResource(selectableItemBackgroundResourceId)
        defaultView!!.text = "Auto"
        defaultView!!.isFocusable = true
        defaultView!!.setOnClickListener(this)
        root.addView(inflater.inflate(R.layout.list_divider, root, false))
        root.addView(defaultView)

        // Per-track views.
        val haveAdaptiveTracks = false
        trackViews = arrayOfNulls(trackGroups!!.length)
        for (groupIndex in 0 until trackGroups!!.length) {
            val group = trackGroups!![groupIndex]
            var groupIsAdaptive = trackGroupsAdaptive!![groupIndex]
            groupIsAdaptive = false

            //   haveAdaptiveTracks |= groupIsAdaptive;
            trackViews!![groupIndex] = arrayOfNulls(group.length)
            for (trackIndex in 0 until group.length) {
                if (trackIndex == 0) {
                    root.addView(inflater.inflate(R.layout.list_divider, root, false))
                }
                val trackViewLayoutId =
                    if (groupIsAdaptive) android.R.layout.simple_list_item_multiple_choice else android.R.layout.simple_list_item_single_choice
                val trackView = inflater.inflate(
                    trackViewLayoutId, root, false
                ) as CheckedTextView
                trackView.setBackgroundResource(selectableItemBackgroundResourceId)
                var head = buildTrackName(group.getFormat(trackIndex))
                try {
                    head = head.split("\\,".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0]
                } catch (e: Exception) {
                }
                trackView.text =
                    head.split("x".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1] + "p"
                //trackView.setText(DemoUtil.buildTrackName(group.getFormat(trackIndex)));
                if (trackInfo!!.getTrackFormatSupport(rendererIndex, groupIndex, trackIndex)
                    == RendererCapabilities.FORMAT_HANDLED
                ) {
                    trackView.isFocusable = true
                    trackView.tag = Pair.create(groupIndex, trackIndex)
                    trackView.setOnClickListener(this)
                } else {
                    trackView.isFocusable = false
                    trackView.isEnabled = false
                }
                trackViews!![groupIndex]?.set(trackIndex, trackView)
                root.addView(trackView)
            }
        }

//disable random option
        if (haveAdaptiveTracks) {
            // View for using random adaptation.
            enableRandomAdaptationView = inflater.inflate(
                android.R.layout.simple_list_item_multiple_choice, root, false
            ) as CheckedTextView
            enableRandomAdaptationView!!.setBackgroundResource(selectableItemBackgroundResourceId)
            enableRandomAdaptationView!!.setText(R.string.enable_random_adaptation)
            enableRandomAdaptationView!!.setOnClickListener(this)

            //root.addView(inflater.inflate(R.layout.list_divider, root, false));
            // root.addView(enableRandomAdaptationView);
        }
        updateViews()
        return view
    }

    // View.OnClickListener
    private fun updateViews() {

//        disableView.setChecked(isDisabled);
        defaultView!!.isChecked = !isDisabled && override == null
        for (i in trackViews!!.indices) {
            for (j in trackViews!![i]!!.indices) {
                trackViews!![i]!![j]!!.isChecked =
                    override != null && override!!.groupIndex == i && override!!.containsTrack(j)
            }
        }
        if (enableRandomAdaptationView != null) {
            val enableView = !isDisabled && override != null && override!!.length > 1
            enableRandomAdaptationView!!.isEnabled = enableView
            enableRandomAdaptationView!!.isFocusable = enableView
            if (enableView) {
                enableRandomAdaptationView!!.isChecked = (!isDisabled
                        && override!!.factory is RandomTrackSelection.Factory)
            }
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        selector.setRendererDisabled(rendererIndex, isDisabled)
        if (override != null) {
            selector.setSelectionOverride(rendererIndex, trackGroups, override)
        } else {
            selector.clearSelectionOverrides(rendererIndex)
        }
    }

    // Track array manipulation.
    override fun onClick(view: View) {

//        if (view == disableView) {
//            isDisabled = true;
//            override = null;
//        } else if (view == defaultView) {
//            isDisabled = false;
//            override = null;
//        } else
//
//        if (view == enableRandomAdaptationView) {
//            setOverride(override.groupIndex, override.tracks, !enableRandomAdaptationView.isChecked());
//        } else
        if (view === defaultView) {
            isDisabled = false
            override = null
        } else {
            isDisabled = false
            val tag = view.tag as Pair<Int, Int>
            val groupIndex = tag.first
            val trackIndex = tag.second
            if (trackGroupsAdaptive!![groupIndex] || override == null || override!!.groupIndex != groupIndex) {
                override = SelectionOverride(FIXED_FACTORY, groupIndex, trackIndex)
            } else {
                // The group being modified is adaptive and we already have a non-null override.
                val isEnabled = (view as CheckedTextView).isChecked
                val overrideLength = override!!.length
                if (isEnabled) {
                    // Remove the track from the override.
                    if (overrideLength == 1) {
                        // The last track is being removed, so the override becomes empty.
                        override = null
                        isDisabled = true
                    } else {
                        if (enableRandomAdaptationView != null) {
                            setOverride(
                                groupIndex, getTracksRemoving(
                                    override!!, trackIndex
                                ),
                                enableRandomAdaptationView!!.isChecked
                            )
                        }
                    }
                } else {
                    // Add the track to the override.
                    if (enableRandomAdaptationView != null) {
                        setOverride(
                            groupIndex, getTracksAdding(
                                override!!, trackIndex
                            ),
                            enableRandomAdaptationView!!.isChecked
                        )
                    }
                }
            }
        }
        // Update the views with the newtag state.
        updateViews()
    }

    private fun setOverride(group: Int, tracks: IntArray, enableRandomAdaptation: Boolean) {
        val factory =
            if (tracks.size == 1) FIXED_FACTORY else (if (enableRandomAdaptation) RANDOM_FACTORY else adaptiveTrackSelectionFactory)!!

//        TrackSelection.Factory factory = FIXED_FACTORY;
        override = SelectionOverride(factory, group, *tracks)
    }

    fun setvisibility() {}

    companion object {
        /*  private static final TrackSelection.Factory FIXED_FACTORY = new FixedTrackSelection.Factory();
    private static final TrackSelection.Factory RANDOM_FACTORY = new RandomTrackSelection.Factory();

    private final MappingTrackSelector selector;
    private final TrackSelection.Factory adaptiveTrackSelectionFactory;

    private Activity activity;
    private MappedTrackInfo trackInfo;
    private int rendererIndex;
    private TrackGroupArray trackGroups;
    private boolean[] trackGroupsAdaptive;
    private boolean isDisabled;
    private SelectionOverride override;

    private CheckedTextView disableView;
    private CheckedTextView defaultView;
    private CheckedTextView enableRandomAdaptationView;
    private CheckedTextView[][] trackViews;

    */
        /**
         * @param selector                      The track selector.
         * @param adaptiveTrackSelectionFactory A factory for adaptive [TrackSelection]s, or null
         * if the selection helper should not support adaptive tracks.
         *
         * public TrackSelectionHelper(MappingTrackSelector selector,
         * TrackSelection.Factory adaptiveTrackSelectionFactory) {
         * this.selector = selector;
         * this.adaptiveTrackSelectionFactory = adaptiveTrackSelectionFactory;
         * }
         *
         * private static int[] getTracksAdding(SelectionOverride override, int addedTrack) {
         * int[] tracks = override.tracks;
         * tracks = Arrays.copyOf(tracks, tracks.length + 1);
         * tracks[tracks.length - 1] = addedTrack;
         * return tracks;
         * }
         *
         * private static int[] getTracksRemoving(SelectionOverride override, int removedTrack) {
         * int[] tracks = new int[override.length - 1];
         * int trackCount = 0;
         * for (int i = 0; i < tracks.length + 1; i++) {
         * int track = override.tracks[i];
         * if (track != removedTrack) {
         * tracks[trackCount++] = track;
         * }
         * }
         * return tracks;
         * }
         *
         *
         * / **
         * Shows the selection dialog for a given renderer.
         *
         * @param activity      The parent activity.
         * @param title         The dialog's title.
         * @param trackInfo     The current track information.
         * @param rendererIndex The index of the renderer.
         */
        /*
    public void showSelectionDialog(Activity activity, CharSequence title, MappedTrackInfo trackInfo,
                                    int rendererIndex) {
        this.trackInfo = trackInfo;
        this.rendererIndex = rendererIndex;
        this.activity = activity;
        trackGroups = trackInfo.getTrackGroups(rendererIndex);
        trackGroupsAdaptive = new boolean[trackGroups.length];
//gfg
        for (int i = 0; i < trackGroups.length; i++) {
            trackGroupsAdaptive[i] = adaptiveTrackSelectionFactory != null
                    && trackInfo.getAdaptiveSupport(rendererIndex, i, false)
                    != RendererCapabilities.ADAPTIVE_NOT_SUPPORTED
                    && trackGroups.get(i).length > 1;
        }

        isDisabled = selector.getRendererDisabled(rendererIndex);
        override = selector.getSelectionOverride(rendererIndex, trackGroups);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title)
                .setView(buildView(builder.getContext()))
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();

    }

    // DialogInterface.OnClickListener

    @SuppressLint("InflateParams")
    private View buildView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.track_selection_dialog, null);
        ViewGroup root = (ViewGroup) view.findViewById(R.id.root);

        TypedArray attributeArray = context.getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.selectableItemBackground});
        int selectableItemBackgroundResourceId = attributeArray.getResourceId(0, 0);
        attributeArray.recycle();

        // View for disabling the renderer.
        disableView = (CheckedTextView) inflater.inflate(
                android.R.layout.simple_list_item_single_choice, root, false);
        disableView.setBackgroundResource(selectableItemBackgroundResourceId);
        disableView.setText(R.string.selection_disabled);
        disableView.setFocusable(true);
        disableView.setOnClickListener(this);
        root.addView(disableView);

        // View for clearing the override to allow the selector to use its default selection logic.
        defaultView = (CheckedTextView) inflater.inflate(android.R.layout.simple_list_item_single_choice, root, false);
        defaultView.setBackgroundResource(selectableItemBackgroundResourceId);
        defaultView.setText(R.string.selection_default);
        defaultView.setFocusable(true);
        defaultView.setOnClickListener(this);
        root.addView(inflater.inflate(R.layout.list_divider, root, false));
        //root.addView(defaultView);

        // Per-track views.
        boolean haveAdaptiveTracks = false;
        trackViews = new CheckedTextView[trackGroups.length][];
        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            TrackGroup group = trackGroups.get(groupIndex);
            boolean groupIsAdaptive = trackGroupsAdaptive[groupIndex];
            haveAdaptiveTracks |= groupIsAdaptive;
            trackViews[groupIndex] = new CheckedTextView[group.length];
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                if (trackIndex == 0) {
                    root.addView(inflater.inflate(R.layout.list_divider, root, false));
                }

*/
        /*                int trackViewLayoutId = groupIsAdaptive ? android.R.layout.simple_list_item_multiple_choice
                        : android.R.layout.simple_list_item_single_choice;*/
        /*

                   int trackViewLayoutId = android.R.layout.simple_list_item_single_choice;

//                int trackViewLayoutId =  android.R.layout.simple_list_item_single_choice;

                CheckedTextView trackView = (CheckedTextView) inflater.inflate(
                        trackViewLayoutId, root, false);
                trackView.setBackgroundResource(selectableItemBackgroundResourceId);

                String head = DemoUtil.buildTrackName(group.getFormat(trackIndex));
                try {
                    head = head.split("\\,")[0] ;
                } catch (Exception e) {

                }
                trackView.setText(head);

                if (trackInfo.getTrackFormatSupport(rendererIndex, groupIndex, trackIndex)
                        == RendererCapabilities.FORMAT_HANDLED) {
                    trackView.setFocusable(true);
                    trackView.setTag(Pair.create(groupIndex, trackIndex));
                    trackView.setOnClickListener(this);
                } else {
                    trackView.setFocusable(false);
                    trackView.setEnabled(false);
                }
                trackViews[groupIndex][trackIndex] = trackView;
                root.addView(trackView);
            }
        }

        if (haveAdaptiveTracks) {
            // View for using random adaptation.
            enableRandomAdaptationView = (CheckedTextView) inflater.inflate(
                    android.R.layout.simple_list_item_multiple_choice, root, false);
            enableRandomAdaptationView.setBackgroundResource(selectableItemBackgroundResourceId);
            enableRandomAdaptationView.setText(R.string.enable_random_adaptation);
            enableRandomAdaptationView.setOnClickListener(this);
            //  root.addView(inflater.inflate(R.layout.list_divider, root, false));
            // root.addView(enableRandomAdaptationView);
        }

        updateViews();
        return view;
    }

    // View.OnClickListener

    private void updateViews() {

        disableView.setChecked(isDisabled);
        defaultView.setChecked(!isDisabled && override == null);
        for (int i = 0; i < trackViews.length; i++) {
            for (int j = 0; j < trackViews[i].length; j++) {
                trackViews[i][j].setChecked(override != null && override.groupIndex == i
                        && override.containsTrack(j));
            }
        }
*/
        /*        if (enableRandomAdaptationView != null) {
            boolean enableView = !isDisabled && override != null && override.length > 1;
            enableRandomAdaptationView.setEnabled(enableView);
            enableRandomAdaptationView.setFocusable(enableView);
            if (enableView) {
                enableRandomAdaptationView.setChecked(!isDisabled
                        && override.factory instanceof RandomTrackSelection.Factory);
            }
        }*/
        /*
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        selector.setRendererDisabled(rendererIndex, isDisabled);
        if (override != null) {
            selector.setSelectionOverride(rendererIndex, trackGroups, override);
        } else {
            selector.clearSelectionOverrides(rendererIndex);
        }
    }

    // Track array manipulation.

    @Override
    public void onClick(View view) {
        //yo...
        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            TrackGroup group = trackGroups.get(groupIndex);
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                if (!view.equals(trackViews[groupIndex][trackIndex])) {
                    trackViews[groupIndex][trackIndex].setChecked(false);
                    Log.e("hey", "f: " + trackIndex);
                } else {
                    trackViews[groupIndex][trackIndex].setChecked(true);
                    Log.e("hey", "t: " + trackIndex);
                }
            }
        }

        // Log.e("hey", "last");
        //((CheckedTextView) view).setChecked(true);

        //End

        if (view == disableView) {
            isDisabled = true;
            override = null;
        } else if (view == defaultView) {
            isDisabled = false;
            override = null;
        } else if (view == enableRandomAdaptationView) {
            setOverride(override.groupIndex, override.tracks, !enableRandomAdaptationView.isChecked());
        } else {
            isDisabled = false;
            @SuppressWarnings("unchecked")
            Pair<Integer, Integer> tag = (Pair<Integer, Integer>) view.getTag();
            int groupIndex = tag.first;
            int trackIndex = tag.second;
            if (!trackGroupsAdaptive[groupIndex] || override == null
                    || override.groupIndex != groupIndex) {
                override = new SelectionOverride(FIXED_FACTORY, groupIndex, trackIndex);
            } else {
                // The group being modified is adaptive and we already have a non-null override.
                boolean isEnabled = ((CheckedTextView) view).isChecked();
                int overrideLength = override.length;
                if (isEnabled) {
                    // Remove the track from the override.
                    if (overrideLength == 1) {
                        // The last track is being removed, so the override becomes empty.
                        override = null;
                        isDisabled = true;
                    } else {
                        setOverride(groupIndex, getTracksRemoving(override, trackIndex),
                                enableRandomAdaptationView.isChecked());
                    }
                } else {
                    // Add the track to the override.
                    setOverride(groupIndex, getTracksAdding(override, trackIndex),
                            enableRandomAdaptationView.isChecked());
                }
            }
        }
        // Update the views with the newtag state.
        updateViews();

    }

    private void setOverride(int group, int[] tracks, boolean enableRandomAdaptation) {
        TrackSelection.Factory factory = tracks.length == 1 ? FIXED_FACTORY
                : (enableRandomAdaptation ? RANDOM_FACTORY : adaptiveTrackSelectionFactory);
        override = new SelectionOverride(factory, group, tracks);
    }*/
        private val FIXED_FACTORY: TrackSelection.Factory = FixedTrackSelection.Factory()
        private val RANDOM_FACTORY: TrackSelection.Factory = RandomTrackSelection.Factory()
        private fun getTracksAdding(override: SelectionOverride, addedTrack: Int): IntArray {
            var tracks = override.tracks
            tracks = Arrays.copyOf(tracks, tracks.size + 1)
            tracks[tracks.size - 1] = addedTrack
            return tracks
        }

        private fun getTracksRemoving(override: SelectionOverride, removedTrack: Int): IntArray {
            val tracks = IntArray(override.length - 1)
            var trackCount = 0
            for (i in 0 until tracks.size + 1) {
                val track = override.tracks[i]
                if (track != removedTrack) {
                    tracks[trackCount++] = track
                }
            }
            return tracks
        }
    }
}
