package com.garethevans.church.opensongtablet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

public class PopUpSongCreateFragment extends DialogFragment {
    // This is a quick popup to enter a new song folder name, or rename a current one
    // Once it has been completed positively (i.e. ok was clicked) it sends a refreshAll() interface call

    static ArrayList<String> newtempfolders;
    Spinner newFolderSpinner;
    EditText newSongNameEditText;
    private MyInterface mListener;
    AsyncTask<Object, Void, String> getfolders;
    LoadXML loadXML;
    StorageAccess storageAccess;
    DocumentFile homeFolder;

    static PopUpSongCreateFragment newInstance() {
        PopUpSongCreateFragment frag;
        frag = new PopUpSongCreateFragment();
        return frag;
    }

    public interface MyInterface {
        void doEdit();
        void loadSong();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onAttach(Activity activity) {
        mListener = (MyInterface) activity;
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() != null && getDialog() != null) {
            PopUpSizeAndAlpha.decoratePopUp(getActivity(),getDialog());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.dismiss();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setCanceledOnTouchOutside(true);
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadXML = new LoadXML();
        storageAccess = new StorageAccess();
        homeFolder = storageAccess.getHomeFolder(getActivity());
        View V = inflater.inflate(R.layout.popup_songcreate, container, false);

        TextView title = V.findViewById(R.id.dialogtitle);
        title.setText(getActivity().getResources().getString(R.string.createanewsong));
        final FloatingActionButton closeMe = V.findViewById(R.id.closeMe);
        closeMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomAnimations.animateFAB(closeMe,getActivity());
                closeMe.setEnabled(false);
                dismiss();
            }
        });
        final FloatingActionButton saveMe = V.findViewById(R.id.saveMe);
        saveMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomAnimations.animateFAB(saveMe,getActivity());
                //saveMe.setEnabled(false);
                doSave();
            }
        });

        // Initialise the views
        newFolderSpinner = V.findViewById(R.id.newFolderSpinner);
        newSongNameEditText = V.findViewById(R.id.newSongNameEditText);

        if (FullscreenActivity.whattodo.equals("savecameraimage")) {
            try {
                String currimagename = FullscreenActivity.mCurrentPhotoPath.
                        substring(FullscreenActivity.mCurrentPhotoPath.lastIndexOf("/") + 1);
                    newSongNameEditText.setText(currimagename);
            } catch (Exception e) {
                newSongNameEditText.setText(FullscreenActivity.imagetext);
            }
        } else {
            newSongNameEditText.setText("");
        }

        // Set up the folderspinner
        // Populate the list view with the current song folders
        // Reset to the main songs folder, so we can list them
        FullscreenActivity.currentFolder = FullscreenActivity.whichSongFolder;
        FullscreenActivity.newFolder = FullscreenActivity.whichSongFolder;
        getfolders = new GetFolders();
        try {
            getfolders.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            Log.d("d","Probably closed popup before folders listed\n"+e);
        }

        // Set the newFolderSpinnerListener
        newFolderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                FullscreenActivity.newFolder = newtempfolders.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        PopUpSizeAndAlpha.decoratePopUp(getActivity(),getDialog());
        return V;

    }

    public void doSave() {
        // Get the variables
        String tempNewSong = newSongNameEditText.getText().toString().trim();

        if (FullscreenActivity.whattodo.equals("savecameraimage")) {
            DocumentFile from = DocumentFile.fromSingleUri(getActivity(),Uri.parse(FullscreenActivity.mCurrentPhotoPath));
            String currimagename = from.getName();

            // If no name is specified, use the original ugly one
            if (tempNewSong.isEmpty() || tempNewSong.equals("")) {
                tempNewSong = currimagename;
            }

            // Check the camera image ends with .jpg.  If not, add it!
            if (!tempNewSong.endsWith(".jpg")) {
                tempNewSong = tempNewSong + ".jpg";
            }

            DocumentFile to = storageAccess.getFileLocationAsDocumentFile(getActivity(),homeFolder,
                    "Songs",
                    FullscreenActivity.newFolder,tempNewSong);

            if (storageAccess.moveAFile(getActivity(),from,to)) {
                FullscreenActivity.myToastMessage = getActivity().getResources().getString(R.string.success);

                if (mListener != null) {
                    FullscreenActivity.songfilename = tempNewSong;
                    FullscreenActivity.whichSongFolder = FullscreenActivity.newFolder;
                    mListener.loadSong();
                }

            } else {
                FullscreenActivity.myToastMessage = getActivity().getResources().getString(R.string.error);
            }

            ShowToast.showToast(getActivity());
            dismiss();

        } else {
            if (!tempNewSong.equals("") && !tempNewSong.isEmpty()
                    && !tempNewSong.contains("/")
                    && !tempNewSong.equals(FullscreenActivity.mainfoldername)) {

                FullscreenActivity.whichSongFolder = FullscreenActivity.newFolder;

                // Try to create
                if (tempNewSong.endsWith(".pdf") || tempNewSong.endsWith(".PDF")) {
                    // Naughty, naughty, it shouldn't be a pdf extension
                    tempNewSong = tempNewSong.replace(".pdf", "");
                    tempNewSong = tempNewSong.replace(".PDF", "");
                }

                LoadXML loadXML = new LoadXML();
                loadXML.initialiseSongTags();

                // Prepare the XML
                FullscreenActivity.songfilename = tempNewSong;
                FullscreenActivity.mTitle = tempNewSong;

                Preferences.savePreferences();

                PopUpEditSongFragment.prepareBlankSongXML();

                // If this is an import from text intent, add the text to the lyrics
                if (FullscreenActivity.scripture_title!=null &&
                        FullscreenActivity.scripture_title.equals("importedtext_in_scripture_verse") &&
                        FullscreenActivity.scripture_verse!=null && !FullscreenActivity.scripture_verse.equals("")) {
                    FullscreenActivity.mLyrics = FullscreenActivity.scripture_verse;
                    FullscreenActivity.mynewXML =  FullscreenActivity.mynewXML.replace("<lyrics>[V]\n</lyrics>",
                            "<lyrics>[V]\n"+FullscreenActivity.scripture_verse+"</lyrics>");
                }

                // Save the file
                try {
                    PopUpEditSongFragment.justSaveSongXML(getActivity());
                } catch (Exception e) {
                    e.printStackTrace();
                    FullscreenActivity.myToastMessage = getActivity().getResources().getString(R.string.savesong) + " - " +
                            getActivity().getResources().getString(R.string.error);
                    ShowToast.showToast(getActivity());
                }

                // Load the XML up into memory
                try {
                    loadXML.loadXML(getActivity(),homeFolder);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Tell the main page to now edit the song
                if (mListener != null) {
                    mListener.doEdit();
                }

                // If we are autologging CCLI information
                if (FullscreenActivity.ccli_automatic) {
                    PopUpCCLIFragment popUpCCLIFragment = new PopUpCCLIFragment();
                    popUpCCLIFragment.addUsageEntryToLog(getActivity(),FullscreenActivity.whichSongFolder+"/"+FullscreenActivity.songfilename,
                            FullscreenActivity.songfilename, "",
                            "", "", "1"); // Created
                }

                // Close the popup
                dismiss();
            } else {
                FullscreenActivity.myToastMessage = getResources().getString(R.string.error_notset);
                ShowToast.showToast(getActivity());
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class GetFolders extends AsyncTask<Object, Void, String> {
        @Override
        protected String doInBackground(Object... objects) {
            ListSongFiles listSongFiles = new ListSongFiles();
            listSongFiles.getAllSongFolders(getActivity(), homeFolder);
            return null;
        }

        protected void onPostExecute(String s) {
            // The song folder
            newtempfolders = new ArrayList<>();
            if (FullscreenActivity.mainfoldername!=null) {
                newtempfolders.add(FullscreenActivity.mainfoldername);
            }
            for (int e = 0; e < FullscreenActivity.mSongFolderNames.length; e++) {
                if (FullscreenActivity.mSongFolderNames[e] != null &&
                        !FullscreenActivity.mSongFolderNames[e].equals(FullscreenActivity.mainfoldername)) {
                    newtempfolders.add(FullscreenActivity.mSongFolderNames[e]);
                }
            }
            if (newtempfolders==null) {
                newtempfolders = new ArrayList<>();
                newtempfolders.add("");
            }
            ArrayAdapter<String> folders = new ArrayAdapter<>(getActivity(), R.layout.my_spinner, newtempfolders);
            folders.setDropDownViewResource(R.layout.my_spinner);
            newFolderSpinner.setAdapter(folders);

            // Select the current folder as the preferred one - i.e. rename into the same folder
            newFolderSpinner.setSelection(0);
            for (int w = 0; w < newtempfolders.size(); w++) {
                if (FullscreenActivity.currentFolder.equals(newtempfolders.get(w)) ||
                        FullscreenActivity.currentFolder.equals("(" + newtempfolders.get(w) + ")")) {
                    newFolderSpinner.setSelection(w);
                    FullscreenActivity.newFolder = newtempfolders.get(w);
                }
            }
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (getfolders!=null) {
            getfolders.cancel(true);
        }
        this.dismiss();
    }

}
