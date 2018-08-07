package com.garethevans.church.opensongtablet;

import android.content.Context;
import android.support.v4.provider.DocumentFile;

import java.io.OutputStream;

class CustomSlide {

    void addCustomSlide(Context c, DocumentFile homeFolder) {
        String where, reusablewhere, subfolder, templocator;

        // Get rid of illegal characters
        String filetitle = FullscreenActivity.customslide_title.replaceAll("[|?*<\":>+\\[\\]']", " ");

        switch (FullscreenActivity.noteorslide) {
            case "note":
                where = "Notes";
                subfolder = "_cache";
                reusablewhere = "";
                templocator = c.getResources().getString(R.string.note);
                FullscreenActivity.customimage_list = "";
                break;
            case "slide":
                where = "Slides";
                subfolder = "_cache";
                reusablewhere = "";
                templocator = c.getResources().getString(R.string.slide);
                FullscreenActivity.customimage_list = "";
                break;
            case "scripture":
                where = "Scripture";
                subfolder = "_cache";
                reusablewhere = "";
                templocator = c.getResources().getString(R.string.scripture);
                FullscreenActivity.customreusable = false;
                FullscreenActivity.customimage_list = "";
                break;
            default:
                where = "Images";
                subfolder = "_cache";
                reusablewhere = "";
                templocator = c.getResources().getString(R.string.image);
                break;
        }

        // If slide content is empty - put the title in
        if ((FullscreenActivity.customslide_content.isEmpty() ||
                FullscreenActivity.customslide_content.equals("")) &&
                !FullscreenActivity.noteorslide.equals("image")) {
            FullscreenActivity.customslide_content = FullscreenActivity.customslide_title;
        }

        // Prepare the custom slide so it can be viewed in the app
        // When exporting/saving the set, the contents get grabbed from this

        FullscreenActivity.mynewXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        FullscreenActivity.mynewXML += "<song>\n";
        FullscreenActivity.mynewXML += "  <title>" + FullscreenActivity.customslide_title + "</title>\n";
        FullscreenActivity.mynewXML += "  <author></author>\n";
        FullscreenActivity.mynewXML += "  <user1>" + FullscreenActivity.customimage_time + "</user1>\n";  // This is used for auto advance time
        FullscreenActivity.mynewXML += "  <user2>" + FullscreenActivity.customimage_loop + "</user2>\n";  // This is used for loop on or off
        FullscreenActivity.mynewXML += "  <user3>" + FullscreenActivity.customimage_list + "</user3>\n";  // This is used as links to a background images
        FullscreenActivity.mynewXML += "  <aka></aka>\n";
        FullscreenActivity.mynewXML += "  <key_line></key_line>\n";
        FullscreenActivity.mynewXML += "  <hymn_number></hymn_number>\n";
        FullscreenActivity.mynewXML += "  <lyrics>" + FullscreenActivity.customslide_content + "</lyrics>\n";
        FullscreenActivity.mynewXML += "</song>";

        FullscreenActivity.mynewXML = FullscreenActivity.mynewXML.replace("&amp;", "&");
        FullscreenActivity.mynewXML = FullscreenActivity.mynewXML.replace("&", "&amp;");

        // Now write the modified song
        StorageAccess storageAccess = new StorageAccess();
        OutputStream os = storageAccess.getOutputStream(c,homeFolder, where,subfolder,filetitle);
        storageAccess.writeStringToFile(c,os,FullscreenActivity.mynewXML);

        // If this is to be a reusable custom slide
        if (FullscreenActivity.customreusable) {
            // Now write the modified song
            OutputStream osr = storageAccess.getOutputStream(c,homeFolder, where,reusablewhere,filetitle);
            storageAccess.writeStringToFile(c,osr,FullscreenActivity.mynewXML);
            FullscreenActivity.customreusable = false;
        }

        // Add to set
        FullscreenActivity.whatsongforsetwork = "$**_**" + templocator + "/" + filetitle + "_**$";

        // Allow the song to be added, even if it is already there
        FullscreenActivity.mySet = FullscreenActivity.mySet + FullscreenActivity.whatsongforsetwork;

        // Save the set and other preferences
        Preferences.savePreferences();

        // Show the current set
        SetActions setActions = new SetActions();
        setActions.prepareSetList();
    }
}