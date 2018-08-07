

package com.garethevans.church.opensongtablet;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PopUpDirectoryChooserFragment extends DialogFragment {

    static PopUpDirectoryChooserFragment newInstance() {
        PopUpDirectoryChooserFragment frag;
        frag = new PopUpDirectoryChooserFragment();
        return frag;
    }

    public interface MyInterface {
        void updateCustomStorage();
        //void updateLinksPopUp();
        void openFragment();
    }

    interface SettingsInterface {
        void openStorageFragment();
    }

    private MyInterface mListener;
    private SettingsInterface sListener;

    @Override
    @SuppressWarnings("deprecation")
    public void onAttach(Activity activity) {
        if (FullscreenActivity.whattodo.equals("splashpagestorage")) {
            sListener = (SettingsInterface) activity;
        } else {
            mListener = (MyInterface) activity;
        }
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    ImageView navigateUp;
    public TextView currentFolder;
    ListView directoryList;
    public static DocumentFile location;
    public static List<String> splitlocation;
    public static List<String> tempProperDirectories;
    public static List<String> tempProperDirectoriesAndFiles;
    public static List<String> tempProperFolders;
    public static List<String> tempProperFiles;
    static Collator coll;
    String chooserAction;
    public Context context;
    FloatingActionButton selectButton;
    TextView isWritableText;
    TextView extraText;
    boolean isinsideopensongfolder = false;
    StorageAccess storageAccess;
    DocumentFile homeFolder;

    public void onStart() {
        super.onStart();

        // safety check
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
        storageAccess = new StorageAccess();
        homeFolder = storageAccess.getHomeFolder(getActivity());
        getDialog().setTitle(getActivity().getResources().getString(R.string.storage_choose));
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(true);
        View V = inflater.inflate(R.layout.popup_folderexplorer, container, false);
        context = getActivity().getBaseContext();

        TextView title = V.findViewById(R.id.dialogtitle);
        if (chooserAction!=null && chooserAction.equals("findosbfile")) {
            title.setText(getActivity().getResources().getString(R.string.backup_import));
        } else {
            title.setText(getActivity().getResources().getString(R.string.storage_choose));
        }
        final FloatingActionButton closeMe = V.findViewById(R.id.closeMe);
        closeMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomAnimations.animateFAB(closeMe,getActivity());
                closeMe.setEnabled(false);
                try {
                    dismiss();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        selectButton = V.findViewById(R.id.saveMe);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomAnimations.animateFAB(selectButton,getActivity());
                selectButton.setEnabled(false);
                doSave();
            }
        });
        if (chooserAction != null && chooserAction.contains("file")) {
            selectButton.setVisibility(View.GONE);
        }

        FullscreenActivity.filechosen = null;

        location = storageAccess.getFileLocationAsDocumentFile(getActivity(),homeFolder,"","","");

        currentFolder = V.findViewById(R.id.currentFolderText);
        currentFolder.setText(location.toString());
        isWritableText = V.findViewById(R.id.isWritableText);
        extraText = V.findViewById(R.id.extraText);
        extraText.setVisibility(View.GONE);

        // Identify the listview which will either just show folders, or folders and files
        directoryList = V.findViewById(R.id.folderListView);

        // Set up the navigate up arrow button
        navigateUp = (FloatingActionButton) V.findViewById(R.id.upFolderButton);
        navigateUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doNavigateUp();
            }
        });

        // Decide if we are using the fragment as a folder chooser or a file chooser
        // If it is a file chooser, we need to hide the select/ok button
        // We need to set the appropriate action for the navigate up and list views
        // tempProperDirectoriesAndFiles or tempProperDirectories will populate the listview
        chooserAction = getArguments().getString("type");
        if (chooserAction!=null && chooserAction.equals("file")) {
            // File browser mode (navigate through folders, clicking on a file gets its path
            // List all folders and files in the current location
            listFoldersAndFiles();

        } else if (chooserAction!=null && chooserAction.equals("findosbfiles")) {
            // File browser mode (navigate through folders, clicking on a file gets its path
            // List all folders and files in the current location
            listFoldersAndFilesOSB();

        } else {
            // Folder browser mode (navigate through folders, clicking on ok gets its path)
            // List all folders in the current location
            listFolders();
        }

        directoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the item clicked
                String itemclicked;

                if (chooserAction.contains("file")) {
                    if (tempProperDirectoriesAndFiles.size() > position) {
                        itemclicked = tempProperDirectoriesAndFiles.get(position);
                    } else {
                        if (chooserAction.equals("file")) {
                            listFoldersAndFiles();
                        } else {
                            listFoldersAndFilesOSB();
                        }
                        itemclicked = "";
                    }
                } else {
                    if (tempProperDirectories.size() > position) {
                        itemclicked = tempProperDirectories.get(position);
                    } else {
                        listFolders();
                        itemclicked = "";
                    }
                }
                itemclicked = itemclicked.replace("/", "");
                // Check if link clicked is a folder or a file.

                //Use the standard file browsing features
                Uri newloc = Uri.withAppendedPath(location.getUri(), itemclicked);
                location = DocumentFile.fromSingleUri(getActivity(), newloc);
                if (location.isDirectory()) {
                    // List the new folder contents
                    if (chooserAction.contains("file")) {
                        if (chooserAction.equals(("file"))) {
                            listFoldersAndFiles();
                        } else {
                            listFoldersAndFilesOSB();
                        }
                    } else {
                        listFolders();
                    }
                } else {
                    // This is the file we want (folder chooser won't list files!)
                    FullscreenActivity.filechosen = location;
                    if (chooserAction.equals("file")) {
                        mListener.updateCustomStorage();
                    } else if (chooserAction.equals("findosbfiles")) {
                        FullscreenActivity.whattodo = "processimportosb";
                        mListener.openFragment();
                    }
                    if (FullscreenActivity.filetoselect.equals("audiolink") || FullscreenActivity.filetoselect.equals("otherlink")) {
                        //mListener.updateLinksPopUp();
                        FullscreenActivity.whattodo = "page_links";
                        mListener.openFragment();

                    }
                    try {
                        dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        checkCanWrite();

        PopUpSizeAndAlpha.decoratePopUp(getActivity(),getDialog());

        return V;
    }

    public void doSave() {
        //StorageChooser.customStorageLoc = location;
        FullscreenActivity.customStorage = location.toString();
        Preferences.savePreferences();
        if (FullscreenActivity.whattodo.equals("splashpagestorage")) {
            sListener.openStorageFragment();
        } else {
            mListener.updateCustomStorage();
        }

        try {
            dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkCanWrite() {
        // If folder is writeable, set the button to OK, If not, disable it.
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Storage permission has not been granted.
            Log.d("d","PERMISSION DENIED");
        } else {
            Log.d("d","PERMISSION ALLOWED");
        }
        if (location.canWrite()) {
            if (selectButton != null) {
                selectButton.setVisibility(View.VISIBLE);
                selectButton.setEnabled(true);
            }
            isWritableText.setVisibility(View.INVISIBLE);
        } else {
            if (selectButton != null) {
                selectButton.setVisibility(View.GONE);
                selectButton.setEnabled(false);
            }
            isWritableText.setVisibility(View.VISIBLE);
        }
    }

    public void doNavigateUp() {
        // If we can, remove the last part of the folder name
        // Use the standard file browsing method
        if (splitlocation != null && splitlocation.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (String s:splitlocation) {
                sb.append(s);
                sb.append(s);
            }
            Uri newuri = Uri.parse(sb.toString());
            location = DocumentFile.fromSingleUri(getActivity(),newuri);
            try {
                if (chooserAction.contains("file")) {
                    if (chooserAction.equals("file")) {
                        listFoldersAndFiles();
                    } else {
                        listFoldersAndFilesOSB();
                    }
                } else {
                    listFolders();
                }
            } catch (Exception e) {
                FullscreenActivity.myToastMessage = getResources().getString(R.string.error);
                ShowToast.showToast(getActivity());
            }
        }
    }

    public void listFoldersAndFiles() {
        splitlocation = location.getUri().getPathSegments();
        DocumentFile[] tempmyitems = location.listFiles();

        //Now set the size of the temp arrays
        tempProperFolders = new ArrayList<>();
        tempProperFiles = new ArrayList<>();
        tempProperDirectoriesAndFiles = new ArrayList<>();

        //Now read the stuff into the temp array
        if (tempmyitems!=null) {
            for (DocumentFile tempmyitem : tempmyitems) {
                if (tempmyitem != null) {
                    if (tempmyitem.isDirectory()) {
                        tempProperFolders.add(tempmyitem.getName() + "/");
                    } else {
                        tempProperFiles.add(tempmyitem.getName());
                    }
                }
            }

            //Sort these arrays
            // Add locale sort
            if  (FullscreenActivity.locale==null) {
                FullscreenActivity.locale = new Locale(Locale.getDefault().getDisplayLanguage());
            }
            coll = Collator.getInstance(FullscreenActivity.locale);
            coll.setStrength(Collator.SECONDARY);
            Collections.sort(tempProperFolders, coll);
            Collections.sort(tempProperFiles, coll);

            if (tempProperFolders!=null) {
                tempProperDirectoriesAndFiles.addAll(tempProperFolders);
            }
            if (tempProperFiles!=null) {
                tempProperDirectoriesAndFiles.addAll(tempProperFiles);
            }

            // Update the listView with the folders
            ArrayAdapter<String> listAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, tempProperDirectoriesAndFiles);
            directoryList.setAdapter(listAdapter);
            currentFolder.setText(location.toString());
        }
    }

    public void listFoldersAndFilesOSB() {
        splitlocation = location.getUri().getPathSegments();
        DocumentFile[] tempmyitems = location.listFiles();

        //Now set the size of the temp arrays
        tempProperFolders = new ArrayList<>();
        tempProperFiles = new ArrayList<>();
        tempProperDirectoriesAndFiles = new ArrayList<>();

        //Now read the stuff into the temp array
        if (tempmyitems!=null) {
            for (DocumentFile tempmyitem : tempmyitems) {
                if (tempmyitem != null) {
                    if (tempmyitem.isDirectory()) {
                        tempProperFolders.add(tempmyitem.getName() + "/");
                    } else {
                        if (tempmyitem.getName().endsWith(".osb")) {
                            tempProperFiles.add(tempmyitem.getName());
                        }
                    }
                }
            }

            //Sort these arrays
            // Add locale sort
            if  (FullscreenActivity.locale==null) {
                FullscreenActivity.locale = new Locale(Locale.getDefault().getDisplayLanguage());
            }
            coll = Collator.getInstance(FullscreenActivity.locale);
            coll.setStrength(Collator.SECONDARY);
            Collections.sort(tempProperFolders, coll);
            Collections.sort(tempProperFiles, coll);

            if (tempProperFolders!=null) {
                tempProperDirectoriesAndFiles.addAll(tempProperFolders);
            }
            if (tempProperFiles!=null) {
                tempProperDirectoriesAndFiles.addAll(tempProperFiles);
            }

            // Update the listView with the folders
            ArrayAdapter<String> listAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, tempProperDirectoriesAndFiles);
            directoryList.setAdapter(listAdapter);
            currentFolder.setText(location.toString());
        }
    }

    public void listFolders() {

        tempProperDirectories = new ArrayList<>();
        isinsideopensongfolder = false;

        boolean foundSongsFolder = false;
        boolean foundSetsFolder = false;
        boolean foundOpenSongFolder = false;

        // This searches the SD card as a standard file location
        splitlocation = location.getUri().getPathSegments();
        DocumentFile[] tempmyitems = location.listFiles();
        int tempnumitems;
        if (tempmyitems != null && tempmyitems.length > 0) {
            tempnumitems = tempmyitems.length;
        } else {
            tempnumitems = 0;
        }
        int numactualdirs = 0;
        for (int x = 0; x < tempnumitems; x++) {
            if (tempmyitems[x] != null && tempmyitems[x].isDirectory()) {
                numactualdirs++;
            }
        }
        //Now read the stuff into the temp array
        for (int x = 0; x < numactualdirs; x++) {
            if (tempmyitems[x] != null && tempmyitems[x].isDirectory()) {
                if (location.toString().endsWith("OpenSong")) {
                    if (tempmyitems[x].getName().equals("Sets")) {
                        foundSetsFolder = true;
                    } else if (tempmyitems[x].getName().equals("Songs")) {
                        foundSongsFolder = true;
                    }
                } else if (tempmyitems[x].getName().equals("OpenSong")) {
                    foundOpenSongFolder = true;
                }
                tempProperDirectories.add(tempmyitems[x].getName());
            }
        }

        currentFolder.setText(location.toString());

        extraText.setVisibility(View.GONE);
        isinsideopensongfolder = foundSetsFolder && foundSongsFolder;
        if (isinsideopensongfolder) {
            extraText.setVisibility(View.VISIBLE);
            extraText.setBackgroundColor(0xFF880000);
            extraText.setText(getString(R.string.nothere));
        }

        if (foundOpenSongFolder) {
            extraText.setVisibility(View.VISIBLE);
            extraText.setBackgroundColor(0xFF008800);
            extraText.setText(getString(R.string.previouslyused));
        }

        //Sort these arrays
        if (FullscreenActivity.locale!=null) {
            coll = Collator.getInstance(FullscreenActivity.locale);
        } else {
            coll = Collator.getInstance();
        }
        coll.setStrength(Collator.SECONDARY);
        Collections.sort(tempProperDirectories, coll);

        // Update the listView with the folders
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, tempProperDirectories);
        directoryList.setAdapter(listAdapter);

        checkCanWrite();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        try {
            this.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}