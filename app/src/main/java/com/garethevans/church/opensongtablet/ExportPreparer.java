package com.garethevans.church.opensongtablet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class ExportPreparer {

    private String song_title;
    private String song_author;
    private String song_hymnnumber;
    private String song_key;
    private String folderstoexport = "";
    private ArrayList<String> filesinset = new ArrayList<>(), filesinset_ost = new ArrayList<>();
    private Backup_Create_Selected backup_create_selected;
    private ZipOutputStream outSelected;

    Intent exportSet(Context c, Preferences preferences, StorageAccess storageAccess) {
        String nicename = FullscreenActivity.settoload;

        // This is the actual set file
        Uri seturi = storageAccess.getFileProviderUri(c, preferences, "Sets", "", FullscreenActivity.settoload);

        // These get set later if the user wants them
        Uri texturi = null;
        Uri desktopuri = null;
        Uri ostsuri = null;

        // Read in the set
        setParser(c, preferences, storageAccess);

        // Make the set name nicer (add the category in brackets)
        if (FullscreenActivity.settoload.contains("__")) {
            String[] bits = FullscreenActivity.settoload.split("__");
            String category = "";
            String name = FullscreenActivity.settoload;
            if (bits[0]!=null && !bits[0].equals("")) {
                category = " (" + bits[0] + ")";
            }
            if (bits[1]!=null && !bits[1].equals("")) {
                name = bits[1];
            }
            nicename = name + category;
        }

        // Prepare the email intent
        Intent emailIntent = setEmailIntent(nicename,nicename,nicename + "\n\n" + FullscreenActivity.emailtext);

        // If the user has requested to attach a .txt version of the set
        if (FullscreenActivity.exportText) {
            texturi = storageAccess.getFileProviderUri(c, preferences, "Export", "",
                    FullscreenActivity.settoload+".txt");

            // Check the uri exists for the outputstream to be valid
            storageAccess.lollipopCreateFileForOutputStream(c, preferences, texturi, null, "Export", "", FullscreenActivity.settoload + ".txt");

            // Write the set text file
            OutputStream outputStream = storageAccess.getOutputStream(c,texturi);
            storageAccess.writeFileFromString(FullscreenActivity.emailtext,outputStream);
        }

        // Reset the text version of the set
        FullscreenActivity.emailtext = "";

        // If the user wants to attach the normal set (desktop file) without and xml extenstion, set the uri
        if (FullscreenActivity.exportDesktop) {
            desktopuri = seturi;
        }

        // If the user wants to add the OpenSongApp version of the set (same as desktop with .osts extension)
        if (FullscreenActivity.exportOpenSongAppSet) {
            // Copy the set file to an .osts file
            InputStream inputStream = storageAccess.getInputStream(c,seturi);

            ostsuri = storageAccess.getFileProviderUri(c, preferences, "Export", "", FullscreenActivity.settoload + ".osts");

            // Check the uri exists for the outputstream to be valid
            storageAccess.lollipopCreateFileForOutputStream(c, preferences, ostsuri, null, "Export", "", FullscreenActivity.settoload + ".osts");

            OutputStream outputStream = storageAccess.getOutputStream(c,ostsuri);
            storageAccess.copyFile(inputStream,outputStream);
        }

        // Add the uris for each file requested
        ArrayList<Uri> uris = new ArrayList<>();
        if (texturi!=null) {
            uris.add(texturi);
        }
        if (ostsuri!=null) {
            uris.add(ostsuri);
        }
        if (desktopuri!=null) {
            uris.add(desktopuri);
        }

        Log.d("ExportPreparer", "exportOpenSongAppSet=" + FullscreenActivity.exportOpenSongAppSet + "  ostsuri=" + ostsuri);

        // Go through each song in the set and attach them (assuming they exist!)
        // Also try to attach a copy of the song ending in .ost, as long as they aren't images if the user requested that
        if (FullscreenActivity.exportOpenSongApp) {
            for (int q = 0; q < FullscreenActivity.exportsetfilenames.size(); q++) {
                // Remove any subfolder from the exportsetfilenames_ost.get(q)
                String tempsong_ost = FullscreenActivity.exportsetfilenames_ost.get(q);
                tempsong_ost = tempsong_ost.substring(tempsong_ost.lastIndexOf("/") + 1);
                Log.d("ExportPreparer", "tempsong_ost=" + tempsong_ost);
                Uri songtoload = storageAccess.getFileProviderUri(c, preferences, "Songs", "",
                        FullscreenActivity.exportsetfilenames.get(q));
                Log.d("ExportPreparer", "songtoload=" + songtoload);

                boolean isimage = false;
                String s = songtoload.toString();
                try {
                    s = songtoload.getLastPathSegment();
                } catch (Exception e) {
                    e.printStackTrace();
                }


                if (s != null && (s.endsWith(".jpg") || s.endsWith(".JPG") || s.endsWith(".jpeg") || s.endsWith(".JPEG") ||
                        s.endsWith(".gif") || s.endsWith(".GIF") || s.endsWith(".png") || s.endsWith(".PNG") ||
                        s.endsWith(".bmp") || s.endsWith(".BMP"))) {
                    isimage = true;
                }

                // Copy the song
                if (!storageAccess.lollipopOrLater() || storageAccess.uriExists(c, songtoload) && !isimage) {
                    InputStream inputStream = storageAccess.getInputStream(c,songtoload);
                    Log.d("ExportPreparer", "inputStream=" + inputStream);
                    if (inputStream != null) {
                        Uri ostsongcopy = storageAccess.getFileProviderUri(c, preferences, "Notes", "_cache",
                                tempsong_ost + ".ost");
                        Log.d("ExportPreparer", "ostsongcopy=" + ostsongcopy);
                        // Check the uri exists for the outputstream to be valid
                        storageAccess.lollipopCreateFileForOutputStream(c, preferences, ostsongcopy, null, "Notes", "_cache", tempsong_ost + ".ost");
                        Log.d("ExportPreparer", "filecreated");
                        OutputStream outputStream = storageAccess.getOutputStream(c, ostsongcopy);
                        Log.d("ExportPreparer", "outputStream=" + outputStream);
                        storageAccess.copyFile(inputStream, outputStream);
                        Log.d("ExportPreparer", "file copied=");
                        uris.add(ostsongcopy);
                    }
                }
            }
        }

        // Add the standard song file (desktop version) - if it exists
        if (FullscreenActivity.exportDesktop) {
            for (int q = 0; q < FullscreenActivity.exportsetfilenames.size(); q++) {
                Uri uri = storageAccess.getFileProviderUri(c, preferences, "Songs", "", FullscreenActivity.exportsetfilenames.get(q));
                if (storageAccess.uriExists(c, uri)) {
                    uris.add(uri);
                }
            }
        }

        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return emailIntent;
    }

    Intent exportSong(Context c, Preferences preferences, Bitmap bmp, StorageAccess storageAccess) {
        // Prepare the appropriate attachments
        String emailcontent = "";
        Uri text = null;
        Uri ost = null;
        Uri desktop = null;
        Uri chopro = null;
        Uri onsong = null;
        Uri image = null;
        Uri pdf = null;
        Bitmap pdfbmp = bmp.copy(bmp.getConfig(),true);

        // Prepare the song uri and input stream
        Uri uriinput = storageAccess.getUriForItem(c, preferences, "Songs", FullscreenActivity.whichSongFolder,
                FullscreenActivity.songfilename);
        InputStream inputStream;

        // Prepare a txt version of the song.
        String exportText_String = prepareTextFile(c);

        emailcontent += exportText_String;
        if (FullscreenActivity.exportText) {
            text = storageAccess.getUriForItem(c, preferences, "Export", "",
                    FullscreenActivity.songfilename+".txt");

            // Check the uri exists for the outputstream to be valid
            storageAccess.lollipopCreateFileForOutputStream(c, preferences, text, null, "Export", "", FullscreenActivity.songfilename + ".txt");

            OutputStream outputStream = storageAccess.getOutputStream(c,text);
            storageAccess.writeFileFromString(exportText_String,outputStream);
        }

        if (FullscreenActivity.exportOpenSongApp) {
            // Prepare an ost version of the song.
            Log.d("d", "uriinput=" + uriinput);
            ost = storageAccess.getUriForItem(c, preferences, "Export", "",
                    FullscreenActivity.songfilename+".ost");

            // Check the uri exists for the outputstream to be valid
            storageAccess.lollipopCreateFileForOutputStream(c, preferences, ost, null, "Export", "", FullscreenActivity.songfilename + ".ost");

            inputStream = storageAccess.getInputStream(c, uriinput);
            Log.d("d", "inputstream=" + inputStream);
            OutputStream outputStream = storageAccess.getOutputStream(c,ost);
            Log.d("d", "outputstream=" + outputStream);

            storageAccess.copyFile(inputStream,outputStream);
        }

        if (FullscreenActivity.exportDesktop) {
            // Prepare a desktop version of the song.
            desktop = storageAccess.getUriForItem(c, preferences, "Export", "",
                    FullscreenActivity.songfilename);

            // Check the uri exists for the outputstream to be valid
            storageAccess.lollipopCreateFileForOutputStream(c, preferences, desktop, null, "Export", "", FullscreenActivity.songfilename);

            inputStream = storageAccess.getInputStream(c, uriinput);
            OutputStream outputStream = storageAccess.getOutputStream(c,desktop);
            storageAccess.copyFile(inputStream,outputStream);
        }

        if (FullscreenActivity.exportChordPro) {
            // Prepare a chordpro version of the song.
            String exportChordPro_String = prepareChordProFile(c);
            chopro = storageAccess.getUriForItem(c, preferences, "Export", "",
                    FullscreenActivity.songfilename+".chopro");

            // Check the uri exists for the outputstream to be valid
            storageAccess.lollipopCreateFileForOutputStream(c, preferences, chopro, null, "Export", "", FullscreenActivity.songfilename + ".chopro");

            OutputStream outputStream = storageAccess.getOutputStream(c,chopro);
            storageAccess.writeFileFromString(exportChordPro_String,outputStream);
        }

        if (FullscreenActivity.exportOnSong) {
            // Prepare an onsong version of the song.
            String exportOnSong_String = prepareOnSongFile(c);
            onsong = storageAccess.getUriForItem(c, preferences, "Export", "",
                    FullscreenActivity.songfilename+".onsong");

            // Check the uri exists for the outputstream to be valid
            storageAccess.lollipopCreateFileForOutputStream(c, preferences, onsong, null, "Export", "", FullscreenActivity.songfilename + ".onsong");

            OutputStream outputStream = storageAccess.getOutputStream(c,onsong);
            storageAccess.writeFileFromString(exportOnSong_String,outputStream);
        }

        if (FullscreenActivity.exportImage) {
            // Prepare an image/png version of the song.
            image = storageAccess.getUriForItem(c, preferences, "Export", "",
                    FullscreenActivity.songfilename+".png");

            // Check the uri exists for the outputstream to be valid
            storageAccess.lollipopCreateFileForOutputStream(c, preferences, image, null, "Export", "", FullscreenActivity.songfilename + ".png");

            OutputStream outputStream = storageAccess.getOutputStream(c,image);
            storageAccess.writeImage(outputStream, bmp);
        }

        if (FullscreenActivity.exportPDF) {
            // Prepare a pdf version of the song.
            pdf = storageAccess.getUriForItem(c, preferences, "Export", "",
                    FullscreenActivity.songfilename+".pdf");
            storageAccess.lollipopCreateFileForOutputStream(c, preferences, pdf, null, "Export", "", FullscreenActivity.songfilename + ".pdf");
            makePDF(c,pdfbmp,pdf,storageAccess);
        }

        Intent emailIntent = setEmailIntent(FullscreenActivity.songfilename, FullscreenActivity.songfilename,
                emailcontent);

        FullscreenActivity.emailtext = "";

        // Add the attachments to the intent and make them readable uris
        ArrayList<Uri> uris = addUris(ost,text,desktop,chopro,onsong,image,pdf);
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return emailIntent;
    }

    private Intent exportBackup(Context c, Uri uri) {
        Intent emailIntent = setEmailIntent(c.getString(R.string.backup_info),c.getString(R.string.backup_info),
                c.getString(R.string.backup_info));
        FullscreenActivity.emailtext = "";
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(uri);
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return emailIntent;
    }

    Intent exportActivityLog(Context c, Preferences preferences, StorageAccess storageAccess) {
        String title = c.getString(R.string.app_name) + ": " + c.getString(R.string.edit_song_ccli);
        String subject = title + " - " + c.getString(R.string.ccli_view);
        String text = c.getString(R.string.ccli_church) + ": " + FullscreenActivity.ccli_church + "\n";
        text += c.getString(R.string.ccli_licence) + ": " + FullscreenActivity.ccli_licence + "\n\n";
        Intent emailIntent = setEmailIntent(subject,title,text);

        // Add the attachments
        Uri uri = storageAccess.getUriForItem(c, preferences, "Settings", "", "ActivityLog.xml");
        ArrayList<Uri> uris = new ArrayList<>();
        if (!storageAccess.uriExists(c,uri)) {
            PopUpCCLIFragment.createBlankXML(c, preferences);
        }
        // Add the uri
        uris.add(uri);
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return emailIntent;
    }

    private Intent setEmailIntent(String subject, String title, String content) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TITLE, title);
        emailIntent.putExtra(Intent.EXTRA_TEXT, content);
        return emailIntent;
    }

    private ArrayList<Uri> addUris(Uri ost, Uri text, Uri desktop, Uri chopro, Uri onsong, Uri image, Uri pdf) {
        ArrayList<Uri> uris = new ArrayList<>();
        if (ost != null) {
            uris.add(ost);
        }
        if (text != null) {
            uris.add(text);
        }
        if (desktop != null) {
            uris.add(desktop);
        }
        if (chopro != null) {
            uris.add(chopro);
        }
        if (onsong != null) {
            uris.add(onsong);
        }
        if (image != null) {
            uris.add(image);
        }
        if (pdf != null) {
            uris.add(pdf);
        }
        return uris;
    }

    private void setParser(Context c, Preferences preferences, StorageAccess storageAccess) {
        StringBuilder sb = new StringBuilder();

        FullscreenActivity.exportsetfilenames.clear();
        FullscreenActivity.exportsetfilenames_ost.clear();
        filesinset.clear();
        filesinset_ost.clear();

		// First up, load the set

        Uri seturi = storageAccess.getUriForItem(c, preferences, "Sets", "", FullscreenActivity.settoload);
        InputStream inputStream = storageAccess.getInputStream(c,seturi);
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(inputStream, "UTF-8");
            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("slide_group")) {
                        switch (xpp.getAttributeValue(null, "type")) {
                            case "song":
                                Uri songuri = storageAccess.getUriForItem(c, preferences, "Songs",
                                        LoadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "path")),
                                        LoadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "name")));
                                String thisline;

                                // Ensure there is a folder '/'
                                if (xpp.getAttributeValue(null, "path").equals("")) {
                                    thisline = "/" + LoadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "name"));
                                } else {
                                    thisline = LoadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "path")) + LoadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "name"));
                                }
                                filesinset.add(thisline);
                                filesinset_ost.add(thisline);

                                // Set the default values exported with the text for the set
                                song_title = LoadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "name"));
                                song_author = "";
                                song_hymnnumber = "";
                                song_key = "";
                                // Now try to improve on this info
                                if (storageAccess.uriExists(c,songuri)) {
                                    // Read in the song title, author, copyright, hymnnumber, key
                                    getSongData(c,songuri,storageAccess);
                                }
                                sb.append(song_title);
                                if (!song_author.isEmpty()) {
                                    sb.append(", ").append(song_author);
                                }
                                if (!song_hymnnumber.isEmpty()) {
                                    sb.append(", #").append(song_hymnnumber);
                                }
                                if (!song_key.isEmpty()) {
                                    sb.append(" (").append(song_key).append(")");
                                }
                                sb.append("\n");
                                break;
                            case "scripture":
                                sb.append(LoadXML.parseFromHTMLEntities(xpp.getAttributeValue(null,"name"))).append("\n");
                                break;
                            case "custom":
                                // Decide if this is a note or a slide
                                if (xpp.getAttributeValue(null, "name").contains("# " + c.getResources().getString(R.string.note) + " # - ")) {
                                    String nametemp = LoadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "name"));
                                    nametemp = nametemp.replace("# " + c.getResources().getString(R.string.note) + " # - ", "");
                                    sb.append(nametemp).append("\n");
                                } else {
                                    sb.append(LoadXML.parseFromHTMLEntities(xpp.getAttributeValue(null,"name"))).append("\n");
                                }
                                break;
                            case "image":
                                // Go through the descriptions of each image and extract the absolute file locations
                                boolean allimagesdone = false;
                                ArrayList<String> theseimages = new ArrayList<>();
                                String imgname;
                                imgname = LoadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "name"));
                                while (!allimagesdone) { // Keep iterating unless the current eventType is the end of the document
                                    if (eventType == XmlPullParser.START_TAG) {
                                        if (xpp.getName().equals("description")) {
                                            xpp.next();
                                            theseimages.add(LoadXML.parseFromHTMLEntities(xpp.getText()));
                                            filesinset.add(LoadXML.parseFromHTMLEntities(xpp.getText()));
                                            filesinset_ost.add(LoadXML.parseFromHTMLEntities(xpp.getText()));
                                        }

                                    } else if (eventType == XmlPullParser.END_TAG) {
                                        if (xpp.getName().equals("slide_group")) {
                                            allimagesdone = true;
                                        }
                                    }

                                    eventType = xpp.next(); // Set the current event type from the return value of next()
                                }
                                // Go through each of these images and add a line for each one
                                sb.append(imgname).append("\n");
                                for (int im = 0; im < theseimages.size(); im++) {
                                    sb.append("     - ").append(theseimages.get(im)).append("\n");
                                }
                                break;
                        }
                    }
                }
                try {
                    eventType = xpp.next();
                } catch (Exception e) {
                    Log.d("ExportPreparer", "Error moving to the next tag");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Send the settext back to the FullscreenActivity as emailtext
        FullscreenActivity.emailtext = sb.toString();
        FullscreenActivity.exportsetfilenames = filesinset;
        FullscreenActivity.exportsetfilenames_ost = filesinset_ost;
	}

	private void getSongData(Context c, Uri uri, StorageAccess storageAccess) {
		// Parse the song xml.
		// Grab the title, author, lyrics_withchords, lyrics_withoutchords, copyright, hymnnumber, key
		// Initialise all the xml tags a song should have that we want
		song_title = "";
		song_author = "";
        song_hymnnumber = "";
		song_key = "";


		// Get inputtream for the song
        InputStream inputStream = storageAccess.getInputStream(c,uri);

        try {
            XmlPullParserFactory factorySong = XmlPullParserFactory.newInstance();
            factorySong.setNamespaceAware(true);
            XmlPullParser xppSong = factorySong.newPullParser();
            xppSong.setInput(inputStream, "UTF-8");
            int eventType = xppSong.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    switch (xppSong.getName()) {
                        case "author":
                            song_author = LoadXML.parseFromHTMLEntities(xppSong.nextText());
                            break;
                        case "title":
                            song_title = LoadXML.parseFromHTMLEntities(xppSong.nextText());
                            break;
                        /*case "lyrics":
                            song_lyrics_chords = LoadXML.parseFromHTMLEntities(xppSong.nextText());
                            break;*/
                        case "hymn_number":
                            song_hymnnumber = LoadXML.parseFromHTMLEntities(xppSong.nextText());
                            break;
                        case "key":
                            song_key = LoadXML.parseFromHTMLEntities(xppSong.nextText());
                            break;
                    }
                }
                eventType = xppSong.next();
            }
            /*// Remove the chord lines from the song lyrics
            String[] templyrics = song_lyrics_chords.split("\n");

            // Only add the lines that don't start with a .
            int numlines = templyrics.length;
            if (numlines>0) {
                StringBuilder song_lyrics_withoutchords = new StringBuilder();
                for (String templyric : templyrics) {
                    if (!templyric.startsWith(".")) {
                        song_lyrics_withoutchords.append(templyric).append("\n");
                    }
                }
                song_lyrics = song_lyrics_withoutchords.toString();
            }*/

        } catch (Exception e) {
            e.printStackTrace();
        }
	}

    private void makePDF(Context c, Bitmap bmp, Uri uri, StorageAccess storageAccess) {
        Document document = new Document();
        OutputStream outputStream = storageAccess.getOutputStream(c,uri);
        try {
            PdfWriter.getInstance(document, outputStream);
            document.addAuthor(FullscreenActivity.mAuthor.toString());
            document.addTitle(FullscreenActivity.mTitle.toString());
            document.addCreator("OpenSongApp");
            if (bmp!=null && bmp.getWidth()>bmp.getHeight()) {
                document.setPageSize(PageSize.A4.rotate());
            } else {
                document.setPageSize(PageSize.A4);
            }
            document.addTitle(FullscreenActivity.mTitle.toString());
            document.open();//document.add(new Header("Song title",FullscreenActivity.mTitle.toString()));
            BaseFont urName = BaseFont.createFont("assets/fonts/Lato-Reg.ttf", "UTF-8",BaseFont.EMBEDDED);
            Font TitleFontName  = new Font(urName, 14);
            Font AuthorFontName = new Font(urName, 10);
            document.add(new Paragraph(FullscreenActivity.mTitle.toString(),TitleFontName));
            document.add(new Paragraph(FullscreenActivity.mAuthor.toString(),AuthorFontName));
            addImage(document,bmp);
            document.close();
        } catch (Exception | OutOfMemoryError e) {
            e.printStackTrace();
        }
    }

    private void addImage(Document document, Bitmap bmp) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] bArray = stream.toByteArray();
            Image myimage = Image.getInstance(bArray);
            float A4_width  = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin() - 80;
            float A4_height = document.getPageSize().getHeight() - document.topMargin() - document.bottomMargin();
            int bmp_width   = bmp.getWidth();
            int bmp_height  = bmp.getHeight();
            // If width is bigger than height, then landscape it!

            float x_scale = A4_width/(float)bmp_width;
            float y_scale = A4_height/(float)bmp_height;
            float new_width;
            float new_height;

            if (x_scale>y_scale) {
                new_width  = bmp_width  * y_scale;
                new_height = bmp_height * y_scale;
            } else {
                new_width  = bmp_width  * x_scale;
                new_height = bmp_height * x_scale;
            }
            myimage.scaleAbsolute(new_width,new_height);
            myimage.scaleToFit(A4_width,A4_height);
            myimage.setAlignment(Image.ALIGN_CENTER | Image.ALIGN_BOTTOM);
            document.add(myimage);

        } catch (Exception | OutOfMemoryError e) {
            e.printStackTrace();
        }
    }

    void createSelectedOSB(Context c, Preferences preferences, String selected, StorageAccess storageAccess) {
        folderstoexport = selected;
        if (backup_create_selected!=null) {
            backup_create_selected.cancel(true);
        }
        backup_create_selected = new Backup_Create_Selected(c, preferences, storageAccess);
        backup_create_selected.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private String makeBackupZipSelected(Context c, Preferences preferences, StorageAccess storageAccess) {
        // Get the date for the file
        Calendar cal = Calendar.getInstance();
        System.out.println("Current time => " + cal.getTime());

        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd", FullscreenActivity.locale);
        String formattedDate = df.format(cal.getTime());
        String backup = "OpenSongBackup_" + formattedDate + ".osb";
        zipDirSelected(c, preferences, backup, storageAccess);
        return backup;
    }

    private void zipDirSelected(Context c, Preferences preferences, String zipFileName, StorageAccess storageAccess) {
        Uri uri = storageAccess.getUriForItem(c, preferences, "", "", zipFileName);

        // Check the uri exists for the outputstream to be valid
        storageAccess.lollipopCreateFileForOutputStream(c, preferences, uri, null, "", "", zipFileName);

        OutputStream outputStream = storageAccess.getOutputStream(c,uri);
        try {
            outSelected = new ZipOutputStream(outputStream);

            // Go through each of the selected folders and add them to the zip file
            String[] whichfolders = folderstoexport.split("__%%__");
            for (int i = 0; i < whichfolders.length; i++) {
                if (!whichfolders[i].equals("")) {
                    whichfolders[i] = whichfolders[i].replace("%__", "");
                    whichfolders[i] = whichfolders[i].replace("__%", "");
                    addDirSelected(c, preferences, whichfolders[i], storageAccess);
                }
            }
            outSelected.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO not sure if subfolders (e.g. Band/Temp/Inner are added to the zipfile
    private void addDirSelected(Context c, Preferences preferences, String subfolder, StorageAccess storageAccess) {
        ArrayList<String> files = storageAccess.listFilesInFolder(c, preferences, "Songs", subfolder);
        byte[] tmpBuf = new byte[1024];
        for (String s:files) {
            Uri uri = storageAccess.getUriForItem(c, preferences, "Songs", subfolder, s);
            if (storageAccess.uriIsFile(c,uri)) {
                try {
                    InputStream inputStream = storageAccess.getInputStream(c, uri);
                    ZipEntry ze;
                    if (subfolder.equals(c.getString(R.string.mainfoldername))) {
                        ze = new ZipEntry(s);
                    } else {
                        ze = new ZipEntry(subfolder + "/" + s);
                    }
                    outSelected.putNextEntry(ze);
                    int len;
                    while ((len = inputStream.read(tmpBuf)) > 0) {
                        outSelected.write(tmpBuf, 0, len);
                    }
                    outSelected.closeEntry();
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Backup_Create_Selected extends AsyncTask<String, Void, String> {
        Context c;
        Intent emailIntent;
        StorageAccess storageAccess;
        Preferences preferences;

        Backup_Create_Selected(Context context, Preferences p, StorageAccess sA) {
            c = context;
            storageAccess = sA;
            preferences = p;
        }

        @Override
        protected String doInBackground(String... strings) {
            return makeBackupZipSelected(c, preferences, storageAccess);
        }

        boolean cancelled = false;

        @Override
        protected void onCancelled() {
            cancelled = true;
        }

        @Override
        public void onPostExecute(String s) {
            if (!cancelled) {
                try {
                    Uri uri = storageAccess.getUriForItem(c, preferences, "", "", s);
                    FullscreenActivity.myToastMessage = c.getString(R.string.backup_success);
                    ShowToast.showToast(c);
                    emailIntent = exportBackup(c, uri);
                    ((Activity) c).startActivityForResult(Intent.createChooser(emailIntent, c.getString(R.string.backup_info)), 12345);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String prepareChordProFile(Context c) {
        // This converts an OpenSong file into a ChordPro file
        StringBuilder s = new StringBuilder("{ns}\n");
        s.append("{t:").append(FullscreenActivity.mTitle).append("}\n");
        s.append("{st:").append(FullscreenActivity.mAuthor).append("}\n\n");

        // Go through each song section and add the ChordPro formatted chords
        for (int f=0;f<FullscreenActivity.songSections.length;f++) {
            s.append(ProcessSong.songSectionChordPro(c, f, false));
        }
        String string = s.toString();
        string = string.replace("\n\n\n", "\n\n");
        return string;
    }
    private String prepareOnSongFile(Context c) {
        // This converts an OpenSong file into a OnSong file
        StringBuilder s = new StringBuilder(FullscreenActivity.mTitle + "\n");
        if (!FullscreenActivity.mAuthor.equals("")) {
            s.append(FullscreenActivity.mAuthor).append("\n");
        }
        if (!FullscreenActivity.mCopyright.equals("")) {
            s.append("Copyright: ").append(FullscreenActivity.mCopyright).append("\n");
        }
        if (!FullscreenActivity.mKey.equals("")) {
            s.append("Key: ").append(FullscreenActivity.mKey).append("\n\n");
        }

        // Go through each song section and add the ChordPro formatted chords
        for (int f=0;f<FullscreenActivity.songSections.length;f++) {
            s.append(ProcessSong.songSectionChordPro(c, f, true));
        }

        String string = s.toString();
        string = string.replace("\n\n\n", "\n\n");
        return string;
    }
    private String prepareTextFile(Context c) {
        // This converts an OpenSong file into a text file
        StringBuilder s = new StringBuilder(FullscreenActivity.mTitle + "\n");
        if (!FullscreenActivity.mAuthor.equals("")) {
            s.append(FullscreenActivity.mAuthor).append("\n");
        }
        if (!FullscreenActivity.mCopyright.equals("")) {
            s.append("Copyright: ").append(FullscreenActivity.mCopyright).append("\n");
        }
        if (!FullscreenActivity.mKey.equals("")) {
            s.append("Key: ").append(FullscreenActivity.mKey).append("\n\n");
        }

        // Go through each song section and add the text trimmed lines
        for (int f=0;f<FullscreenActivity.songSections.length;f++) {
            s.append(ProcessSong.songSectionText(c, f));
        }

        String string = s.toString();
        string = string.replace("\n\n\n", "\n\n");
        return string;
    }

}