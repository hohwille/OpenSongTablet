package com.garethevans.church.opensongtablet;

import android.app.Activity;
import android.content.Context;
import android.support.v4.provider.DocumentFile;
import android.util.Base64;
import android.util.Log;

import java.io.OutputStream;
import java.util.ArrayList;

public class CreateNewSet extends Activity {

    boolean doCreation(Context c,DocumentFile homeFolder) {

        LoadXML loadXML = new LoadXML();

        Log.d( "CreateNewSet","settoload="+FullscreenActivity.settoload);
        Log.d( "CreateNewSet","homeFolder="+homeFolder.getUri());
        Log.d("CreateNewSet", "mSetList.length="+FullscreenActivity.mSetList.length);
        for (String s:FullscreenActivity.mSetList) {
            Log.d("CreateNewSet", "s="+s);
        }

        // Keep the current song and directory aside for now
        String tempsongfilename = FullscreenActivity.songfilename;
        String tempdir = FullscreenActivity.whichSongFolder;
        StringBuilder setbuilder = new StringBuilder();
        setbuilder.append("");

        // Only do this is the mSetList isn't empty
        if (FullscreenActivity.mSetList!=null && FullscreenActivity.mSetList.length>0) {
            setbuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<set name=\"");
            setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(FullscreenActivity.settoload));
            setbuilder.append("\">\n<slide_groups>\n");

