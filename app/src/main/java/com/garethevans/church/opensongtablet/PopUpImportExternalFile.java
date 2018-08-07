package com.garethevans.church.opensongtablet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PopUpImportExternalFile extends DialogFragment {

    static PopUpImportExternalFile newInstance() {
        PopUpImportExternalFile frag;
        frag = new PopUpImportExternalFile();
        return frag;
    }

    public interface MyInterface {
        void refreshAll();
        void onSongImportDone(String message);
        void backupInstall(String message);
        void openFragment();
    }

    private MyInterface mListener;

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

    TextView itemTitle_TextView;
    EditText fileTitle_EditText;
    TextView fileType_heading;
    TextView fileType_TextView;
    TextView messageOnSong_TextView;
    TextView messageOpenSong_TextView;
    TextView chooseFolder_TextView;
    Spinner chooseFolder_Spinner;
    CheckBox overWrite_CheckBox;
    ImportOnSongBackup import_os;
    Backup_Install import_osb;
    String mTitle = "";

    //static ArrayList<String> newtempfolders;
    String moveToFolder;
    String backupchosen;
    //Backup_Install backup_install;
    InputStream inputStream;
    String scheme = "";
    ArrayList<String> backups = new ArrayList<>();
    String message = "";
    View V;
    FloatingActionButton saveMe;
    FloatingActionButton closeMe;
    ProgressBar progressbar;
    TextView title;

    // StorageStuff
    StorageAccess storageAccess;
    DocumentFile homeFolder;

    @Override
    public void onStart() {
        super.onStart();

        // safety check
        if (getActivity() != null && getDialog() != null) {
            PopUpSizeAndAlpha.decoratePopUp(getActivity(),getDialog());
        }
    }

    public void setTitle(String s) {
        try {
            if (title != null) {
                title.setText(s);
            } else {
                if (getDialog() != null) {
                    getDialog().setTitle(s);
                }
            }
        } catch (Exception e) {
            Log.d("d","Problem with title");
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

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(true);

        V = inflater.inflate(R.layout.popup_importexternalfile, container, false);

        title = V.findViewById(R.id.dialogtitle);
        title.setText(getActivity().getResources().getString(R.string.importnewsong));
        final FloatingActionButton closeMe = V.findViewById(R.id.closeMe);
        closeMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomAnimations.animateFAB(closeMe,getActivity());
                closeMe.setEnabled(false);
                dismiss();
            }
        });
        saveMe = V.findViewById(R.id.saveMe);
        saveMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomAnimations.animateFAB(saveMe,getActivity());
                saveMe.setEnabled(false);
                defaultSaveAction();
            }
        });

        // Initialise the views
        itemTitle_TextView = V.findViewById(R.id.itemTitle_TextView);
        fileTitle_EditText = V.findViewById(R.id.fileTitle_EditText);
        fileType_heading = V.findViewById(R.id.fileType_heading);
        fileType_TextView = V.findViewById(R.id.fileType_TextView);
        chooseFolder_TextView = V.findViewById(R.id.chooseFolder_TextView);
        messageOnSong_TextView = V.findViewById(R.id.messageOnSong_TextView);
        messageOpenSong_TextView = V.findViewById(R.id.messageOpenSong_TextView);
        chooseFolder_Spinner = V.findViewById(R.id.chooseFolder_Spinner);
        overWrite_CheckBox = V.findViewById(R.id.overWrite_CheckBox);
        progressbar = V.findViewById(R.id.progressbar);

        // By default, we will assume this is a song
        FullscreenActivity.file_type = getResources().getString(R.string.options_song);

        // Decide if this has been actioned by needtoimport
        switch (FullscreenActivity.whattodo) {
            case "doimport":
                try {
                    scheme = FullscreenActivity.file_uri.getScheme();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                switch (scheme) {
                    case "content":
                        schemeContent();
                        break;

                    case "file":
                        if (!FullscreenActivity.file_name.endsWith(".backup") && !FullscreenActivity.file_name.endsWith(".osb")) {
                            schemeFile();
                        }
                        break;
                    default:
                        dismiss();
                        break;
                }

                if (FullscreenActivity.file_name.endsWith(".ost")) {
                    // This is definitely a song
                    setupOSTImport();

                } else if (FullscreenActivity.file_name.endsWith(".usr") || FullscreenActivity.file_name.endsWith(".USR")) {
                    // This is a song from SongSelect
                    setupUSRImport();

                } else if (FullscreenActivity.file_name.endsWith(".pdf") || FullscreenActivity.file_name.endsWith(".PDF") ||
                        FullscreenActivity.file_name.endsWith(".png") || FullscreenActivity.file_name.endsWith(".PNG") ||
                        FullscreenActivity.file_name.endsWith(".jpg") || FullscreenActivity.file_name.endsWith(".JPG") ||
                        FullscreenActivity.file_name.endsWith(".jpeg") || FullscreenActivity.file_name.endsWith(".JPG") ||
                        FullscreenActivity.file_name.endsWith(".gif") || FullscreenActivity.file_name.endsWith(".gif")) {
                    // This is an image or a pdf
                    setupImageOrPDFImport();

                } else if (FullscreenActivity.file_name.endsWith(".osts")) {
                    // This is definitely a set
                    setupOSTSImport();

                } else if (FullscreenActivity.file_name.endsWith(".osb")) {
                    // This is an OpenSong backup
                    setupOSBImport();

                } else if (FullscreenActivity.file_name.endsWith(".backup")) {
                    // This is definitely an opensong archive
                    setupOSImport();

                } else if (Bible.isYouVersionScripture(FullscreenActivity.incoming_text)) {
                    // It is a scripture, so create the Scripture file
                    // Get the bible translation
                    setupBibleImport();

                } else {
                    // Unknown file
                    setupUnknownImport();
                }

                break;
            case "importos":
                setupOSImport();

                break;
            case "importosb":
                setupOSBImport();
                break;
        }
         FullscreenActivity.whattodo = "";

        PopUpSizeAndAlpha.decoratePopUp(getActivity(),getDialog());

        return V;
    }

    public void defaultSaveAction() {
        // Now check that the file doesn't already exist.  If it does alert the user to try again
        DocumentFile testfile;
        String name = "NEW";
        try {
            name = fileTitle_EditText.getText().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (moveToFolder==null) {
            moveToFolder=getResources().getString(R.string.mainfoldername);
        }

        testfile = storageAccess.getFileLocationAsDocumentFile(getActivity(), homeFolder,"Songs",
                moveToFolder, name);

        if (FullscreenActivity.file_type.equals(getResources().getString(R.string.options_set))) {
            testfile = storageAccess.getFileLocationAsDocumentFile(getActivity(), homeFolder, "Sets",
                    moveToFolder, name);
        }

        // Does it exist?
        if (testfile.exists() && !overWrite_CheckBox.isChecked()) {
            Toast.makeText(getActivity(), getResources().getString(R.string.file_exists), Toast.LENGTH_LONG).show();
        } else {
            FullscreenActivity.myToastMessage = getResources().getString(R.string.ok);
            Uri from = Uri.parse(FullscreenActivity.file_location);

            try {
                InputStream in = storageAccess.getInputStreamFromUri(getActivity(),from);
                OutputStream out = storageAccess.getOutputStreamFromUri(getActivity(), testfile.getUri());

                // Transfer bytes from in to out
                storageAccess.copyFile(getActivity(),in,out);

                if (!FullscreenActivity.file_type.equals(getResources().getString(R.string.options_set))) {
                    FullscreenActivity.songfilename = FullscreenActivity.file_name;
                    FullscreenActivity.whichSongFolder = moveToFolder;
                }

                mListener.refreshAll();
                dismiss();

            } catch (Exception e) {
                Toast.makeText(getActivity(), getResources().getString(R.string.no), Toast.LENGTH_LONG).show();
            }
        }

    }

    public void setupOSTImport() {
        // OpenSongApp .ost Song file
        FullscreenActivity.file_type = getResources().getString(R.string.options_song);
        FullscreenActivity.file_name = FullscreenActivity.file_name.replace(".ost", "");
        fileTitle_EditText.setText(FullscreenActivity.file_name);
        fileType_TextView.setText(FullscreenActivity.file_type);
        messageOnSong_TextView.setVisibility(View.GONE);
        messageOpenSong_TextView.setVisibility(View.GONE);
        showSongFolders();
    }

    public void setupOSTSImport() {
        // OpenSongApp .osts Set file
        FullscreenActivity.file_type = getResources().getString(R.string.options_set);
        FullscreenActivity.file_name = FullscreenActivity.file_name.replace(".osts", "");
        // Remove the 'choose folder' views as it will be saved to the sets folder
        chooseFolder_Spinner.setVisibility(View.GONE);
        chooseFolder_TextView.setVisibility(View.GONE);
        // Change the title
        setTitle(getActivity().getResources().getString(R.string.importnewset));
        fileTitle_EditText.setText(FullscreenActivity.file_name);
        fileType_TextView.setText(FullscreenActivity.file_type);
        messageOnSong_TextView.setVisibility(View.GONE);
        messageOpenSong_TextView.setVisibility(View.GONE);
    }

    public void setupOSBImport() {
        // Copy file the the correct location
        if (FullscreenActivity.whattodo.equals("doimport")) {
            copyFile();
        }
        // Hide the views we don't need
        fileTitle_EditText.setVisibility(View.GONE);
        fileType_heading.setVisibility(View.GONE);
        fileType_TextView.setVisibility(View.GONE);
        messageOnSong_TextView.setVisibility(View.GONE);
        overWrite_CheckBox.setVisibility(View.GONE);

        // Change the views to read what we want
        setTitle(getActivity().getResources().getString(R.string.backup_import));
        itemTitle_TextView.setText(getActivity().getResources().getString(R.string.backup_import));
        chooseFolder_TextView.setText(getActivity().getResources().getString(R.string.file_chooser));
        progressbar.setVisibility(View.GONE);


        FullscreenActivity.file_type = getResources().getString(R.string.backup_info);

        // Change the views to be what we want
        if(!showOSBFiles()) {
            // No files exist in the appropriate location
            chooseFolder_TextView.setText(getActivity().getResources().getString(R.string.backup_error));
            chooseFolder_Spinner.setVisibility(View.GONE);
            if (saveMe!=null) {
                saveMe.setVisibility(View.GONE);
            }

        } else {
            // Set the OK button to import
            if (saveMe!=null) {
                saveMe.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        importOSB();
                    }
                });
            }
        }
    }

    public void setupUSRImport() {
        // SongSelect .usr file
        FullscreenActivity.file_type = "USR";
        FullscreenActivity.file_name = FullscreenActivity.file_name.replace("_", " ");
        fileTitle_EditText.setText(FullscreenActivity.file_name);
        fileType_TextView.setText(FullscreenActivity.file_type);
        messageOnSong_TextView.setVisibility(View.GONE);
        messageOpenSong_TextView.setVisibility(View.GONE);
        showSongFolders();
    }

    public void setupOSImport() {
        // Copy file the the correct location
        if (FullscreenActivity.whattodo.equals("doimport")) {
            copyFile();
        }

        // Hide the views we don't need
        fileTitle_EditText.setVisibility(View.GONE);
        fileType_heading.setVisibility(View.GONE);
        fileType_TextView.setVisibility(View.GONE);
        messageOpenSong_TextView.setVisibility(View.GONE);
        overWrite_CheckBox.setVisibility(View.GONE);

        // Change the views to read what we want
        setTitle(getActivity().getResources().getString(R.string.import_onsong_choose));
        itemTitle_TextView.setText(getActivity().getResources().getString(R.string.import_onsong_choose));
        chooseFolder_TextView.setText(getActivity().getResources().getString(R.string.file_chooser));
        if (saveMe!=null) {
            saveMe.setClickable(true);
        }
        progressbar.setVisibility(View.GONE);

        FullscreenActivity.file_type = "ONSONGARCHIVE";

        // Change the views to be what we want
        if(!showOSFiles()) {
            // No files exist in the appropriate location
            chooseFolder_TextView.setText(getActivity().getResources().getString(R.string.import_onsong_error));
            chooseFolder_Spinner.setVisibility(View.GONE);
            if (saveMe!=null) {
                saveMe.setVisibility(View.GONE);
            }
        } else {
            // Set the OK button to import
            if (saveMe!=null) {
                saveMe.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        importOS();
                    }
                });
            }
        }
    }

    public void setupBibleImport() {
        // YouVersion Bible import
        messageOnSong_TextView.setVisibility(View.GONE);
        messageOpenSong_TextView.setVisibility(View.GONE);
        String translation = FullscreenActivity.scripture_title.substring(FullscreenActivity.scripture_title.lastIndexOf(" "));
        String verses = FullscreenActivity.scripture_title.replace(translation, "");
        // Since the scripture is one big line, split it up a little (50 chars max)
        String[] scripture = FullscreenActivity.mScripture.split(" ");
        StringBuilder scriptureline = new StringBuilder();
        ArrayList<String> scripturearray = new ArrayList<>();

        for (String aScripture : scripture) {
            scriptureline.append(aScripture);
            if (scriptureline.length() > 50) {
                scripturearray.add(scriptureline.toString());
                scriptureline = new StringBuilder();
            }
        }
        scripturearray.add(scriptureline.toString());

        // Convert the array back into one string separated by new lines
        FullscreenActivity.mScripture = "";
        StringBuilder sb = new StringBuilder();
        for (int x=0;x<scripturearray.size();x++) {
            sb.append(scripturearray.get(x));
            sb.append("\n");
        }

        FullscreenActivity.mScripture = sb.toString().trim();

        String text = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<song>" +
                "  <title>"+verses+"</title>\n" +
                "  <author>"+translation+"</author>\n" +
                "  <user1></user1>\n" +
                "  <user2>false</user2>\n" +
                "  <user3></user3>\n" +
                "  <aka></aka>\n" +
                "  <key_line></key_line>\n" +
                "  <hymn_number></hymn_number>\n" +
                "  <lyrics>"+FullscreenActivity.mScripture+"</lyrics>\n" +
                "</song>";

        // Write the file
        // Create the Scriptures folder if it doesn't exist
        storageAccess.tryCreateDirectory(getActivity(), homeFolder,"Scriptures","");
        DocumentFile df = storageAccess.getFileLocationAsDocumentFile(getActivity(), homeFolder,
                "Scriptures","","YouVersion");

        try {
            OutputStream overWrite = storageAccess.getOutputStreamFromUri(getActivity(),df.getUri());
            storageAccess.writeBytes(getActivity(),overWrite,text.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Alert the user that the Scripture has been written
        FullscreenActivity.myToastMessage = getString(R.string.scripture) + " - " + getString(R.string.ok);
        ShowToast.showToast(getActivity());
        dismiss();
    }

    public void setupImageOrPDFImport() {
        // Image
        String s = getActivity().getString(R.string.image) + " / PDF";
        FullscreenActivity.file_type = getActivity().getString(R.string.image);
        fileTitle_EditText.setText(FullscreenActivity.file_name);
        fileType_TextView.setText(s);
        messageOnSong_TextView.setVisibility(View.GONE);
        messageOpenSong_TextView.setVisibility(View.GONE);
        showSongFolders();
    }
    public void setupUnknownImport() {
        // Not too sure what this file is!
        messageOnSong_TextView.setVisibility(View.GONE);
        messageOpenSong_TextView.setVisibility(View.GONE);
        if (FullscreenActivity.file_contents==null) {
            FullscreenActivity.file_contents=getResources().getString(R.string.hasnotbeenimported);
        }

        if (FullscreenActivity.file_contents.contains("<slide")) {
            // Remove the 'choose folder' views as it will be saved to the sets folder
            chooseFolder_Spinner.setVisibility(View.GONE);
            chooseFolder_TextView.setVisibility(View.GONE);
            FullscreenActivity.file_type = getResources().getString(R.string.options_set);
            // Change the title
            setTitle(getActivity().getResources().getString(R.string.importnewset));
        } else if (FullscreenActivity.file_contents.contains("<lyrics>")) {
            FullscreenActivity.file_type = getResources().getString(R.string.options_song);
        } else {
            FullscreenActivity.file_type = getResources().getString(R.string.file_type_unknown);
        }

        if (FullscreenActivity.file_contents.equals(getResources().getString(R.string.hasnotbeenimported))) {
            FullscreenActivity.myToastMessage = FullscreenActivity.file_contents;
            FullscreenActivity.file_contents = FullscreenActivity.file_type + " " + FullscreenActivity.file_contents;
            ShowToast.showToast(getActivity());
            dismiss();
        }
        fileTitle_EditText.setText(FullscreenActivity.file_name);
        fileType_TextView.setText(FullscreenActivity.file_type);
        showSongFolders();
    }

    public void schemeContent() {
        Cursor cursor = getActivity().getContentResolver().query(FullscreenActivity.file_uri, new String[]{
                MediaStore.MediaColumns.DISPLAY_NAME
        }, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            if (nameIndex >= 0 && cursor.getCount()>nameIndex) {
                FullscreenActivity.file_name = cursor.getString(nameIndex);
            } else {
                FullscreenActivity.file_name = "";
            }
            cursor.close();
        }

        if (!FullscreenActivity.file_name.endsWith(".backup") && !FullscreenActivity.file_name.endsWith(".osb")) {
        try {
            InputStream is = getActivity().getContentResolver().openInputStream(FullscreenActivity.file_uri);
            OutputStream os = storageAccess.getOutputStream(getActivity(), homeFolder,"","",
                    FullscreenActivity.file_name);
            FullscreenActivity.file_location = storageAccess.getFileLocationAsUri(getActivity(), homeFolder,
                    "", "",
                    FullscreenActivity.file_name).toString();
            storageAccess.copyFile(getActivity(),is,os);
            inputStream = storageAccess.getInputStreamFromUri(getActivity(),
                    storageAccess.getFileLocationAsUri(getActivity(), homeFolder,"", "",
                    FullscreenActivity.file_name));
            FullscreenActivity.file_contents = storageAccess.readTextFile(getActivity(),inputStream);
        } catch (Exception e) {
            // Error
            e.printStackTrace();
        }
        }
    }

    public void schemeFile() {
        try {
            //if (FullscreenActivity.file_name.endsWith(".ost") ||
            //        FullscreenActivity.file_name.endsWith(".osts"))
            if (!FullscreenActivity.file_name.endsWith(".backup") &&
                    !FullscreenActivity.file_name.endsWith(".osb") &&
                    !FullscreenActivity.file_name.endsWith(".pdf") &&
                    !FullscreenActivity.file_name.endsWith(".doc") &&
                    !FullscreenActivity.file_name.endsWith(".docx") &&
                    !FullscreenActivity.file_name.endsWith(".jpg") &&
                    !FullscreenActivity.file_name.endsWith(".png") &&
                    !FullscreenActivity.file_name.endsWith(".bmp") &&
                    !FullscreenActivity.file_name.endsWith(".gif") &&
                    !FullscreenActivity.file_name.endsWith(".zip") &&
                    !FullscreenActivity.file_name.endsWith(".sqlite")) {
                inputStream = storageAccess.getInputStreamFromUri(getActivity(),
                        Uri.parse(FullscreenActivity.file_location));
                FullscreenActivity.file_contents = storageAccess.readTextFile(getActivity(),inputStream);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void copyFile() {
        // Move the file to the correct location
        DocumentFile importIt = DocumentFile.fromSingleUri(getActivity(), Uri.parse(FullscreenActivity.file_location));
        DocumentFile newFile = storageAccess.getFileLocationAsDocumentFile(getActivity(), homeFolder,
                "","",FullscreenActivity.file_name);
        storageAccess.moveAFile(getActivity(),importIt, newFile);
    }

    public void showSongFolders() {
        //ListSongFiles listSongFiles = new ListSongFiles();
        //listSongFiles.getAllSongFolders(getActivity(), homeFolder);
        new Thread(new Runnable() {
            @Override
            public void run() {
                storageAccess.getSongFolderContents(getActivity());

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayAdapter<String> folders = new ArrayAdapter<>(getActivity(), R.layout.my_spinner, FullscreenActivity.mSongFolderNames);
                        chooseFolder_Spinner.setAdapter(folders);
                        moveToFolder = FullscreenActivity.mSongFolderNames[0];
                        chooseFolder_Spinner.setSelection(0);
                        chooseFolder_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                try {
                                    //moveToFolder = newtempfolders.get(position);
                                    moveToFolder = FullscreenActivity.mSongFolderNames[position];
                                } catch (Exception e) {
                                    // Can't find folder
                                    parent.setSelection(0);
                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {}

                        });
                    }
                });
            }
        }).run();
    }

    public boolean showOSFiles() {
        // Populate the list
        DocumentFile backupfilelocation = storageAccess.getFileLocationAsDocumentFile(getActivity(), homeFolder,
                "","","");
        DocumentFile[] backupfilecheck = backupfilelocation.listFiles();
        if (backupfilecheck != null) {
            for (DocumentFile aBackupfilecheck : backupfilecheck) {
                if (aBackupfilecheck!=null && aBackupfilecheck.isFile()
                        && aBackupfilecheck.getName().endsWith(".backup")) {
                    backups.add(aBackupfilecheck.getName());
                }
            }
        }

        if (backups.size()>0) {
            ArrayAdapter<String> files = new ArrayAdapter<>(getActivity(), R.layout.my_spinner, backups);
            chooseFolder_Spinner.setAdapter(files);
            chooseFolder_Spinner.setSelection(0);
            backupchosen = backups.get(0);
            chooseFolder_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        backupchosen = backups.get(position);
                    } catch (Exception e) {
                        // Can't find file
                        parent.setSelection(0);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}

            });
            return true;
        } else {
            return false;
        }
    }

    public boolean showOSBFiles() {
        // Populate the list
        DocumentFile backupsfolder = storageAccess.getFileLocationAsDocumentFile(getActivity(), homeFolder,
                "","","");
        DocumentFile[] backupfilecheck = backupsfolder.listFiles();
        if (backupfilecheck != null) {
            for (DocumentFile aBackupfilecheck : backupfilecheck) {
                if (aBackupfilecheck!=null && aBackupfilecheck.isFile() && aBackupfilecheck.getName().endsWith(".osb")) {
                    backups.add(aBackupfilecheck.getName());
                }
            }
        }

        if (backups.size()>0) {
            ArrayAdapter<String> files = new ArrayAdapter<>(getActivity(), R.layout.my_spinner, backups);
            chooseFolder_Spinner.setAdapter(files);
            chooseFolder_Spinner.setSelection(0);
            backupchosen = backups.get(0);
            chooseFolder_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        backupchosen = backups.get(position);
                    } catch (Exception e) {
                        // Can't find file
                        parent.setSelection(0);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}

            });
            return true;
        } else {
            return false;
        }
    }

    public void importOS() {
        // Hide the cancel button
        if (closeMe!=null) {
            closeMe.setVisibility(View.GONE);
        }
        //Change the text of the save button
        if (progressbar!=null) {
            progressbar.setVisibility(View.GONE);
        }
        if (saveMe!=null) {
            saveMe.setClickable(false);
        }

        // Now start the AsyncTask
        import_os = new ImportOnSongBackup();
        try {
            import_os.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            Log.d("d","Error importing");
        }
    }
    @SuppressLint("StaticFieldLeak")
    private class ImportOnSongBackup extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            message = getActivity().getResources().getString(R.string.import_onsong_done);
            // Check the OnSong directory exists, if not, create it
            storageAccess.tryCreateDirectory(getActivity(), homeFolder,"Songs","OnSong");

            DocumentFile dbfile = storageAccess.getFileLocationAsDocumentFile(getActivity(), homeFolder,
                    "","","OnSong.Backup.sqlite3");
            if (dbfile.exists()) {
                if (!dbfile.delete()) {
                    Log.d("d","Error deleting db file");
                }
            }

            InputStream is;
            ZipArchiveInputStream zis;
            String filename;
            try {
                is = storageAccess.getInputStream(getActivity(), homeFolder,"","",backupchosen);
                zis = new ZipArchiveInputStream(new BufferedInputStream(is),"UTF-8",false);

                ZipArchiveEntry ze;
                while ((ze = (ZipArchiveEntry) zis.getNextEntry()) != null) {
                    final byte[] buffer = new byte[2048];
                    int count;
                    filename = ze.getName();
                    Log.d("d", "filename=" + filename);
                    if (!filename.startsWith("Media")) {
                        // The Media folder throws errors (it has zero length files sometimes
                        // It also contains stuff that is irrelevant for OpenSongApp importing
                        // Only process stuff that isn't in that folder!
                        // It will also ignore any song starting with 'Media' - not worth a check for now!

                        OutputStream fout;
                        if (filename.equals("OnSong.Backup.sqlite3") || filename.equals("OnSong.sqlite3")) {
                            fout = storageAccess.getOutputStream(getActivity(), homeFolder, "", "",
                                    "OnSong.Backup.sqlite3");
                        } else {
                            fout = storageAccess.getOutputStream(getActivity(), homeFolder,"Songs",
                                    "OnSong", filename);
                        }

                        final BufferedOutputStream out = new BufferedOutputStream(fout);

                        try {
                            while ((count = zis.read(buffer)) != -1) {
                                out.write(buffer, 0, count);
                            }
                            out.flush();
                        } catch (Exception e) {
                            message = getActivity().getResources().getString(R.string.import_onsong_error);
                            e.printStackTrace();
                        } finally {
                            try {
                                out.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                zis.close();

            } catch (Exception e) {
                e.printStackTrace();
                message = getActivity().getResources().getString(R.string.import_onsong_error);
                return message;
            }

            if (dbfile.exists()) {
                SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbfile.getUri().getPath(),null);
                // Go through each row and read in the content field
                // Save the files with the .onsong extension

                String query = "SELECT * FROM Song";

                //Cursor points to a location in your results
                Cursor cursor;
                message = getActivity().getResources().getString(R.string.import_onsong_done);
                String str_title;
                String str_content;

                try {
                    cursor = db.rawQuery(query, null);

                    // Move to first row
                    cursor.moveToFirst();

                    while (cursor.moveToNext()) {
                        // Extract data.
                        str_title = cursor.getString(cursor.getColumnIndex("title"));
                        // Make sure title doesn't have /
                        str_title = str_title.replace("/", "_");
                        str_title = TextUtils.htmlEncode(str_title);
                        str_content = cursor.getString(cursor.getColumnIndex("content"));

                        try {
                            // Now write the modified song
                            OutputStream overWrite = storageAccess.getOutputStream(getActivity(), homeFolder,
                                    "Songs","OnSong",
                                    str_title + ".onsong");
                            storageAccess.writeBytes(getActivity(),overWrite,str_content.getBytes());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    cursor.close();

                } catch (Exception e) {
                    // Error with sql database
                    e.printStackTrace();
                    message = getActivity().getResources().getString(R.string.import_onsong_error);
                }
            }
            return message;
        }

        @Override
        protected void onPostExecute(String doneit) {
            try {
                if (mListener != null) {
                    mListener.onSongImportDone(doneit);
                }
                dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void importOSB() {
        // Hide the cancel button
        if (closeMe!=null) {
            closeMe.setVisibility(View.GONE);
        }
        //Change the text of the save button
        if (progressbar!=null) {
            progressbar.setVisibility(View.VISIBLE);
        }

        if (saveMe!=null) {
            saveMe.setClickable(false);
        }

        // Now start the AsyncTask
        import_osb = new Backup_Install();

        // The new fancy one!
        FullscreenActivity.whattodo = "processimportosb";
        if (mListener!=null) {
            mListener.openFragment();
        }
    }
    @SuppressLint("StaticFieldLeak")
    private class Backup_Install extends AsyncTask<String, Void, String> {

        @Override
        public void onPreExecute() {
            FullscreenActivity.myToastMessage = getActivity().getString(R.string.backup_import) +
                    "\n" + getActivity().getString(R.string.wait);
            ShowToast.showToast(getActivity());
            message = getActivity().getResources().getString(R.string.assetcopydone);
        }

        @SuppressWarnings("TryFinallyCanBeTryWithResources")
        @Override
        protected String doInBackground(String... strings) {
            ZipInputStream zis = null;
            try {
                InputStream is = storageAccess.getInputStream(getActivity(),homeFolder,"",
                        "",backupchosen);
                zis = new ZipInputStream(new BufferedInputStream(is));
                ZipEntry ze;
                int count;
                byte[] buffer = new byte[8192];
                while ((ze = zis.getNextEntry()) != null) {
                    // If this is a directory, create it if it doesn't exist
                    if (ze.isDirectory()) {
                        storageAccess.tryCreateDirectory(getActivity(), homeFolder,"Songs",ze.getName());
                    }
                    OutputStream fout = storageAccess.getOutputStream(getActivity(), homeFolder,"Songs",
                            "",ze.getName());
                    try {
                        while ((count = zis.read(buffer)) != -1)
                            fout.write(buffer, 0, count);
                    } finally {
                        try {
                            fout.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                message = getActivity().getResources().getString(R.string.backup_error);
            } finally {
                if (zis!=null) {
                    try {
                        zis.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        message = getActivity().getString(R.string.backup_error);
                    }
                }
            }
            return message;
        }

        @Override
        public void onPostExecute(String s) {
            if (mListener!=null) {
                mListener.backupInstall(s);
            }
            dismiss();
        }

    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (import_os!=null) {
            import_os.cancel(true);
        }
        if (import_osb!=null) {
            import_osb.cancel(true);
        }
        this.dismiss();
    }

}