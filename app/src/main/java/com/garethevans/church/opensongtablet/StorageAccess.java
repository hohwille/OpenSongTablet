package com.garethevans.church.opensongtablet;

// This class is used to expose storage locations and permissions used by the app (in the near future).
// Still a work in progress

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.support.annotation.RequiresApi;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

class StorageAccess {

    String utf = "UTF-8";
    private String[] foldersNeeded = {"Backgrounds", "Export", "Fonts", "Highlighter", "Images",
            "Media", "Notes", "OpenSong Scripture", "Pads", "Profiles", "Received", "Scripture",
            "Sets", "Settings", "Slides", "Songs", "Variations"};
    private String[] cacheFoldersNeeded = {"Images/_cache", "Notes/_cache", "Scripture/_cache",
            "Slides/_cache"};
    String appFolder = "TestOpenSong";
    Uri treeUri;
    private DocumentFile homeFolder;

    Uri getTreeUri(Context c) {
        Preferences mPreferences = new Preferences();
        String s = mPreferences.getMyPreferenceString(c, "treeUri", null);

        // If we are pre-lollipop, enforce internal default storage
        if (!isLollipop()) {
            // For Lollipop, we have to default to the default external storage allowed
            s = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        // Convert the string to a uri and check if the path ends with OpenSong
        if (s != null && !s.equals("")) {
            treeUri = Uri.parse(s);
        }
        return treeUri;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void getSongFolderContents(Context c) {
        ContentResolver contentResolver = c.getContentResolver();
        List<Uri> dirNodes = new LinkedList<>();

        try {
            // Initialise the songs and folder documentIds
            FullscreenActivity.allSongs = new ArrayList<>();
            FullscreenActivity.allSongFolders = new ArrayList<>();

            String treeUriId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeUriId);

            // Keep track of our directory hierarchy
            dirNodes.add(childrenUri);

            while (!dirNodes.isEmpty()) {
                childrenUri = dirNodes.remove(0); // get the item from top
                Cursor cur = contentResolver.query(childrenUri, new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null);
                try {
                    if (cur != null) {
                        while (cur.moveToNext()) {
                            final String docId = cur.getString(0);
                            final String mime = cur.getString(2);

                            if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                                if (docId.contains(appFolder)) {
                                    // Sub directories within app folder only are added as search nodes
                                    final Uri newNode = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId);
                                    dirNodes.add(newNode);
                                    if (docId.contains(appFolder + "/Songs")) {
                                        // Only add song folders to the database though
                                        FullscreenActivity.allSongFolders.add(docId);
                                    }
                                }
                            } else {
                                if (docId.contains(appFolder + "/Songs")) {
                                    FullscreenActivity.allSongs.add(docId);
                                }
                            }
                        }
                        cur.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Sort the folders alphabetically
        Collator coll = Collator.getInstance(FullscreenActivity.locale);
        coll.setStrength(Collator.SECONDARY);
        Collections.sort(FullscreenActivity.allSongFolders, coll);
        // Add MAIN to the top
        FullscreenActivity.allSongFolders.add(0,c.getString(R.string.mainfoldername));

        // Sort the songs alphabetically
        Collections.sort(FullscreenActivity.allSongs, coll);



    }

    ArrayList<String> getFileDetailsFromUri(Context c, Uri uri) {
        // This returns an array with the appropriate bits
        ArrayList<String> songdetails = new ArrayList<>();
        String filename = null;
        String filesize = "1";
        Cursor cursor = c.getContentResolver().query(uri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                filesize = cursor.getString(cursor.getColumnIndex(OpenableColumns.SIZE));

            }
        } catch (Exception e) {
            Log.d("d","Storage Access.  Problem getting filename from uri");
            e.printStackTrace();
        } finally {
            if (cursor!=null) {
                cursor.close();
            }
        }

        if (filename == null) {
            filename = uri.getPath();
            int cut = filename.lastIndexOf('/');
            if (cut != -1) {
                filename = filename.substring(cut + 1);
            }
        }

        if (filesize == null) {
            filesize = "1";
        }

        songdetails.add(filename);
        songdetails.add(filesize);

        return songdetails;
    }

    InputStream getInputStream(Context c, DocumentFile home, String where, String subfolder, String filename) {
        homeFolder = home;
        Uri uri = getFileLocationAsUri(c, home, where, subfolder, filename);
        InputStream is;
        if (uri == null) {
            return null;
        }
        try {
            is = c.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            is = null;
        }
        return is;
    }
    InputStream getInputStreamFromUri(Context c, Uri uri) {
        InputStream is;
        if (uri == null) {
            return null;
        }
        try {
            is = c.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            is = null;
        }
        return is;
    }
    OutputStream getOutputStream(Context c, DocumentFile home, String where, String subfolder, String filename) {
        homeFolder = home;
        Uri uri = getFileLocationAsUri(c, home, where, subfolder, filename);
        OutputStream os;
        if (uri == null) {
            return null;
        }
        try {
            os = c.getContentResolver().openOutputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            os = null;
        }
        return os;
    }
    OutputStream getOutputStreamFromUri(Context c, Uri uri) {
        OutputStream os;
        if (uri == null) {
            return null;
        }
        try {
            os = c.getContentResolver().openOutputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            os = null;
        }
        return os;
    }

    DocumentFile getFileLocationAsDocumentFile(Context c, DocumentFile home, String where, String subfolder, String filename) {
        homeFolder = home;
        // Firstly get the home folder location
        if (homeFolder==null || !homeFolder.exists()) {
            homeFolder = getHomeFolder(c);
        }

        if (subfolder.equals("../Received")) {
            where = "Received";
            subfolder = "";
        }

        DocumentFile df_where;
        if (homeFolder != null) {
            try {
                // Get the required subfolder of OpenSong using 'where'.
                // This could be the Songs, Sets, Variables, etc. folder
                // These should all be there as they are checked on boot
                df_where = homeFolder.findFile(where);

                // Check that the filename doesn't have folders in it.
                // If it does, add add them to the subfolder string
                if (filename.contains("/")) {
                    // Ok, so split it up
                    String[] splitup = filename.split("/");
                    // Last one is the actual filename
                    filename = splitup[splitup.length-1];
                    splitup[splitup.length-1] = "";

                    // Add the additional folders
                    StringBuilder subfolderBuilder = new StringBuilder(subfolder);
                    for (String f:splitup) {
                        if (!f.equals((""))) {
                            subfolderBuilder.append("/");
                        }
                    }
                    subfolder += subfolder + "/" + subfolderBuilder.toString();
                }

                // Go through the folders (may be lots of sub folders, or none) and get the folder uri
                String[] folders = subfolder.split("/");
                DocumentFile folderLocation = df_where;
                for (String folder : folders) {
                    if (!folder.equals(c.getString(R.string.mainfoldername)) && !folder.equals("")
                            && df_where!=null && df_where.exists()) {
                        DocumentFile newFolderLocation = folderLocation.findFile(folder);
                        if (newFolderLocation == null) {
                            // Create the folder
                            folderLocation.createDirectory(folder);
                            newFolderLocation = folderLocation.findFile(folder);
                        }
                        folderLocation = newFolderLocation;
                    }
                }

                // Now for the file
                DocumentFile df = null;
                if (folderLocation!=null) {
                    df = folderLocation.findFile(filename);
                }

                // If the file doesn't exist, create
                if (df==null && filename.equals("") && folderLocation!=null) {
                    // No file specified, so send the folder
                    return folderLocation;
                } else if (df == null && !filename.equals("ReceivedSong") && folderLocation!=null) {
                    // File not found, so create it
                    folderLocation.createFile(null, filename);
                    df = folderLocation.findFile(filename);
                    return df;
                } else if (df!=null && df.exists() && df.isFile()) {
                    return df;
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }
    Uri getFileLocationAsUri(Context c, DocumentFile home, String where, String subfolder, String filename) {
        homeFolder = home;
        if (subfolder.equals("../Received")) {
            where = "Received";
            subfolder = "";
        }
        DocumentFile df = getFileLocationAsDocumentFile(c, home, where, subfolder, filename);
        if (df!=null) {
            return df.getUri();
        } else {
            return null;
        }
    }
    String getTempFolderLocation(Context c, String where) {
        if (where.equals(FullscreenActivity.mainfoldername)) {
            where = "";
        } else if (where.contains("**" + c.getResources().getString(R.string.note))) {
            where = "Notes/_cache";
        } else if (where.contains("**" + c.getResources().getString(R.string.image))) {
            where = "Images/_cache";
        } else if (where.contains("**" + c.getResources().getString(R.string.scripture))) {
            where = "Scripture/_cache";
        } else if (where.contains("**" + c.getResources().getString(R.string.slide))) {
            where = "Slides/_cache";
        } else if (where.contains("**" + c.getResources().getString(R.string.variation))) {
            where = "Variations";
        }
        return where;
    }

    String getUTFEncoding(Context c, DocumentFile home, String where, String subfolder, String filename) {
        // Try to determine the BOM for UTF encoding
        InputStream is = null;
        UnicodeBOMInputStream ubis = null;
        homeFolder = home;
        try {
            is = getInputStream(c, home, where, subfolder, filename);
            ubis = new UnicodeBOMInputStream(is);
            utf = ubis.getBOM().toString();

        } catch (Exception e) {
            FullscreenActivity.myXML = "<title>OpenSongApp</title>\n<author></author>\n<lyrics>"
                    + c.getResources().getString(R.string.songdoesntexist) + "\n\n" + "</lyrics>";
            FullscreenActivity.myLyrics = "ERROR!";
            utf = null;
        }
        try {
            if (is != null) {
                is.close();
            }
            if (ubis != null) {
                ubis.close();
            }
        } catch (Exception e) {
            // Error closing
        }
        return utf;
    }
    String getUTFEncodingFromUri(Context c, Uri uri) {
        // Try to determine the BOM for UTF encoding
        InputStream is = null;
        UnicodeBOMInputStream ubis = null;

        try {
            is = getInputStreamFromUri(c, uri);
            ubis = new UnicodeBOMInputStream(is);
            utf = ubis.getBOM().toString();

        } catch (Exception e) {
            FullscreenActivity.myXML = "<title>OpenSongApp</title>\n<author></author>\n<lyrics>"
                    + c.getResources().getString(R.string.songdoesntexist) + "\n\n" + "</lyrics>";
            FullscreenActivity.myLyrics = "ERROR!";
            utf = null;
        }
        try {
            if (is != null) {
                is.close();
            }
            if (ubis != null) {
                ubis.close();
            }
        } catch (Exception e) {
            // Error closing
        }
        return utf;
    }

    String readTextFile(Context c, InputStream is) {
        String text = "";
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }
            text = total.toString();
        } catch (Exception e) {
            e.printStackTrace();
            ShowToast showtoast = new ShowToast();
            showtoast.showToastMessage(c, c.getString(R.string.error));
        } catch (OutOfMemoryError oom) {
            oom.printStackTrace();
            ShowToast showtoast = new ShowToast();
            showtoast.showToastMessage(c, "OOM - " + c.getString(R.string.error));
        }
        return text;
    }
    byte[] readBytes(InputStream is) {
        try {
            // this dynamically extends to take the bytes you read
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

            // this is storage overwritten on each iteration with bytes
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            // we need to know how may bytes were read to write them to the byteBuffer
            int len;
            while ((len = is.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }

            // and then we can return your byte array.
            is.close();
            return byteBuffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("d", "Error reading bytes from input stream");
            return null;
        } catch (OutOfMemoryError oom) {
            oom.printStackTrace();
            Log.d("d","Out of memory");
            return null;
        }
    }
    boolean writeBytes(Context c, OutputStream os, byte[] bytes) {
        try {
            os.write(bytes);
            os.flush();
            os.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            ShowToast st = new ShowToast();
            st.showToastMessage(c, c.getString(R.string.error));
            return false;
        }
    }
    void writeStringToFile(Context c, OutputStream os, String s) {
        writeBytes(c, os, s.getBytes());
    }
    void writeDocumentFile(Context c, Uri uri, String what, String s, Bitmap bmp) {
        OutputStream out = getOutputStreamFromUri(c, uri);
        if (what.equals("png")) {
            try {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                writeBytes(c, out, s.getBytes());
            } catch (Exception e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }
    }

    boolean moveAFile(Context c, DocumentFile from, DocumentFile to) {
        // This copies and then deletes the original
        if (copyFile(c, getInputStreamFromUri(c, from.getUri()),getOutputStreamFromUri(c, to.getUri()))) {
            from.delete();
            return true;
        } else {
            return false;
        }
    }
    boolean renameAFile(Context c, DocumentFile home, String where, String subfolder, String oldname, String newname) {
        homeFolder = home;
        // Get the original DocumentFile
        // Since the subfolder may change, the newname will include any subfolder - to be parsed later.
        DocumentFile oldFile = getFileLocationAsDocumentFile(c, home, where, subfolder, oldname);
        return oldFile.renameTo(newname);
    }
    boolean renameSongFolder(Context c, DocumentFile home, String oldfolder, String newfolder) {
        // Get the documentfiles
        DocumentFile from = getFileLocationAsDocumentFile(c, home, "Songs", oldfolder, "");
        return from.renameTo(newfolder);
    }
    boolean copyFile(Context c, InputStream is, OutputStream os) {
        return writeBytes(c, os, readBytes(is));
    }

    DocumentFile getHomeFolder(Context c) {
        DocumentFile locator;
        try {
            // Get the treeUri
            if (treeUri==null) {
                treeUri = getTreeUri(c);
            }

            // treeUri is also set - this is the top level folder we have permission for
            if (treeUri!=null) {
                locator = getDocumentFileFromUri(c, treeUri);

                if (locator != null && locator.exists()) {

                    // Decide if we need to create the OpenSong folder, or if we are already in it
                    if (locator.findFile(appFolder) != null) {
                        // We are in the level above the OpenSong folder
                        locator = locator.findFile(appFolder);

                    } else if (locator.getParentFile() != null && locator.getParentFile().getName().equals(appFolder)) {
                        // We are already in the OpenSong folder
                        // Do nothing;
                        locator = getDocumentFileFromUri(c, treeUri);
                    } else {
                        // Fresh location, so need to create the directory
                        locator.createDirectory(appFolder);
                        locator = locator.findFile(appFolder);
                    }

                }
                homeFolder = locator;
                return locator;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    String checkRootFoldersExist(Context c, DocumentFile home) {
        homeFolder = home;
        StringBuilder allok = new StringBuilder();

        // Firstly check for the OpenSong folder at the parent of the tree uri
        if (homeFolder==null || !homeFolder.exists()) {
            homeFolder = getHomeFolder(c);
        }

        // Now we go through all of the main folders
        DocumentFile checkIt;
        if (homeFolder!=null) {
            for (String folder : foldersNeeded) {
                checkIt = tryCreateDirectory(c, home, folder, "");
                if (checkIt == null || !checkIt.exists()) {
                    allok.append(folder).append("-Error\n");
                } else {
                    allok.append(folder).append("-Done: ").append(checkIt.getUri()).append("\n");
                }
            }
            for (String cachefolder : cacheFoldersNeeded) {
                // Get the where and _cache folders
                String[] bits = cachefolder.split("/");
                checkIt = tryCreateDirectory(c, home, bits[0], bits[1]);
                if (checkIt == null || !checkIt.exists()) {
                    allok.append(cachefolder).append("-Error\n");
                } else {
                    allok.append(cachefolder).append("-Done: ").append(checkIt.getUri()).append("\n");
                }
            }
        }
        return allok.toString();
    }
    void clearCacheFolders(Context c, DocumentFile home) {
        homeFolder = home;
        for (String cachefolder:cacheFoldersNeeded) {
            // The cache folders are in two parts - the where and the subfolder
            String[] bits = cachefolder.split("/");
            DocumentFile df = getFileLocationAsDocumentFile(c,home, bits[0],bits[1],"");
            DocumentFile[] cachefiles = df.listFiles();
            for (DocumentFile file:cachefiles) {
                file.delete();
            }
        }
    }

    DocumentFile tryCreateSongSubFolder(Context c, DocumentFile home, String newfolder) {
        homeFolder = home;
        DocumentFile folder = tryCreateDirectory(c,home, "Songs","");

        // Split the subfolder(s) up
        String[] subfolders = newfolder.split("/");
        for (String subfolder:subfolders) {
            // Look for it
            if (folder.findFile(subfolder)==null) {
                folder.createDirectory(subfolder);
            }
            // Set this folder as the new parent to look for children
            folder = folder.findFile(subfolder);
        }
        return folder;
    }

    DocumentFile tryCreateDirectory(Context c, DocumentFile home, String where, String subfolder) {
        homeFolder = home;
        // The 'where' folder is fine - it's inside the OpenSong folder defined in code by me!
        // Get the base app  storage location if it isn't set
        if (homeFolder==null || !homeFolder.exists()) {
            homeFolder = getHomeFolder(c);
        }

        DocumentFile df_where = null;

        if (homeFolder!=null && homeFolder.exists()) {
            // Now check for the 'where' folder.  Create it if it is missing
            df_where = homeFolder.findFile(where);

            if (df_where == null || !df_where.exists()) {
                homeFolder.createDirectory(where);
                df_where = homeFolder.findFile(where);
            }

            if (subfolder != null && !subfolder.equals("")) {
                // Iterate through the subfolders one at a time and create them if they don't exist
                String[] folder = subfolder.split("/");
                DocumentFile df_sub;

                for (String f : folder) {
                    if (f != null && !f.equals("") && df_where != null) {
                        df_sub = df_where.findFile(f);
                        if (df_sub == null || !df_sub.exists()) {
                            df_where.createDirectory(f);
                        }
                        df_where = df_where.findFile(f);
                    }
                }
            }
        }
        return df_where;
    }

    DocumentFile getDocumentFileFromUri(Context c, Uri uri) {
        // Lollipop+ has to use fromTreeUri, where as KitKat can't use this, so uses from SingleUri instead
        if (isLollipop()) {
            return DocumentFile.fromTreeUri(c, uri);
        } else {
            return DocumentFile.fromSingleUri(c, uri);
        }
    }


    DocumentFile getDirectory(Context c, DocumentFile home, String folder) {
        homeFolder = home;
        // Get the storage location
        if (homeFolder==null || !homeFolder.exists()) {
            homeFolder = getHomeFolder(c);
        }

        DocumentFile df = homeFolder;
        // If we send subfolders, split them
        String[] folders = folder.split("/");


        DocumentFile df_sub = df;
        if (df_sub!=null) {
            Log.d("d", "df.getUri=" + df.getUri());
        }
        for (String f:folders) {
            if (f!=null && !f.equals("") && df!=null && df_sub!=null) {
                Log.d("d", "f=" + f);
                if (df_sub.findFile(f)!=null) {
                    df_sub = df_sub.findFile(f);
                    if (df_sub!=null) {
                        Log.d("d", "Found it - df_sub.getUri=" + df_sub.getUri());
                    }
                } else {
                    df_sub = df;
                    if (df_sub!=null) {
                        Log.d("d", "Didn't find it df.getUri=" + df_sub.getUri());
                    }
                }
            }
        }
        return df_sub;
    }

    DocumentFile getLocalisedFile(Context c, DocumentFile home, String path) {
        homeFolder = home;
        // These are read only, so can use fromSingleUri
        DocumentFile doc;
        if (path==null) {
            path="";
        }
        // This is used to change the location to the specific location in the OpenSong folder
        // This is to allow users to sync all files.
        // Localised files are saved with the location starting ../OpenSong/
        if (path.startsWith("../OpenSong/")) {
            path = path.replace("../OpenSong/","");
            doc = getFileLocationAsDocumentFile(c,home,"","",path);
        } else {
            doc = DocumentFile.fromSingleUri(c, Uri.parse(path));
        }
        return doc;
    }

    void copyAssets(Context c, DocumentFile home) {
        homeFolder = home;
        AssetManager assetManager = c.getAssets();
        String[] files = new String[2];
        files[0] = "backgrounds/ost_bg.png";
        files[1] = "backgrounds/ost_logo.png";
        for (String filename : files) {
            try {
                InputStream in = assetManager.open(filename);
                OutputStream out = getOutputStream(c, home, "Backgrounds","",filename.replace("backgrounds/",""));
                copyFile(c, in, out);
            } catch(Exception e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            }
        }
    }

    boolean isLollipop() {
        return Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP;
    }

    String returnBlankForMain(Context c, String whichSongFolder) {
        if (whichSongFolder.equals(c.getString(R.string.mainfoldername))) {
            return "";
        } else {
            return whichSongFolder;
        }
    }

}


