package com.garethevans.church.opensongtablet;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Collator;
import java.util.ArrayList;

public class ListSongFiles {

    LoadXML loadXML;

    void songUrisInFolder (Context c) {
        // When the app is started, StorageAccess.getSongFolderContents() is called
        // This builds the FullscreenActivity.allSongs and FullscreenActivity.allSongFolders arraylists
        // These are strings which contain the DocumentsContract IDs.
        // We need to filter these and only return the Uris of those that are in the current folder
        // If we create new songs or folders or refresh, we'll need to re-run that method

        // Initialise the song uris
        FullscreenActivity.currentSongsInFoldersUris = new ArrayList<>();

        // Initialise the helper class
        StorageAccess storageAccess = new StorageAccess();
        Uri treeUri = storageAccess.getTreeUri(c);

        // Use whichSongFolder to only list the files that match
        for (String id : FullscreenActivity.allSongs) {
            String bittosearchfor = storageAccess.returnBlankForMain(c, FullscreenActivity.whichSongFolder);
            if (!bittosearchfor.equals("")) {
                bittosearchfor = bittosearchfor + "/";
                bittosearchfor = bittosearchfor.replace("//","/");
            }

            String treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
            String bittosearch = id.replace(treeDocId,"");
            bittosearch = bittosearch.replace(storageAccess.appFolder,"");
            bittosearch = bittosearch.replaceFirst("/Songs", "");
            if (bittosearch.startsWith("/")) {
                bittosearch = bittosearch.replaceFirst("/", "");
            }

            String bitthatisleft = bittosearch.replaceFirst(bittosearchfor, "");
            if (bitthatisleft.startsWith("/")) {
                bitthatisleft = bitthatisleft.replaceFirst("/", "");
            }

            if ((bittosearchfor.equals("") && !bittosearch.contains("/")) ||
                    // We are listing the MAIN folder and there are no more directories
                    // or... we don't have anymore sub directories
                    (!bittosearchfor.equals("") &&
                            bittosearch.contains(bittosearchfor) &&
                            !bitthatisleft.replace(bittosearch,"").contains("/"))) {
                FullscreenActivity.currentSongsInFoldersUris.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, id));
            }
        }
    }

    void getSongDetails(final Context c) {
        // Go through each song in the current folder and extract the title, key and author
        // If not a valid song, just return the file name
        try {
            StorageAccess storageAccess = new StorageAccess();
            FullscreenActivity.songDetails = new String[FullscreenActivity.currentSongsInFoldersUris.size()][3];
            boolean fileextensionok;
            String utf;
            for (int i = 0; i < FullscreenActivity.currentSongsInFoldersUris.size(); i++) {
                Uri uri = FullscreenActivity.currentSongsInFoldersUris.get(i);
                String[] vals = new String[3];
                ArrayList<String> songdetails = storageAccess.getFileDetailsFromUri(c, uri);
                String filename = songdetails.get(0);
                Log.d("d", "filename=" + filename);
                fileextensionok = checkFileExtension(filename);
                utf = storageAccess.getUTFEncodingFromUri(c, uri);
                if (fileextensionok) {
                    vals = getSongDetailsXML(c, uri, filename, utf);
                } else {
                    // Non OpenSong
                    vals[0] = filename;
                    vals[1] = "";
                    vals[2] = "";
                }
                if (vals[2] == null) {
                    vals[2] = "";
                }
                try {
                    FullscreenActivity.songDetails[i][0] = vals[0];
                    FullscreenActivity.songDetails[i][1] = vals[1];
                    FullscreenActivity.songDetails[i][2] = vals[2];
                } catch (Exception e) {
                    // Error trying to get song details
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static Collator coll;
    //private static ArrayList<String> filelist;

    public void getAllSongFolders(Context c, DocumentFile homeFolder) {
        //FullscreenActivity.allfilesforsearch.clear();
        //FullscreenActivity.mSongFolderNames = FullscreenActivity.songfilelist.getFolderList(c, homeFolder);
    }

    /*
        incorporated new class here.
     */
    void getAllSongFiles(Context c, DocumentFile homeFolder) {
        /*try {
            FullscreenActivity.mSongFileNames = FullscreenActivity.songfilelist.getSongFileListasArray(c, homeFolder);
            //int j = 0;
        } catch (Exception e) {
            Log.d(e.getMessage(), "Error caught in getAllSongFiles() in ListSongFiles.java");
            e.printStackTrace();
        }*/
    }

    /*TODO why use a multidimensional array, when you could use an xml object?
    I've been reading about performance and I guess its because of performance
    limitations?  Is maintaining an object in memory expensive
    in terms of performance?  So, the class I created is essentially worse
    than reading directly from the file system?  I don't think so personally,
    as I don't think the garbage collector will be dereference either of the objects
    internal to the songfilelist class, and the songfilelist class persists for the
    lifetime of the app, so there shouldn't be any extra work, and the memory overhead
    is low and speed of access of cached variable is faster than file access, at
    least I guess.
     */
 /*
    void getSongDetails(final Context c, DocumentFile homeFolder) {
        // Go through each song in the current folder and extract the title, key and author
        // If not a valid song, just return the file name
        try {
            FullscreenActivity.songDetails = new String[FullscreenActivity.mSongFileNames.length][3];
            boolean fileextensionok;
            String utf;
            for (int r = 0; r < FullscreenActivity.mSongFileNames.length; r++) {
                String s = FullscreenActivity.mSongFileNames[r];
                String[] vals = new String[3];

                StorageAccess sa = new StorageAccess();
                DocumentFile f = sa.getFileLocationAsDocumentFile(c, homeFolder, "Songs", FullscreenActivity.whichSongFolder, s);
                if (f.exists()) {
                    if (f.isDirectory()) {
                        vals[0] = s;
                        vals[1] = f.getUri().getLastPathSegment();
                        vals[2] = c.getString(R.string.songsinfolder);
                    } else {
                        fileextensionok = checkFileExtension(s);
                        utf = sa.getUTFEncoding(c, homeFolder, "Songs", FullscreenActivity.whichSongFolder, s);
                        if (fileextensionok) {
                            vals = getSongDetailsXML(c, f, s, utf);
                        } else {
                            // Non OpenSong
                            vals[0] = s;
                            vals[1] = "";
                            vals[2] = "";
                        }
                        if (vals[2] == null || vals[2].equals("")) {
                            vals[2] = "";
                        }
                    }
                    try {
                        FullscreenActivity.songDetails[r][0] = vals[0];
                        FullscreenActivity.songDetails[r][1] = vals[1];
                        FullscreenActivity.songDetails[r][2] = vals[2];
                    } catch (Exception e) {
                        // Error trying to get song details
                    }
                }
            }
        } catch (Exception e) {
            // Ooops, error
        }

        FullscreenActivity.numDirs = 0;
        try {
            while (FullscreenActivity.songDetails[FullscreenActivity.numDirs][2] != null &&
                    FullscreenActivity.songDetails[FullscreenActivity.numDirs][2].equals(c.getString(R.string.songsinfolder))) {
                FullscreenActivity.numDirs++;
            }
        } catch (Exception e){
            Log.d("d","Error building a valid index - it's empty");
        }
        if (FullscreenActivity.numDirs > 0) {
            FullscreenActivity.numDirs += 1;
        }
    }
*/
    private String[] getSongDetailsXML(Context c, Uri uri, String filename, String utf) {
        String vals[] = new String[3];
        vals[0] = filename;
        vals[1] = "";
        vals[2] = "";

        StorageAccess storageAccess = new StorageAccess();

        try {
            XmlPullParserFactory factory;
            factory = XmlPullParserFactory.newInstance();

            factory.setNamespaceAware(true);
            XmlPullParser xpp;
            xpp = factory.newPullParser();

            InputStream inputStream = storageAccess.getInputStreamFromUri(c, uri);
            InputStreamReader lineReader = new InputStreamReader(inputStream);
            BufferedReader buffreader = new BufferedReader(lineReader);

            String line;
            try {
                line = buffreader.readLine();
                if (line != null && line.contains("encoding=\"")) {
                    int startpos = line.indexOf("encoding=\"") + 10;
                    int endpos = line.indexOf("\"", startpos);
                    String enc = line.substring(startpos, endpos);
                    if (enc.length() > 0) {
                        utf = enc.toUpperCase();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            inputStream.close();

            inputStream = storageAccess.getInputStreamFromUri(c, uri);
            xpp.setInput(inputStream, utf);
            loadXML = new LoadXML();
            boolean gotAuthor = false;
            boolean gotKey = false;
            int eventType;
            eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("author")) {
                        gotAuthor = true;
                        vals[1] = loadXML.parseFromHTMLEntities(xpp.nextText());
                        if (gotKey) {
                            // No need to keep looking
                            return vals;
                        }

                    } else if (xpp.getName().equals("key")) {
                        gotKey = true;
                        vals[2] = loadXML.parseFromHTMLEntities(xpp.nextText());
                        if (gotAuthor) {
                            // No need to keep looking
                            return vals;
                        }
                    }
                    try {
                        eventType = xpp.next();
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Oops!
                    }

                } else {
                    vals[1] = "";
                    vals[2] = "";
                }
            }
        } catch (Exception e) {
                vals[0] = filename;
                vals[1] = "";
                vals[2] = "";
                e.printStackTrace();
        }
         return vals;
    }

    private boolean checkFileExtension(String s) {
        boolean isxml = true;
        s = s.toLowerCase();
        String type = null;
        if (s.lastIndexOf(".")>1 && s.lastIndexOf(".")<s.length()-1) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            int index = s.lastIndexOf('.')+1;
            String ext = s.substring(index).toLowerCase();
            type = mime.getMimeTypeFromExtension(ext);
        }

        if (type!=null && !type.equals("")) {
            if (type.contains("image") || type.contains("application") || type.contains("video") || type.contains("audio")) {
                return false;
            }
        }

        if (s.endsWith(".pdf") ||
                s.endsWith(".doc") || s.endsWith(".docx") ||
                s.endsWith(".jpg") || s.endsWith(".png") || s.endsWith(".gif") ||
                s.endsWith(".zip") || s.endsWith(".apk") || s.endsWith(".tar")  || s.endsWith(".backup")) {
            isxml = false;
        }
        return isxml;
    }

    boolean errorClearingAllSongs(Context c, DocumentFile homeFolder) {
        // Clear all songs in the songs folder
        StorageAccess sa = new StorageAccess();
        DocumentFile delPath = sa.tryCreateDirectory(c,homeFolder,"Songs","");
        boolean deleted = false;
        if (delPath.exists()) {
            try {
                delPath.delete();
                deleted = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        sa.tryCreateDirectory(c, homeFolder,"Songs","");
        return !deleted;
    }

    void getCurrentSongIndex() {
        // Find the current song index from the song filename
        // Set them all to 0
        FullscreenActivity.currentSongIndex = 0;
        FullscreenActivity.nextSongIndex = 0;
        FullscreenActivity.previousSongIndex = 0;

        // Go through the array
        try {
            if (FullscreenActivity.mSongFileNames != null && FullscreenActivity.songfilename != null) {
                for (int s = 0; s < FullscreenActivity.mSongFileNames.length; s++) {
                    if (FullscreenActivity.mSongFileNames != null &&
                            FullscreenActivity.songfilename != null &&
                            FullscreenActivity.mSongFileNames[s] != null &&
                            FullscreenActivity.mSongFileNames[s].equals(FullscreenActivity.songfilename)) {
                        FullscreenActivity.currentSongIndex = s;
                        if (s > 0) {
                            FullscreenActivity.previousSongIndex = s - 1;
                        } else {
                            FullscreenActivity.previousSongIndex = s;
                        }
                        if (s < FullscreenActivity.mSongFileNames.length - 1) {
                            FullscreenActivity.nextSongIndex = s + 1;
                        } else {
                            FullscreenActivity.nextSongIndex = s;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d(e.getMessage(),"Some error with the song list");
        }
    }

    void deleteSong(Context c, DocumentFile homeFolder) {
        FullscreenActivity.setView = false;

        StorageAccess sa = new StorageAccess();
        DocumentFile filetoremove = sa.getFileLocationAsDocumentFile(c, homeFolder,
                "Songs", FullscreenActivity.whichSongFolder,
                FullscreenActivity.songfilename);
        if (filetoremove.delete()) {
            FullscreenActivity.myToastMessage = "\"" + FullscreenActivity.songfilename + "\" "
                    + c.getString(R.string.songhasbeendeleted);
            // If we are autologging CCLI information
            if (FullscreenActivity.ccli_automatic) {
                PopUpCCLIFragment popUpCCLIFragment = new PopUpCCLIFragment();
                popUpCCLIFragment.addUsageEntryToLog(c,FullscreenActivity.whichSongFolder+"/"+FullscreenActivity.songfilename,
                        "", "",
                        "", "", "2"); // Deleted
            }

        } else {
            FullscreenActivity.myToastMessage = c.getString(R.string.deleteerror_start)
                    + " \"" + FullscreenActivity.songfilename + "\" "
                    + c.getString(R.string.deleteerror_end_song);
        }
    }

    boolean blacklistFileType(String s) {
        s = s.toLowerCase();
        String type = null;
        if (s.lastIndexOf(".") > 1 && s.lastIndexOf(".") < s.length() - 1) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            int index = s.lastIndexOf('.') + 1;
            String ext = s.substring(index).toLowerCase();
            type = mime.getMimeTypeFromExtension(ext);
        }

        return type != null && !type.equals("") && !type.contains("pdf") && (type.contains("audio") || type.contains("application") || type.contains("video"));

    }
}