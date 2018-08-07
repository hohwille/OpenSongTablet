package com.garethevans.church.opensongtablet;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;


// Stuff changed by Gareth to implement StorageAccessFramework




// File created by James on 10/22/17.

//Todo refactor stream statement if necessary (performance) and use collection

/* final class - uninheritable -   Private member folderList accessible by getter
* getFolderlistasList which initialises the folderList if it is null, and then
* populates it with the folderList, which is parsed to remove the path prefix.
* */
final class SongFileList {
    private ArrayList<String>   folderList;
    private ArrayList<String>   currentFileList;
    private String              topLevelFilePath;
    Collator coll;

    // constructor
    SongFileList() {
        folderList = new ArrayList<>();
        currentFileList = new ArrayList<>();
    }

    /*getters and setters*/
    /*getFolderList - package private, returns Array of String
    * creates list of folders and caches it in private class variable
    * which it then returns*/
    @NonNull
    String[] getFolderList(Context c, DocumentFile homeFolder) {
        if (!folderList.isEmpty()) {
            // initialize toArray[T] with empty array vs size -> https://shipilev.net/blog/2016/arrays-wisdom-ancients/
            // Sort the folder list alphabetically
            try {
                coll = Collator.getInstance(FullscreenActivity.locale);
                coll.setStrength(Collator.SECONDARY);
                Collections.sort(folderList, coll);
            } catch (Exception e) {
                // Error sorting
                Log.d("d","Error sorting");
            }
            return folderList.toArray(new String[folderList.size()].clone());
        } else {
            StorageAccess storageAccess = new StorageAccess();
            DocumentFile topLevelFile = storageAccess.getDirectory(c, homeFolder, "Songs");
            topLevelFilePath = topLevelFile.getUri().getPath();
            initialiseFolderList(topLevelFile);
            postprocessListPath();
            try {
                coll = Collator.getInstance(FullscreenActivity.locale);
                coll.setStrength(Collator.SECONDARY);
                Collections.sort(folderList, coll);
            } catch (Exception e) {
                // Error sorting
                Log.d("d","Error sorting");
            }
            return folderList.toArray(new String[folderList.size()]).clone();
        }
    }

    /*this function simply strips the leading prefix from the file path*/
    private void postprocessListPath() {

        if (topLevelFilePath!=null) {
            //replaceAll(unaryComp) is only available for newer versions of Android.
            // Added a check and alternative for older versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                UnaryOperator<String> unaryComp = new UnaryOperator<String>() {
                    @Override
                    public String apply(String i) {
                        return i.substring(topLevelFilePath.length() + 1);
                    }
                };
                folderList.replaceAll(unaryComp);

            } else {
                for (int z = 0; z < folderList.size(); z++) {
                    String new_string = folderList.get(z).substring(topLevelFilePath.length() + 1);
                    folderList.set(z, new_string);
                }
            }
            folderList.add(0, FullscreenActivity.mainfoldername);
        }
    }

    /*getSongFileList() - package private, returns array of String
    * returns an array of the file names of the currently chosen folder
    * */
    String[] getSongFileListasArray(Context c, DocumentFile homeFolder) {
        //todo place check here to see if new file has been added since the last file list was
        //constructed.  This saves memory.
        fileList(c, homeFolder);
        return currentFileList.toArray(new String[currentFileList.size()]).clone();
    }

    /* a getter to return a list, should it be required. */
    List<String> getSongFileListasList(Context c, DocumentFile homeFolder) {
        //todo datastructure to encapsulate currentFileList and include invalidate
        //code, perhaps event handling?
        fileList(c, homeFolder);
        return currentFileList;
    }
    /*private function to modify currentFileList by scanning the currently selected
    * folder
    * */
    private void fileList(Context c, DocumentFile homeFolder) {
        currentFileList.clear();
        StorageAccess sa = new StorageAccess();
        DocumentFile foldertoindex = sa.getFileLocationAsDocumentFile(c,homeFolder,"Songs",
                FullscreenActivity.whichSongFolder,"");

        DocumentFile[] flist = foldertoindex.listFiles();

        // Not liking the comparator sort.  Reverse folder sorting
        // Create two arrays: one for folders, one for songs
        ArrayList<String> folders_found = new ArrayList<>();
        ArrayList<String> songs_found = new ArrayList<>();

        for (DocumentFile f:flist) {
            if (f.isDirectory()) {
                folders_found.add(f.getName());
            } else {
                songs_found.add(f.getName());
            }
        }

        // Now sort both individually
        // Sort the folder list
        try {
            coll = Collator.getInstance(FullscreenActivity.locale);
            coll.setStrength(Collator.SECONDARY);
            Collections.sort(folders_found, coll);
        } catch (Exception e) {
            // Error sorting
            Log.d("d","Error sorting");
        }

        // Now sort the songs
        try {
            coll = Collator.getInstance(FullscreenActivity.locale);
            coll.setStrength(Collator.SECONDARY);
            Collections.sort(songs_found, coll);
        } catch (Exception e) {
            // Error sorting
            Log.d("d","Error sorting");
        }

        // Now join the two arrays back together
        currentFileList.addAll(folders_found);
        currentFileList.addAll(songs_found);

    }

    /*intialises the folderList variable*/
    private void initialiseFolderList(DocumentFile rfile) {
        try {
            DocumentFile[] listfiles = rfile.listFiles();
            if ((listfiles != null) && (listfiles.length > 0)) {
                for (DocumentFile file : rfile.listFiles()) {
                    if (file.isDirectory()) {
                        folderList.add(file.toString());
                        initialiseFolderList(file);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}