            for (int x = 0; x < FullscreenActivity.mSetList.length; x++) {
                // Check if song is in subfolder
                if (!FullscreenActivity.mSetList[x].contains("/")) {
                    FullscreenActivity.mSetList[x] = "/" + FullscreenActivity.mSetList[x];
                }

                // Split the string into two
                String[] songparts;
                songparts = FullscreenActivity.mSetList[x].split("/");

                if (songparts.length<1) {
                    return false;
                }

                // If the path isn't empty, add a forward slash to the end
                if (songparts[0].length() > 0) {
                    songparts[0] = songparts[0] + "/";
                }
                if (!songparts[0].contains("**"+c.getResources().getString(R.string.image)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.variation)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.scripture)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.slide)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.note))) {
                    // Adding a song
                    // If we are not in a subfolder, we have two parts, otherwise, we have subfolders
                    String s_name;
                    StringBuilder f_name = new StringBuilder();
                    for (int pieces=0;pieces<(songparts.length - 1); pieces++) {
                        f_name.append("/").append(songparts[pieces]);
                    }
                    f_name = new StringBuilder(f_name.toString().replace("///", "/"));
                    f_name = new StringBuilder(f_name.toString().replace("//", "/"));
                    try {
                        s_name = songparts[songparts.length-1];
                    } catch (Exception e) {
                        e.printStackTrace();
                        s_name = songparts[1];
                    }

                    setbuilder.append("  <slide_group name=\"").append(PopUpEditSongFragment.parseToHTMLEntities(s_name));
                    setbuilder.append("\" type=\"song\" presentation=\"\" path=\"");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(f_name.toString())).append("\"/>\n");


                } else if (songparts[0].contains("**"+c.getResources().getString(R.string.scripture)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.variation)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.image)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.slide)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.note))) {
                    // Adding a scripture
                    // Load the scripture file up
                    FullscreenActivity.whichSongFolder = "../Scripture/_cache";
                    FullscreenActivity.songfilename = songparts[1];
                    try {
                        loadXML.loadXML(c,homeFolder);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    FullscreenActivity.myLyrics = FullscreenActivity.mLyrics;

                    String scripture_lyrics = FullscreenActivity.mLyrics;

                    // Parse the lyrics into individual slides;
                    scripture_lyrics = scripture_lyrics.replace("[]", "_SPLITHERE_");

                    String[] mySlides = scripture_lyrics.split("_SPLITHERE_");

                    String newname = songparts[1];
                    if (FullscreenActivity.mAuthor!="") {
                        newname = newname+"|"+FullscreenActivity.mAuthor;
                    }
                    setbuilder.append("  <slide_group type=\"scripture\" name=\"");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(newname));
                    setbuilder.append("\" print=\"true\">\n");
                    setbuilder.append("    <title>");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(songparts[1]));
                    setbuilder.append("</title>\n");
                    setbuilder.append("    <slides>\n");

                    for (String mySlide : mySlides) {
                        if (mySlide != null && mySlide.length() > 0) {
                            setbuilder.append("      <slide>\n");
                            setbuilder.append("      <body>");
                            setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(mySlide.trim()));
                            setbuilder.append("</body>\n");
                            setbuilder.append("      </slide>\n");
                        }
                    }
                    setbuilder.append("    </slides>\n");
                    setbuilder.append("    <subtitle>" + "</subtitle>\n");
                    setbuilder.append("    <notes />\n");
                    setbuilder.append("  </slide_group>\n");

                } else if (songparts[0].contains("**"+c.getResources().getString(R.string.slide)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.variation)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.image)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.note)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.scripture))) {
                    // Adding a custom slide
                    // Load the slide file up
                    // Keep the songfile as a temp
                    FullscreenActivity.whichSongFolder = "../Slides/_cache";
                    FullscreenActivity.songfilename = songparts[1];
                    try {
                        loadXML.loadXML(c,homeFolder);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    FullscreenActivity.myLyrics = FullscreenActivity.mLyrics;

                    String slide_lyrics = FullscreenActivity.mLyrics;

                    if (slide_lyrics.indexOf("---\n") == 0) {
                        slide_lyrics = slide_lyrics.replaceFirst("---\n", "");
                    }
                    // Parse the lyrics into individual slides;
                    slide_lyrics = slide_lyrics.replace("---", "_SPLITHERE_");

                    String[] mySlides = slide_lyrics.split("_SPLITHERE_");

                    setbuilder.append("  <slide_group name=\"");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(songparts[1]));
                    setbuilder.append("\" type=\"custom\" print=\"true\"");
                    setbuilder.append(" seconds=\"");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(FullscreenActivity.mUser1));
                    setbuilder.append("\"");
                    setbuilder.append(" loop=\"");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(FullscreenActivity.mUser2));
                    setbuilder.append("\"");
                    setbuilder.append(" transition=\"");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(FullscreenActivity.mUser3));
                    setbuilder.append("\">\n");
                    setbuilder.append("    <title>");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(FullscreenActivity.mTitle.toString()));
                    setbuilder.append("</title>\n");
                    setbuilder.append("    <subtitle>");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(FullscreenActivity.mCopyright.toString()));
                    setbuilder.append("</subtitle>\n");
                    setbuilder.append("    <notes>");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(FullscreenActivity.mKeyLine));
                    setbuilder.append("</notes>\n");
                    setbuilder.append("    <slides>\n");

                    for (String mySlide : mySlides) {
                        if (mySlide != null && mySlide.length() > 0) {
                            setbuilder.append("      <slide>\n");
                            setbuilder.append("        <body>");
                            setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(mySlide.trim()));
                            setbuilder.append("</body>\n");
                            setbuilder.append("      </slide>\n");
                        }
                    }

                    setbuilder.append("    </slides>\n");
                    setbuilder.append("  </slide_group>\n");


                } else if (songparts[0].contains("**"+c.getResources().getString(R.string.note)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.variation)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.image)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.slide)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.scripture))) {
                    // Adding a custom note

                    // Load the note up to grab the contents
                    FullscreenActivity.whichSongFolder = "../Notes/_cache";
                    FullscreenActivity.songfilename = songparts[1];
                    try {
                        loadXML.loadXML(c,homeFolder);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    FullscreenActivity.myLyrics = FullscreenActivity.mLyrics;

                    String slide_lyrics = FullscreenActivity.mLyrics;

                    setbuilder.append("  <slide_group name=\"# ");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(c.getResources().getString(R.string.note)));
                    setbuilder.append(" # - ");
                    setbuilder.append(songparts[1]);
                    setbuilder.append("\"");
                    setbuilder.append(" type=\"custom\" print=\"true\" seconds=\"\" loop=\"\" transition=\"\">\n");
                    setbuilder.append("    <title></title>\n");
                    setbuilder.append("    <subtitle></subtitle>\n");
                    setbuilder.append("    <notes>");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(slide_lyrics));
                    setbuilder.append("</notes>\n");
                    setbuilder.append("    <slides></slides>\n");
                    setbuilder.append("  </slide_group>\n");

                } else if (songparts[0].contains("**"+c.getResources().getString(R.string.variation)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.note)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.image)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.slide)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.scripture))) {
                    // Adding a variation
                    // The entire song is copied to the notes, and a simplified version is copied to the text

                    // Load the variation song up to grab the contents
                    FullscreenActivity.whichSongFolder = "../Variations";
                    FullscreenActivity.songfilename = songparts[1];
                    try {
                        loadXML.loadXML(c,homeFolder);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    FullscreenActivity.myLyrics = FullscreenActivity.mLyrics;
                    String slide_lyrics = FullscreenActivity.mLyrics;
                    try {
                        byte[] data = FullscreenActivity.myXML.getBytes("UTF-8");
                        slide_lyrics = Base64.encodeToString(data, Base64.DEFAULT);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Prepare the slide contents so it remains compatible with the desktop app
                    // Split the lyrics into individual lines
                    String[] lyrics_lines = FullscreenActivity.myLyrics.split("\n");
                    StringBuilder currentslide = new StringBuilder();
                    ArrayList<String> newslides = new ArrayList<>();

                    for (String thisline:lyrics_lines) {
                        if (!thisline.equals("") && !thisline.startsWith(".") && !thisline.startsWith("[") && !thisline.startsWith(";")) {
                            // Add the current line into the new slide
                            // Replace any new line codes | with \n
                            thisline = thisline.replace("||","\n");
                            thisline = thisline.replace("---","\n");
                            thisline = thisline.replace("|","\n");
                            currentslide.append(thisline.trim()).append("\n");
                        } else if (thisline.startsWith("[")) {
                            // Save the current slide and create a new one
                            currentslide = new StringBuilder(currentslide.toString().trim());
                            newslides.add(currentslide.toString());
                            currentslide = new StringBuilder();
                        }
                    }
                    newslides.add(currentslide.toString());
                    // Now go back through the currentslides and write the slide text
                    StringBuilder slidetexttowrite = new StringBuilder();
                    for (int z=0; z<newslides.size();z++) {
                        if (!newslides.get(z).equals("")) {
                            slidetexttowrite.append("      <slide>\n");
                            slidetexttowrite.append("        <body>");
                            slidetexttowrite.append(PopUpEditSongFragment.parseToHTMLEntities(newslides.get(z).trim()));
                            slidetexttowrite.append("\n");
                            slidetexttowrite.append("        </body>\n");
                            slidetexttowrite.append("      </slide>\n");
                        }
                    }

                    setbuilder.append("  <slide_group name=\"# ");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(c.getResources().getString(R.string.variation)));
                    setbuilder.append(" # - ");
                    setbuilder.append(songparts[1]);
                    setbuilder.append("\"");
                    setbuilder.append(" type=\"custom\" print=\"true\" seconds=\"\" loop=\"\" transition=\"\">\n");
                    setbuilder.append("    <title>");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(songparts[1]));
                    setbuilder.append("</title>\n");
                    setbuilder.append("    <subtitle>");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(FullscreenActivity.mAuthor.toString()));
                    setbuilder.append("</subtitle>\n");
                    setbuilder.append("    <notes>");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(slide_lyrics));
                    setbuilder.append("</notes>\n");
                    setbuilder.append("    <slides>\n");
                    setbuilder.append(slidetexttowrite);
                    setbuilder.append("    </slides>\n");
                    setbuilder.append("  </slide_group>\n");


                } else if (songparts[0].contains("**"+c.getResources().getString(R.string.image)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.variation)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.note)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.slide)) &&
                        !songparts[0].contains("**"+c.getResources().getString(R.string.scripture))) {
                    // Adding a custom image slide

                    // Load the image slide up to grab the contents
                    FullscreenActivity.whichSongFolder = "../Images/_cache";
                    FullscreenActivity.songfilename = songparts[1];
                    try {
                        loadXML.loadXML(c,homeFolder);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // The hymn_number field should contain all the images
                    // Each image is separated by \n$$$\n
                    String imagecode[] = FullscreenActivity.mHymnNumber.split("XX_IMAGE_XX");

                    // Break all the images into the relevant slides
                    String[] separate_slide = FullscreenActivity.mUser3.split("\n");

                    // Get the number of image codes
                    int sepslidesnum = separate_slide.length;


                    FullscreenActivity.myLyrics = FullscreenActivity.mLyrics;
                    StringBuilder slide_code = new StringBuilder();


                    for (int e=0;e<sepslidesnum;e++) {
                        String imglinetext;
                        if (e<imagecode.length && imagecode[e]!=null && !imagecode[e].equals("")) {
                            imglinetext = "        <image>" + imagecode[e].trim() + "</image>\n";
                        } else {
                            imglinetext = "        <filename>" + separate_slide[e] + "</filename>\n";
                        }
                        slide_code.append("      <slide>\n").append(imglinetext).append("        <description>").append(separate_slide[e]).append("</description>\n").append("      </slide>\n");
                    }

                    setbuilder.append("  <slide_group name=\"");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(FullscreenActivity.mAka));
                    setbuilder.append("\" type=\"image\" print=\"true\" seconds=\"");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(FullscreenActivity.mUser1));
                    setbuilder.append("\" loop=\"");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(FullscreenActivity.mUser2));
                    setbuilder.append("\" transition=\"0\"");
                    setbuilder.append(" resize=\"screen\" keep_aspect=\"false\" link=\"false\">\n");
                    setbuilder.append("    <title>");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(FullscreenActivity.mTitle.toString()));
                    setbuilder.append("</title>\n");
                    setbuilder.append("    <subtitle>");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(FullscreenActivity.mAuthor.toString()));
                    setbuilder.append("</subtitle>\n");
                    setbuilder.append("    <notes>");
                    setbuilder.append(PopUpEditSongFragment.parseToHTMLEntities(FullscreenActivity.mKeyLine));
                    setbuilder.append("</notes>\n");
                    setbuilder.append("    <slides>\n");
                    setbuilder.append(slide_code);
                    setbuilder.append("\n");
                    setbuilder.append("    </slides>\n");
                    setbuilder.append("  </slide_group>\n");
                }
            }

            setbuilder.append("</slide_groups>\n</set>");

            FullscreenActivity.newSetContents = setbuilder.toString();

            // Write the string to the file
            StorageAccess storageAccess = new StorageAccess();
            OutputStream os = storageAccess.getOutputStream(c, homeFolder,"Sets", "", FullscreenActivity.settoload);
            storageAccess.writeStringToFile(c,os,setbuilder.toString());

            // Update the last loaded set now it is saved.
            FullscreenActivity.lastLoadedSetContent = FullscreenActivity.mySet;
            FullscreenActivity.lastSetName = FullscreenActivity.settoload;

            // Now we are finished, put the original songfilename back
            FullscreenActivity.songfilename = tempsongfilename;
            FullscreenActivity.whichSongFolder = tempdir;
            try {
                loadXML.loadXML(c, homeFolder);
            } catch (Exception e) {
                e.printStackTrace();
            }

            FullscreenActivity.myLyrics = FullscreenActivity.mLyrics;
        }

        FullscreenActivity.newSetContents = setbuilder.toString();

        // Load the set again - to make sure everything is parsed
        SetActions setActions = new SetActions();
        setActions.prepareSetList();

        Preferences.savePreferences();
        return true;
    }
}
