package com.garethevans.church.opensongtablet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportPreparer {

	private String setxml = "";
	String settext = "";
	private String song_title = "", song_author = "", song_hymnnumber = "", song_key = "";
    private DocumentFile songfile = null;
    private ArrayList<String> filesinset = new ArrayList<>(), filesinset_ost = new ArrayList<>();
    Image image;
    //static Backup_Create backup_create;
    @SuppressLint("StaticFieldLeak")
    private Backup_Create_Selected backup_create_selected;
    Context context;
    @SuppressLint("StaticFieldLeak")
    static Activity activity;
    String folderstoexport = "";
    private ZipOutputStream outSelected;

	private boolean setParser(Context c, DocumentFile homeFolder) throws IOException, XmlPullParserException {
        StorageAccess storageAccess = new StorageAccess();

        StringBuilder settext = new StringBuilder();
        FullscreenActivity.exportsetfilenames.clear();
        FullscreenActivity.exportsetfilenames_ost.clear();
        filesinset.clear();
        filesinset_ost.clear();

		// First up, load the set
        DocumentFile settoparse= storageAccess.getFileLocationAsDocumentFile(c,homeFolder,"Sets","",FullscreenActivity.settoload);
        if (!settoparse.isFile() || !settoparse.exists()) {
			return false;
		}

		try {
		    InputStream is = storageAccess.getInputStream(c,homeFolder,"Sets","",FullscreenActivity.settoload);
			InputStreamReader streamReaderSet = new InputStreamReader(is);
			BufferedReader bufferedReaderSet = new BufferedReader(streamReaderSet);
			setxml = storageAccess.readTextFile(c,is);
			is.close();
			bufferedReaderSet.close();
			is.close(); // close the file
		} catch (Exception e) {
			e.printStackTrace();
		}

		XmlPullParserFactory factory;
		factory = XmlPullParserFactory.newInstance();

		factory.setNamespaceAware(true);
		XmlPullParser xpp;
		xpp = factory.newPullParser();

		xpp.setInput(new StringReader(setxml));
		int eventType;

		LoadXML loadXML = new LoadXML();

		eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				if (xpp.getName().equals("slide_group")) {
                    switch (xpp.getAttributeValue(null, "type")) {
                        case "song":
                            songfile = null;
                            String thisline;
                            songfile = storageAccess.getFileLocationAsDocumentFile(c,homeFolder, "Songs",
                                    loadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "path")),
                                    loadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "name")));
                            // Ensure there is a folder '/'
                            if (xpp.getAttributeValue(null, "path").equals("")) {
                                thisline = "/" + loadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "name"));
                            } else {
                                thisline = loadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "path"))
                                        + loadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "name"));
                            }
                            filesinset.add(thisline);
                            filesinset_ost.add(thisline);

                            // Set the default values exported with the text for the set
                            song_title = loadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "name"));
                            song_author = "";
                            song_hymnnumber = "";
                            song_key = "";
                            // Now try to improve on this info
                            if (songfile.exists() && songfile.isFile()) {
                                // Read in the song title, author, copyright, hymnnumber, key
                                getSongData(c);
                            }
                            settext.append(song_title);
                            if (!song_author.isEmpty()) {
                                settext.append(", ");
                                settext.append(song_author);
                            }
                            if (!song_hymnnumber.isEmpty()) {
                                settext.append(", #");
                                settext.append(song_hymnnumber);
                            }
                            if (!song_key.isEmpty()) {
                                settext.append(" (");
                                settext.append(song_key);
                                settext.append(")");
                            }
                            settext.append("\n");
                            break;
                        case "scripture":
                            settext.append(loadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "name")));
                            settext.append("\n");

                            break;
                        case "custom":
                            // Decide if this is a note or a slide
                            if (xpp.getAttributeValue(null, "name").contains("# " + c.getResources().getString(R.string.note) + " # - ")) {
                                String nametemp = loadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "name"));
                                nametemp = nametemp.replace("# " + c.getResources().getString(R.string.note) + " # - ", "");
                                settext.append(nametemp);
                                settext.append("\n");
                            } else {
                                settext.append(loadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "name")));
                                settext.append("\n");
                            }
                            break;
                        case "image":
                            // Go through the descriptions of each image and extract the absolute file locations
                            boolean allimagesdone = false;
                            ArrayList<String> theseimages = new ArrayList<>();
                            String imgname;
                            imgname = loadXML.parseFromHTMLEntities(xpp.getAttributeValue(null, "name"));
                            while (!allimagesdone) { // Keep iterating unless the current eventType is the end of the document
                                if (eventType == XmlPullParser.START_TAG) {
                                    if (xpp.getName().equals("description")) {
                                        xpp.next();
                                        theseimages.add(loadXML.parseFromHTMLEntities(xpp.getText()));
                                        filesinset.add(loadXML.parseFromHTMLEntities(xpp.getText()));
                                        filesinset_ost.add(loadXML.parseFromHTMLEntities(xpp.getText()));
                                    }

                                } else if (eventType == XmlPullParser.END_TAG) {
                                    if (xpp.getName().equals("slide_group")) {
                                        allimagesdone = true;
                                    }
                                }

                                eventType = xpp.next(); // Set the current event type from the return value of next()
                            }
                            // Go through each of these images and add a line for each one
                            settext.append(imgname);
                            settext.append("\n");
                            for (int im = 0; im < theseimages.size(); im++) {
                                settext.append("     - ");
                                settext.append(theseimages.get(im));
                                settext.append("\n");
                            }
                            break;
                    }
				}
			}
			eventType = xpp.next();		
		}

		// Send the settext back to the FullscreenActivity as emailtext
		FullscreenActivity.emailtext = settext.toString();
        FullscreenActivity.exportsetfilenames = filesinset;
        FullscreenActivity.exportsetfilenames_ost = filesinset_ost;
        return true;
	}

	private void getSongData(Context c) throws XmlPullParserException, IOException {
        StorageAccess storageAccess = new StorageAccess();

        // Parse the song xml.
		// Grab the title, author, lyrics_withchords, lyrics_withoutchords, copyright, hymnnumber, key

		// Initialise all the xml tags a song should have that we want
		String songxml = "";
		song_title = "";
		song_author = "";
        //String song_lyrics_withchords = "";
        //StringBuilder song_lyrics_withoutchords = new StringBuilder();
        song_hymnnumber = "";
		song_key = "";

		try {
		    InputStream inputStreamSong = storageAccess.getInputStreamFromUri(c,songfile.getUri());
			InputStreamReader streamReaderSong = new InputStreamReader(inputStreamSong);
            BufferedReader bufferedReaderSong = new BufferedReader(streamReaderSong);
			songxml = storageAccess.readTextFile(c, inputStreamSong);
			inputStreamSong.close();
			bufferedReaderSong.close();
			inputStreamSong.close(); // close the file
		} catch (IOException e) {
			e.printStackTrace();
		}

		//Change the line breaks and Slides to better match OpenSong
		songxml = songxml.replaceAll("\r\n", "\n");
		songxml = songxml.replaceAll("\r", "\n");
		songxml = songxml.replaceAll("\t", "    ");
		songxml = songxml.replaceAll("\\t", "    ");
		songxml = songxml.replaceAll("\f", "    ");
		songxml = songxml.replace("\r", "");
		songxml = songxml.replace("\t", "    ");
		songxml = songxml.replace("\b", "    ");
		songxml = songxml.replace("\f", "    ");
        songxml = songxml.replace("&#0;","");

		// Extract all of the key bits of the song
		XmlPullParserFactory factorySong;
		factorySong = XmlPullParserFactory.newInstance();

		factorySong.setNamespaceAware(true);
		XmlPullParser xppSong;
		xppSong = factorySong.newPullParser();

		xppSong.setInput(new StringReader(songxml));

		LoadXML loadXML = new LoadXML();
		int eventType;
		eventType = xppSong.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
                switch (xppSong.getName()) {
                    case "author":
                        song_author = loadXML.parseFromHTMLEntities(xppSong.nextText());
                        break;
                    case "title":
                        song_title = loadXML.parseFromHTMLEntities(xppSong.nextText());
                        break;
                    /*case "lyrics":
                        song_lyrics_withchords = loadXML.parseFromHTMLEntities(xppSong.nextText());
                        break;*/
                    case "hymn_number":
                        song_hymnnumber = loadXML.parseFromHTMLEntities(xppSong.nextText());
                        break;
                    case "key":
                        song_key = loadXML.parseFromHTMLEntities(xppSong.nextText());
                        break;
                }
			}
			eventType = xppSong.next();
		}
		/*// Remove the chord lines from the song lyrics
		String[] templyrics = song_lyrics_withchords.split("\n");
		// Only add the lines that don't start with a .
		int numlines = templyrics.length;
		if (numlines>0) {
            for (String templyric : templyrics) {
                if (!templyric.startsWith(".")) {
                    song_lyrics_withoutchords.append(templyric).append("\n");
                }
            }
		}*/
	}

	Intent exportSet(Context c, DocumentFile homeFolder) {
        StorageAccess storageAccess = new StorageAccess();

        String nicename = FullscreenActivity.settoload;
        Uri text = null;
        Uri desktop = null;
        Uri osts = null;

        // Prepare a txt version of the set.
        try {
            if (!setParser(c, homeFolder)) {
                Log.d("d","Problem parsing the set");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, nicename);
        emailIntent.putExtra(Intent.EXTRA_TITLE, nicename);
        emailIntent.putExtra(Intent.EXTRA_TEXT, nicename + "\n\n" + FullscreenActivity.emailtext);

        DocumentFile setfile  = storageAccess.getFileLocationAsDocumentFile(c, homeFolder,"Sets", "", FullscreenActivity.settoload);
        DocumentFile ostsfile  = storageAccess.getFileLocationAsDocumentFile(c, homeFolder, "Export", "", FullscreenActivity.settoload+".osts");

        if (!setfile.exists() || !setfile.canRead()) {
            return null;
        }

        if (FullscreenActivity.exportText) {
            Uri uri = storageAccess.getFileLocationAsDocumentFile(c, homeFolder,"Export", "", FullscreenActivity.settoload+".txt").getUri();
            storageAccess.writeDocumentFile(c,uri,"text",FullscreenActivity.emailtext,null);
            text = uri;
        }

        FullscreenActivity.emailtext = "";

        if (FullscreenActivity.exportDesktop) {
            desktop = setfile.getUri();
        }

        if (FullscreenActivity.exportOpenSongAppSet) {
            // Copy the set file to an .osts file
            try {
                InputStream in = storageAccess.getInputStreamFromUri(c, setfile.getUri());
                OutputStream out = storageAccess.getOutputStreamFromUri(c, ostsfile.getUri());
                byte[] buffer = storageAccess.readBytes(in);
                storageAccess.writeBytes(c,out,buffer);
                in.close();
                osts = ostsfile.getUri();
            } catch (Exception e) {
                // Error
                e.printStackTrace();
            }
        }

        ArrayList<Uri> uris = new ArrayList<>();
        if (text!=null) {
            uris.add(text);
        }
        if (osts!=null) {
            uris.add(osts);
        }
        if (desktop!=null) {
            uris.add(desktop);
        }

        // Go through each song in the set and attach them
        // Also try to attach a copy of the song ending in .ost, as long as they aren't images
        if (FullscreenActivity.exportOpenSongApp) {
            for (int q = 0; q < FullscreenActivity.exportsetfilenames.size(); q++) {
                // Remove any subfolder from the exportsetfilenames_ost.get(q)
                String tempsong_ost = FullscreenActivity.exportsetfilenames_ost.get(q);
                tempsong_ost = tempsong_ost.substring(tempsong_ost.indexOf("/") + 1);

                DocumentFile songtoload = storageAccess.getFileLocationAsDocumentFile(c,homeFolder, "Songs","",FullscreenActivity.exportsetfilenames.get(q));
                DocumentFile ostsongcopy = storageAccess.getFileLocationAsDocumentFile(c, homeFolder,"Notes","_cache",tempsong_ost + ".ost");

                boolean isimage = false;
                if (songtoload.toString().endsWith(".jpg") || songtoload.toString().endsWith(".JPG") ||
                        songtoload.toString().endsWith(".jpeg") || songtoload.toString().endsWith(".JPEG") ||
                        songtoload.toString().endsWith(".gif") || songtoload.toString().endsWith(".GIF") ||
                        songtoload.toString().endsWith(".png") || songtoload.toString().endsWith(".PNG") ||
                        songtoload.toString().endsWith(".bmp") || songtoload.toString().endsWith(".BMP") ||
                        songtoload.getType().contains("image")) {

                    isimage = true;
                }

                // Copy the song
                if (songtoload.exists()) {
                    try {
                        if (!isimage) {
                            InputStream in = storageAccess.getInputStreamFromUri(c,songtoload.getUri());
                            OutputStream out = storageAccess.getOutputStreamFromUri(c, ostsongcopy.getUri());

                            byte[] buffer = storageAccess.readBytes(in);
                            // write the output file (You have now copied the file)
                            storageAccess.writeBytes(c,out,buffer);

                            Uri urisongs_ost = ostsongcopy.getUri();
                            uris.add(urisongs_ost);
                        }
                    } catch (Exception e) {
                        // Error
                        e.printStackTrace();
                    }
                }
            }
        }
        if (FullscreenActivity.exportDesktop) {
            for (int q = 0; q < FullscreenActivity.exportsetfilenames.size(); q++) {
                DocumentFile songtoload = storageAccess.getFileLocationAsDocumentFile(c, homeFolder,"Songs", "",
                        FullscreenActivity.exportsetfilenames.get(q));
                Uri urisongs = songtoload.getUri();
                uris.add(urisongs);
            }
        }

        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        return emailIntent;
    }

	Intent exportSong(Context c, DocumentFile homeFolder, Bitmap bmp) {
        StorageAccess storageAccess = new StorageAccess();

        // Prepare the appropriate attachments
        String emailcontent = "";
        Uri text = null;
        Uri ost = null;
        Uri desktop = null;
        Uri chopro = null;
        Uri onsong = null;
        Uri image = null;
        Uri pdf = null;
        DocumentFile newfile;

        // Prepare a txt version of the song.
        prepareTextFile(c);
        // Check we have a directory to save these
        emailcontent += FullscreenActivity.exportText_String;

        if (FullscreenActivity.exportText) {
            newfile = storageAccess.getFileLocationAsDocumentFile(c, homeFolder,"Export","",
                    FullscreenActivity.songfilename+".txt");
            OutputStream os = storageAccess.getOutputStreamFromUri(c, newfile.getUri());
            storageAccess.writeStringToFile(c,os,FullscreenActivity.exportText_String);
            text = newfile.getUri();
        }

        if (FullscreenActivity.exportOpenSongApp) {
            // Prepare an ost version of the song.
            newfile = storageAccess.getFileLocationAsDocumentFile(c, homeFolder,"Export","",
                    FullscreenActivity.songfilename+".ost");
            OutputStream os = storageAccess.getOutputStreamFromUri(c, newfile.getUri());
            InputStream is = storageAccess.getInputStream(c,homeFolder,"Songs",FullscreenActivity.whichSongFolder,
                    FullscreenActivity.songfilename);
            storageAccess.copyFile(c,is,os);
            ost = newfile.getUri();
        }

        if (FullscreenActivity.exportDesktop) {
            // Prepare a desktop version of the song.
            newfile = storageAccess.getFileLocationAsDocumentFile(c,homeFolder,"Export","",
                    FullscreenActivity.songfilename);
            OutputStream os = storageAccess.getOutputStreamFromUri(c, newfile.getUri());
            InputStream is = storageAccess.getInputStream(c,homeFolder,"Songs",FullscreenActivity.whichSongFolder,
                    FullscreenActivity.songfilename);
            storageAccess.copyFile(c,is,os);
            ost = newfile.getUri();

            desktop = newfile.getUri();
        }

        if (FullscreenActivity.exportChordPro) {
            // Prepare a chordpro version of the song.
            newfile = storageAccess.getFileLocationAsDocumentFile(c,homeFolder,"Export","",
                    FullscreenActivity.songfilename+".chopro");
            storageAccess.writeDocumentFile(c, newfile.getUri(), "chopro", prepareChordProFile(c), null);
            chopro = newfile.getUri();
        }

        if (FullscreenActivity.exportOnSong) {
            // Prepare an onsong version of the song.
            newfile = storageAccess.getFileLocationAsDocumentFile(c,homeFolder,"Export","",
                    FullscreenActivity.songfilename+".onsong");
            storageAccess.writeDocumentFile(c, newfile.getUri(), "onsong", prepareOnSongFile(c), null);
            onsong = newfile.getUri();
        }

        if (FullscreenActivity.exportImage) {
            // Prepare an image/png version of the song.
            newfile = storageAccess.getFileLocationAsDocumentFile(c,homeFolder,"Export","",
                    FullscreenActivity.songfilename+".png");
            storageAccess.writeDocumentFile(c, newfile.getUri(), "png", prepareOnSongFile(c), bmp);
            image = newfile.getUri();
        }

        if (FullscreenActivity.exportPDF) {
            // Prepare a pdf version of the song.
            newfile = storageAccess.getFileLocationAsDocumentFile(c,homeFolder,"Export","",
                    FullscreenActivity.songfilename+".pdf");
            storageAccess.writeDocumentFile(c, newfile.getUri(), "onsong", prepareOnSongFile(c), null);
            makePDF(bmp, storageAccess.getOutputStreamFromUri(c, newfile.getUri()));
            pdf = newfile.getUri();
        }

        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_TITLE, FullscreenActivity.songfilename);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, FullscreenActivity.songfilename);
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailcontent);
        FullscreenActivity.emailtext = "";

        // Add the attachments
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
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        return emailIntent;
    }

    private Intent exportBackup(Context c, DocumentFile f) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_TITLE, c.getString(R.string.backup_info));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT,  c.getString(R.string.backup_info));
        emailIntent.putExtra(Intent.EXTRA_TEXT,  c.getString(R.string.backup_info));
        FullscreenActivity.emailtext = "";

        Uri uri = f.getUri();
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(uri);

        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        return emailIntent;
    }

    Intent exportActivityLog(Context c, DocumentFile homeFolder) {
        StorageAccess storageAccess = new StorageAccess();

        String title = c.getString(R.string.app_name) + ": " + c.getString(R.string.edit_song_ccli);
	    String subject = title + " - " + c.getString(R.string.ccli_view);
	    String text = c.getString(R.string.ccli_church) + ": " + FullscreenActivity.ccli_church + "\n";
	    text += c.getString(R.string.ccli_licence) + ": " + FullscreenActivity.ccli_licence + "\n\n";
        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_TITLE, title);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, text);
        // Add the attachments
        ArrayList<Uri> uris = new ArrayList<>();
        DocumentFile f = storageAccess.getFileLocationAsDocumentFile(c,homeFolder,"Settings","","ActivityLog.xml");
        if (!f.exists()) {
            PopUpCCLIFragment popUpCCLIFragment = new PopUpCCLIFragment();
            popUpCCLIFragment.createBlankXML(c);
        }
        uris.add(f.getUri());
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        return emailIntent;
    }

    private void makePDF(Bitmap bmp, OutputStream os) {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, os);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addImage(Document document, Bitmap bmp) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] bArray = stream.toByteArray();
            image = Image.getInstance(bArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        image.scaleAbsolute(new_width,new_height);
        image.scaleToFit(A4_width,A4_height);
        image.setAlignment(Image.ALIGN_CENTER | Image.ALIGN_BOTTOM);
        //image.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
        // image.scaleAbsolute(150f, 150f);
        try {
            document.add(image);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void createSelectedOSB(Context c, DocumentFile homeFolder) {
	    activity = (Activity) c;
        if (backup_create_selected!=null) {
            backup_create_selected.cancel(true);
        }
        backup_create_selected = new Backup_Create_Selected(c, homeFolder);
        backup_create_selected.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressLint("StaticFieldLeak")
    class Backup_Create_Selected extends AsyncTask<String, Void, String> {
        @SuppressLint("StaticFieldLeak")
        Context c;
        DocumentFile homeFolder;
        Backup_Create_Selected(Context context, DocumentFile home) {
            c = context;
            homeFolder = home;
        }
        @Override
        protected String doInBackground(String... strings) {
            return makeBackupZipSelected(c, homeFolder);
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
                    StorageAccess storageAccess = new StorageAccess();
                    DocumentFile f = storageAccess.getFileLocationAsDocumentFile(c,  homeFolder,"", "", s);
                    FullscreenActivity.myToastMessage = c.getString(R.string.backup_success);
                    ShowToast.showToast(c);
                    Intent emailIntent = exportBackup(c, f);
                    activity.startActivityForResult(Intent.createChooser(emailIntent, c.getString(R.string.backup_info)), 12345);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private String makeBackupZipSelected(Context ctx, DocumentFile homeFolder) {
        // Get the date for the file
        Calendar c = Calendar.getInstance();
        System.out.println("Current time => " + c.getTime());

        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd", FullscreenActivity.locale);
        String formattedDate = df.format(c.getTime());
        String backup = "OpenSongBackup_" + formattedDate + ".osb";
        try {
            zipDirSelected(ctx, homeFolder, backup);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return backup;
    }
    private void zipDirSelected(Context c, DocumentFile homeFolder, String zipFileName) throws Exception {
        StorageAccess storageAccess = new StorageAccess();
        OutputStream os = storageAccess.getOutputStream(c, homeFolder,"", "", zipFileName);
        outSelected = new ZipOutputStream(os);
        System.out.println("Creating : " + zipFileName);

        // Go through each of the selected folders and add them to the zip file
        String[] whichfolders = folderstoexport.split("__%%__");
        for (int i=0;i<whichfolders.length;i++) {
            if (!whichfolders[i].equals("")) {
                whichfolders[i] = whichfolders[i].replace("%__", "");
                whichfolders[i] = whichfolders[i].replace("__%", "");
                DocumentFile dirObj = storageAccess.getFileLocationAsDocumentFile(c, homeFolder,"Songs", whichfolders[i],"");
                addDirSelected(c, homeFolder, dirObj);
            }
        }
        outSelected.close();
    }
    private void addDirSelected(Context c, DocumentFile homeFolder, DocumentFile dirObj) throws IOException {
        StorageAccess storageAccess = new StorageAccess();
        DocumentFile[] files = dirObj.listFiles();
	    byte[] tmpBuff;

        for (DocumentFile file : files) {
            if (file.isFile()) {
                InputStream in = storageAccess.getInputStreamFromUri(c,file.getUri());
                // Try to get the absolute path and remove the rubbish (i.e. Songs folder)
                tmpBuff = storageAccess.readBytes(in);
                String songspath = storageAccess.getFileLocationAsDocumentFile(c,homeFolder,"Songs","","").getUri().getPath();
                String path = file.getUri().getPath();

                path = path.replace (songspath,"");

                System.out.println(" Adding: " + path);
                outSelected.putNextEntry(new ZipEntry(path));
                int len;
                while ((len = in.read(tmpBuff)) > 0) {
                    outSelected.write(tmpBuff, 0, len);
                }
                outSelected.closeEntry();
                in.close();
            }
        }
    }

    private String prepareChordProFile(Context c) {
        // This converts an OpenSong file into a ChordPro file
        FullscreenActivity.exportChordPro_String = "";
        StringBuilder s = new StringBuilder("{ns}\n");
        s.append("{t:").append(FullscreenActivity.mTitle).append("}\n");
        s.append("{st:").append(FullscreenActivity.mAuthor).append("}\n\n");

        // Go through each song section and add the ChordPro formatted chords
        for (int f=0;f<FullscreenActivity.songSections.length;f++) {
            s.append(ProcessSong.songSectionChordPro(c, f, false));
        }

        s = new StringBuilder(s.toString().replace("\n\n\n", "\n\n"));
        FullscreenActivity.exportChordPro_String = s.toString();
        return s.toString();
    }
    private String prepareOnSongFile(Context c) {
        // This converts an OpenSong file into a OnSong file
        FullscreenActivity.exportOnSong_String = "";
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

        s = new StringBuilder(s.toString().replace("\n\n\n", "\n\n"));
        FullscreenActivity.exportOnSong_String = s.toString();
        return s.toString();
    }
    private void prepareTextFile(Context c) {
        // This converts an OpenSong file into a text file
        FullscreenActivity.exportText_String = "";
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

        s = new StringBuilder(s.toString().replace("\n\n\n", "\n\n"));
        FullscreenActivity.exportText_String = s.toString();
    }

}
