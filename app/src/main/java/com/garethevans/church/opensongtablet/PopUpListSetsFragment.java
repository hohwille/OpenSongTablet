package com.garethevans.church.opensongtablet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class PopUpListSetsFragment extends DialogFragment {

    static PopUpListSetsFragment newInstance() {
        PopUpListSetsFragment frag;
        frag = new PopUpListSetsFragment();
        return frag;
    }

    View V;
    TextView title, setCategory_TextView;
    EditText setListName, newCategory_EditText;
    TextView newSetPromptTitle;
    Spinner setCategory_Spinner, oldCategory_Spinner;
    ImageButton newCategory_ImageButton, sort_ImageButton;
    RelativeLayout setCategory, filelist_RelativeLayout;
    LinearLayout oldCategory_LinearLayout, newCategory_LinearLayout, newSetTitle_LinearLayout;
    CheckBox overWrite_CheckBox;
    String myTitle, val;
    static LoadSetDataTask dataTask;
    Runnable runnable;
    String[] setnames, filteredsetnames;
    ArrayAdapter<String> adapter, categoryadapter;
    ListView setListView1;
    ArrayList<String> catsFound = new ArrayList<>(), setsFound = new ArrayList<>();
    ProgressBar progressBar;

    AsyncTask<Void, Void, Void> updatesets_async;
    AsyncTask<Void, Void, Void> setcategories_async;

    SetActions setActions;
    StorageAccess storageAccess;
    DocumentFile homeFolder;

    public interface MyInterface {
        void refreshAll();
        void openFragment();
        void confirmedAction();
    }
    private static MyInterface mListener;


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
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        try {
            dataTask.cancel(true);
        } catch (Exception e) {
            // Don't worry
        }

        try {
            dataTask = null;
        } catch (Exception e) {
            // Don't worry
        }
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
        // Call the view
        V = inflater.inflate(R.layout.popup_setlists, container, false);

        // Initialise the helper classes
        setActions = new SetActions();
        storageAccess = new StorageAccess();
        homeFolder = storageAccess.getHomeFolder(getActivity());

        // Set up the Title bar
        setUpTitleBar();

        // Declare the main views
        declareViews();

        // Match the view to the one we need (Load, Save, Delete, Export, Manage)
        fixViewsNeeded();

        // List the available sets and update the set list on screen
        listAvailableSets();

        // Show the available set categories
        prepareSetCategories();

        // Reset the setname chosen
        FullscreenActivity.setnamechosen = "";
        FullscreenActivity.abort = false;

        // Set the listeners for the page
        setListenersForPage();










        PopUpSizeAndAlpha.decoratePopUp(getActivity(),getDialog());

        return V;
    }


    void setUpTitleBar() {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(true);

        myTitle = getActivity().getResources().getString(R.string.options_set);

        switch (FullscreenActivity.whattodo) {
            default:
            case "loadset":
                myTitle = myTitle + " - " + getActivity().getResources().getString(R.string.options_set_load);
                break;

            case "saveset":
                myTitle = myTitle + " - " + getActivity().getResources().getString(R.string.options_set_save);
                break;

            case "deleteset":
                myTitle = myTitle + " - " + getActivity().getResources().getString(R.string.options_set_delete);
                break;

            case "exportset":
                myTitle = myTitle + " - " + getActivity().getResources().getString(R.string.options_set_export);
                break;

            case "managesets":
                myTitle = myTitle + " - " + getActivity().getResources().getString(R.string.managesets);
                break;

        }


        title = V.findViewById(R.id.dialogtitle);
        title.setText(myTitle);
        final FloatingActionButton closeMe = V.findViewById(R.id.closeMe);
        closeMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomAnimations.animateFAB(closeMe,getActivity());
                closeMe.setEnabled(false);
                FullscreenActivity.myToastMessage = "";
                dismiss();
            }
        });
        final FloatingActionButton saveMe = V.findViewById(R.id.saveMe);
        saveMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomAnimations.animateFAB(saveMe,getActivity());
                //saveMe.setEnabled(false);
                doAction();
            }
        });
    }
    void declareViews() {
        progressBar = V.findViewById(R.id.progressBar);
        setListView1 = V.findViewById(R.id.setListView1);
        setListName = V.findViewById(R.id.setListName);
        newSetPromptTitle = V.findViewById(R.id.newSetPromptTitle);
        oldCategory_Spinner = V.findViewById(R.id.oldCategory_Spinner);
        setCategory_Spinner = V.findViewById(R.id.setCategory_Spinner);
        newCategory_ImageButton = V.findViewById(R.id.newCategory_ImageButton);
        newCategory_EditText = V.findViewById(R.id.newCategory_EditText);
        setCategory_TextView = V.findViewById(R.id.setCategory_TextView);
        setCategory = V.findViewById(R.id.setCategory);
        filelist_RelativeLayout = V.findViewById(R.id.filelist_RelativeLayout);
        oldCategory_LinearLayout = V.findViewById(R.id.oldCategory_LinearLayout);
        newCategory_LinearLayout = V.findViewById(R.id.newCategory_LinearLayout);
        newSetTitle_LinearLayout = V.findViewById(R.id.newSetTitle_LinearLayout);
        overWrite_CheckBox = V.findViewById(R.id.overWrite_CheckBox);
        setListName.setText(FullscreenActivity.lastSetName);
        sort_ImageButton = V.findViewById(R.id.sort_ImageButton);
    }
    void fixViewsNeeded() {
        switch (FullscreenActivity.whattodo) {
            default:
            case "loadset":
                filelist_RelativeLayout.setVisibility(View.VISIBLE);
                oldCategory_LinearLayout.setVisibility(View.GONE);
                newCategory_LinearLayout.setVisibility(View.VISIBLE);
                newSetTitle_LinearLayout.setVisibility(View.GONE);
                newCategory_ImageButton.setVisibility(View.GONE);
                newCategory_EditText.setVisibility(View.GONE);
                overWrite_CheckBox.setVisibility(View.VISIBLE);
                break;

            case "saveset":
                filelist_RelativeLayout.setVisibility(View.VISIBLE);
                oldCategory_LinearLayout.setVisibility(View.GONE);
                newCategory_LinearLayout.setVisibility(View.VISIBLE);
                newSetTitle_LinearLayout.setVisibility(View.VISIBLE);
                newCategory_EditText.setVisibility(View.GONE);
                overWrite_CheckBox.setVisibility(View.GONE);
                break;

            case "deleteset":
                filelist_RelativeLayout.setVisibility(View.VISIBLE);
                oldCategory_LinearLayout.setVisibility(View.GONE);
                newCategory_LinearLayout.setVisibility(View.VISIBLE);
                newSetTitle_LinearLayout.setVisibility(View.GONE);
                setListName.setVisibility(View.GONE);
                newSetPromptTitle.setVisibility(View.GONE);
                newCategory_ImageButton.setVisibility(View.GONE);
                newCategory_EditText.setVisibility(View.GONE);
                overWrite_CheckBox.setVisibility(View.GONE);
                break;

            case "exportset":
                oldCategory_LinearLayout.setVisibility(View.GONE);
                newSetTitle_LinearLayout.setVisibility(View.GONE);
                setListName.setVisibility(View.GONE);
                newSetPromptTitle.setVisibility(View.GONE);
                newCategory_ImageButton.setVisibility(View.GONE);
                newCategory_EditText.setVisibility(View.GONE);
                overWrite_CheckBox.setVisibility(View.GONE);
                break;

            case "managesets":
                setListView1.setVisibility(View.VISIBLE);
                setCategory_TextView.setText(getActivity().getString(R.string.new_category));
                setCategory.setVisibility(View.VISIBLE);
                newSetPromptTitle.setVisibility(View.VISIBLE);
                setListName.setVisibility(View.VISIBLE);
                newCategory_LinearLayout.setVisibility(View.VISIBLE);
                newSetTitle_LinearLayout.setVisibility(View.VISIBLE);
                newCategory_EditText.setVisibility(View.GONE);
                overWrite_CheckBox.setVisibility(View.VISIBLE);
                break;
        }
    }
    void setListenersForPage() {
        if (FullscreenActivity.whattodo.equals("managesets")) {
            oldCategory_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    setCategory_Spinner.setSelection(i);
                    FullscreenActivity.whichSetCategory = catsFound.get(i);
                    listAvailableSets();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {}
            });
        }
        newCategory_ImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Change button function and image
                changeCategoryButton(true);
            }
        });

        sort_ImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Sorting is done in setActions.listFoundSetsInCategory
                FullscreenActivity.sortAlphabetically = !FullscreenActivity.sortAlphabetically;
                listAvailableSets();
            }
        });
        setListView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the name of the set to do stuff with
                // Since we can select multiple sets, check it isn't already in the setnamechosen field

                // Get the set name
                String msetname = filteredsetnames[position];
                // If we have a category selected, add this to the file name

                if (!FullscreenActivity.whattodo.equals("managesets") && setCategory_Spinner.getSelectedItemPosition()>0) {
                    msetname = catsFound.get(setCategory_Spinner.getSelectedItemPosition()) + "__" + msetname;
                }

                if (FullscreenActivity.whattodo.equals("managesets") && oldCategory_Spinner.getSelectedItemPosition()>0) {
                    msetname = catsFound.get(oldCategory_Spinner.getSelectedItemPosition()) + "__" + msetname;
                }

                if (FullscreenActivity.whattodo.equals("exportset")) {
                    FullscreenActivity.setnamechosen = msetname + "%_%";
                } else {
                    if (!FullscreenActivity.setnamechosen.contains(msetname)) {
                        // Add it to the setnamechosen
                        FullscreenActivity.setnamechosen = FullscreenActivity.setnamechosen + msetname + "%_%";
                    } else {
                        // Remove it from the setnamechosen
                        FullscreenActivity.setnamechosen = FullscreenActivity.setnamechosen.replace(msetname + "%_%", "");
                    }
                }
                setListName.setText(filteredsetnames[position]);
            }
        });
    }


    void listAvailableSets() {
        // Get a note of the available sets - Run in a separate async
        if (updatesets_async!=null) {
            updatesets_async.cancel(true);
            updatesets_async = null;
        }
        updatesets_async = new UpdateSets(getActivity(), homeFolder);
        updatesets_async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    @SuppressLint("StaticFieldLeak")
    private class UpdateSets extends AsyncTask<Void,Void,Void> {
        Context c;
        DocumentFile homeFolder;

        UpdateSets(Context ctx, DocumentFile home) {
            c = ctx;
            homeFolder = home;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // List the set files
            setsFound = setActions.listFoundSetsInCategory(c, homeFolder, FullscreenActivity.whichSetCategory);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Now we have the set lists found stored in a variable, update the view
            // This method also ticks the appropriate ones
            setCorrectAdapter();
        }
    }
    void prepareSetCategories(){
        // Do this as an asynctask
        if (setcategories_async!=null) {
            setcategories_async.cancel(true);
            setcategories_async = null;
        }
        setcategories_async = new SetCategories(getActivity(), homeFolder);
        setcategories_async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    @SuppressLint("StaticFieldLeak")
    private class SetCategories extends AsyncTask<Void, Void, Void> {
        int lastcategory;
        Context c;
        DocumentFile homeFolder;

        SetCategories(Context ctx, DocumentFile home) {
            c = ctx;
            homeFolder = home;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Get the category filter
            catsFound = setActions.listCategoriesOfFoundSets(c, homeFolder);

            // Find the last used one
            lastcategory = lastUsedCategory();

            return null;
        }

        @Override
        protected void onPostExecute(Void v){
            // Update the set category spinners
            setCategoryAdapters(lastcategory);
        }
    }
    int lastUsedCategory() {
        if (catsFound!=null) {
            // Try to set the spinners to match the recently used set category
            for (int i = 0; i < catsFound.size(); i++) {
                if (catsFound.get(i).equals(FullscreenActivity.whichSetCategory)) {
                    return i;
                }
            }
            return 0;
        } else {
            return -1;
        }
    }
    void setCorrectAdapter() {
        switch (FullscreenActivity.whattodo) {
            default:
                adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_multiple_choice, setsFound);
                setListView1.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                break;

            case "saveset":
                adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, setsFound);
                setListView1.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                break;

            case "deleteset":
                adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_multiple_choice, setsFound);
                setListView1.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                break;

            case "exportset":
                adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_checked, setsFound);
                setListView1.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                break;

            case "managesets":
                adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_checked, setsFound);
                setListView1.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                break;

        }
        adapter.notifyDataSetChanged();
        setListView1.setAdapter(adapter);
        tickSelectedSetsInCategory();
    }
    void setCategoryAdapters(int i) {
        categoryadapter = new ArrayAdapter<>(getActivity(), R.layout.my_spinner, catsFound);
        categoryadapter.setDropDownViewResource(R.layout.my_spinner);
        oldCategory_Spinner.setAdapter(categoryadapter);
        setCategory_Spinner.setAdapter(categoryadapter);
        oldCategory_Spinner.setSelection(i);
        setCategory_Spinner.setSelection(i);

        // Set the category listener if we are managing sets
        if (!FullscreenActivity.whattodo.equals("managesets")) {
            setCategory_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    FullscreenActivity.whichSetCategory = catsFound.get(i);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
        }
    }

    void tickSelectedSetsInCategory() {
        String filter = FullscreenActivity.whichSetCategory;
        if (filter.equals(FullscreenActivity.mainfoldername)) {
            filter="";
        } else {
            filter=filter+"__";
        }

        for (int f=0;f<setsFound.size();f++) {
            if (FullscreenActivity.setnamechosen.contains(filter+setsFound.get(f))) {
                setListView1.setItemChecked(f,true);
            } else {
                setListView1.setItemChecked(f,false);
            }
        }
    }






/*    ArrayAdapter<String> setCategories() {
        // Go through the available sets and only show those matching the filter
        // Set categories are identified by mycategory__setname
        // Those with no category are in the main category
        cats = new ArrayList<>();
        StringBuilder log = new StringBuilder();

        // There might be no sets!
        for (int w=0;w<FullscreenActivity.mySetsFileNames.length;w++) {
            if (FullscreenActivity.mySetsFileNames[w].contains("__")) {
                // Split it into category and set name;
                String[] msplit = FullscreenActivity.mySetsFileNames[w].split("__");
                if (!log.toString().contains(msplit[0])) {
                    log.append(msplit[0]).append(",");
                }
            }
        }
        // Now split the log into available categories
        String[] categoriesfound = log.toString().split(",");
        for (String s:categoriesfound) {
            if (!s.equals("")) {
                cats.add(s);
            }
        }



        ArrayAdapter<String> myadapter = new ArrayAdapter<>(getActivity(),R.layout.my_spinner,cats);
        myadapter.setDropDownViewResource(R.layout.my_spinner);

        return myadapter;
    }*/








    private static class LoadSetDataTask extends AsyncTask<String,Integer,String> {

        private WeakReference<Application> c;
        private DocumentFile homeFolder;
        @SuppressLint("StaticFieldLeak")
        private SetActions setActions;
        private StringBuilder allsongsinset;

        LoadSetDataTask(Application context, DocumentFile home) {
            c = new WeakReference<>(context);
            homeFolder = home;
        }
        @Override
        public void onPreExecute() {

            // Check the directories and clear them of prior content
            setActions = new SetActions();
            setActions.checkDirectories(c.get(), homeFolder);
            allsongsinset = new StringBuilder();
        }

        @Override
        protected String doInBackground(String... args) {
            // Now users can load multiple sets and merge them, we need to load each one it turn
            // We then add the items to a temp string 'allsongsinset'
            // Once we have loaded them all, we replace the mySet field.
            Looper.prepare();

            // Split the string by "%_%" - last item will be empty as each set added ends with this
            String[] tempsets = FullscreenActivity.setnamechosen.split("%_%");

            for (String tempfile:tempsets) {
                if (tempfile!=null && !tempfile.equals("") && !tempfile.isEmpty()) {
                    try {
                        FullscreenActivity.settoload = tempfile;
                        setActions.loadASet(c.get(),homeFolder);
                    } catch (XmlPullParserException | IOException e) {
                        e.printStackTrace();
                    }
                    allsongsinset.append(FullscreenActivity.mySet);
                }
            }

            // Add all the songs of combined sets back to the mySet
            FullscreenActivity.mySet = allsongsinset.toString();

            // Reset the options menu
            setActions.prepareSetList();
            setActions.indexSongInSet();

            return "LOADED";
        }

        @Override
        protected void onCancelled(String result) {
            Log.d("dataTask","onCancelled");
        }

        @Override
        protected void onPostExecute(String result) {
            FullscreenActivity.setView = true;
            SetActions setActions = new SetActions();

            if (result.equals("LOADED") && !this.isCancelled()) {
                // Get the set first item
                setActions.prepareFirstItem(c.get(), homeFolder);

                // Save the new set to the preferences
                Preferences.savePreferences();

                // Tell the listener to do something
                mListener.refreshAll();
                FullscreenActivity.whattodo = "editset";
                mListener.openFragment();
                FullscreenActivity.abort = false;
            }
        }
    }

    public void doAction() {
        if (FullscreenActivity.setnamechosen.endsWith("%_%")) {
            FullscreenActivity.setnamechosen = FullscreenActivity.setnamechosen.substring(0,FullscreenActivity.setnamechosen.length()-3);
        }

        if (FullscreenActivity.whattodo.equals("loadset") && !FullscreenActivity.setnamechosen.isEmpty() && !FullscreenActivity.setnamechosen.equals("")) {
            doLoadSet();
        } else if (FullscreenActivity.whattodo.equals("saveset") && !setListName.getText().toString().trim().isEmpty() && !setListName.getText().toString().trim().equals("")) {
            doSaveSet();
        } else if (FullscreenActivity.whattodo.equals("deleteset") && !FullscreenActivity.setnamechosen.isEmpty() && !FullscreenActivity.setnamechosen.equals("")) {
            doDeleteSet();
        } else if (FullscreenActivity.whattodo.equals("exportset") && !FullscreenActivity.setnamechosen.isEmpty() && !FullscreenActivity.setnamechosen.equals("")) {
            FullscreenActivity.settoload = FullscreenActivity.setnamechosen;
            doExportSet();
        } else if (FullscreenActivity.whattodo.equals("managesets")) {
            if (!FullscreenActivity.setnamechosen.equals("") && !setListName.getText().toString().equals("")) {
                doRenameSet();
            } else {
                FullscreenActivity.myToastMessage = getActivity().getString(R.string.error_notset);
            }
        }
    }

/*
    public void whichSetCategory() {
        // Try to set the spinners to match the recently used set category
        boolean done = false;
        for (int i=0;i<cats.size();i++) {
            if (cats.get(i).equals(FullscreenActivity.whichSetCategory)) {
                oldCategory_Spinner.setSelection(i);
                setCategory_Spinner.setSelection(i);
                done = true;
            }
        }
        if (!done) {
            // Can't find the set category, so default to the MAIN one (position 0)
            oldCategory_Spinner.setSelection(0);
            setCategory_Spinner.setSelection(0);
        }
    }
*/

    public void changeCategoryButton(boolean makedelete) {
        if (makedelete) {
            setCategory_Spinner.setVisibility(View.GONE);
            newCategory_EditText.setVisibility(View.VISIBLE);
            newCategory_ImageButton.setImageResource(R.drawable.ic_delete_white_36dp);
            newCategory_ImageButton.setVisibility(View.VISIBLE);
            newCategory_ImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    changeCategoryButton(false);
                }
            });
        } else {
            setCategory_Spinner.setVisibility(View.VISIBLE);
            newCategory_EditText.setVisibility(View.GONE);
            newCategory_EditText.setText("");
            newCategory_ImageButton.setImageResource(R.drawable.ic_plus_white_36dp);
            newCategory_ImageButton.setVisibility(View.VISIBLE);
            newCategory_ImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    changeCategoryButton(true);
                }
            });
        }
    }


/*
    public void filterByCategory(int i) {
        // Get the text to filter the sets by
        String filter = cats.get(i);
        ArrayList<String> filtered = new ArrayList<>();

        // Go through the setnames list and only show the available ones
        for (String setname : setnames) {
            if (setname != null && setname.contains(filter + "__")) {
                String addthis = setname.replace(filter + "__", "");
                filtered.add(addthis);
            } else if (filter.equals(FullscreenActivity.mainfoldername)) {
                if (setname != null && !setname.contains("__")) {
                    filtered.add(setname);
                }
            }
        }

        // Sort the categories alphabetically using locale
        Collator coll = Collator.getInstance(FullscreenActivity.locale);
        coll.setStrength(Collator.SECONDARY);
        Collections.sort(filtered, coll);

        filteredsetnames = new String[filtered.size()];

        filteredsetnames = filtered.toArray(filteredsetnames);

        setCorrectAdapter(filteredsetnames);

        // Go through new list and re tick any currently selected ones
        if (FullscreenActivity.whattodo.equals("loadset")) {
            tickSelectedSetsInCategory(filter);
        }
    }
*/



    public void doLoadSet() {
        // Load the set up
        FullscreenActivity.settoload = null;
        FullscreenActivity.abort = false;

        FullscreenActivity.settoload = FullscreenActivity.setnamechosen;
        FullscreenActivity.lastSetName = setListName.getText().toString();

        // Display the progress
        progressBar.setVisibility(View.VISIBLE);

        dataTask = null;
        dataTask = new LoadSetDataTask(getActivity().getApplication(),homeFolder);
        try {
            dataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("d","Error getting data");
        }
    }
    public void doSaveSet() {
        // Save the set into the settoload name
        FullscreenActivity.settoload = setListName.getText().toString().trim();
        FullscreenActivity.lastSetName = setListName.getText().toString().trim();
        String new_cat = newCategory_EditText.getText().toString();

        if (!new_cat.equals("")) {
            FullscreenActivity.settoload = new_cat + "__" + setListName.getText().toString().trim();
        } else if (setCategory_Spinner.getSelectedItemPosition()>0) {
            FullscreenActivity.settoload = catsFound.get(setCategory_Spinner.getSelectedItemPosition()) +
                    "__" + setListName.getText().toString().trim();
        }

        // Popup the are you sure alert into another dialog fragment
        DocumentFile newsetname = storageAccess.getFileLocationAsDocumentFile(getActivity(), homeFolder, "Sets","",FullscreenActivity.settoload);

        // New structure, only give the are you sure prompt if the set name already exists.
        if (newsetname.exists()) {
            String message = getResources().getString(R.string.options_set_save) + " \'" + setListName.getText().toString().trim() + "\"?";
            FullscreenActivity.myToastMessage = message;
            DialogFragment newFragment = PopUpAreYouSureFragment.newInstance(message);
            newFragment.show(getFragmentManager(), "dialog");
            dismiss();
        } else {
            if (mListener!=null) {
                FullscreenActivity.whattodo = "saveset";
                mListener.confirmedAction();
            }
            try {
                dismiss();
            } catch (Exception e) {
                Log.d("d","Error closing");
            }
        }


        /*if (newsetname.exists() && !overWrite_CheckBox.isChecked()) {
            FullscreenActivity.myToastMessage = getActivity().getString(R.string.renametitle) + " - " +
                    getActivity().getString(R.string.file_exists);
            ShowToast.showToast(getActivity());
        } else {
            String message = getResources().getString(R.string.options_set_save) + " \'" + setListName.getText().toString().trim() + "\"?";
            FullscreenActivity.myToastMessage = message;
            DialogFragment newFragment = PopUpAreYouSureFragment.newInstance(message);
            newFragment.show(getFragmentManager(), "dialog");
            dismiss();
        }*/
        // If the user clicks on the areyousureYesButton, then action is confirmed as ConfirmedAction
    }
    public void doDeleteSet() {
        // Load the set up
        FullscreenActivity.settoload = null;
        FullscreenActivity.settoload = FullscreenActivity.setnamechosen;

        // Popup the are you sure alert into another dialog fragment
        // Get the list of set lists to be deleted
        String setstodelete = FullscreenActivity.setnamechosen.replace("%_%",", ");
        if (setstodelete.endsWith(", ")) {
            setstodelete = setstodelete.substring(0, setstodelete.length() - 2);
        }

        String message = getResources().getString(R.string.options_set_delete) + " \"" + setstodelete + "\"?";
        FullscreenActivity.myToastMessage = message;
        DialogFragment newFragment = PopUpAreYouSureFragment.newInstance(message);
        newFragment.show(getFragmentManager(), "dialog");
        dismiss();
        // If the user clicks on the areyousureYesButton, then action is confirmed as ConfirmedAction

    }
    public void doExportSet() {
        if (mListener!=null) {
            FullscreenActivity.whattodo = "customise_exportset";
            mListener.openFragment();
            dismiss();
        }
    }







    /*private class LoadSetDataTask extends AsyncTask<String,Integer,String> {
        DocumentFile homeFolder;

        private WeakReference<LoadSetDataTask> appReference;

        LoadSetDataTask(Application context) {
            appReference = new WeakReference<>(context);
        }

        @SuppressLint("StaticFieldLeak")
        Context c;
        LoadSetDataTask (Context ctx, DocumentFile home) {
            homeFolder = home;
            c = ctx;
        }
        @SuppressLint("StaticFieldLeak")
        SetActions setActions;
        @Override
        public void onPreExecute() {
            // Check the directories and clear them of prior content
            setActions = new SetActions();
            setActions.checkDirectories(c, homeFolder);
        }

        @Override
        protected String doInBackground(String... args) {
            // Now users can load multiple sets and merge them, we need to load each one it turn
            // We then add the items to a temp string 'allsongsinset'
            // Once we have loaded them all, we replace the mySet field.

            StringBuilder allsongsinset = new StringBuilder();

            // Split the string by "%_%" - last item will be empty as each set added ends with this
            String[] tempsets = FullscreenActivity.setnamechosen.split("%_%");

            for (String tempfile:tempsets) {
                if (tempfile!=null && !tempfile.equals("") && !tempfile.isEmpty()) {
                    try {
                        FullscreenActivity.settoload = tempfile;
                        setActions.loadASet(c,homeFolder);
                    } catch (XmlPullParserException | IOException e) {
                        e.printStackTrace();
                    }
                    allsongsinset.append(FullscreenActivity.mySet);
                }
            }

            // Add all the songs of combined sets back to the mySet
            FullscreenActivity.mySet = allsongsinset.toString();

            // Reset the options menu
            setActions.prepareSetList();
            setActions.indexSongInSet();

            return "LOADED";
        }

        @Override
        protected void onCancelled(String result) {
            Log.d("dataTask","onCancelled");
        }

        @Override
        protected void onPostExecute(String result) {
            FullscreenActivity.setView = true;

            if (result.equals("LOADED") && !dataTask.isCancelled()) {
                // Get the set first item
                setActions.prepareFirstItem(c, homeFolder);

                // Save the new set to the preferences
                Preferences.savePreferences();

                // Tell the listener to do something
                mListener.refreshAll();
                FullscreenActivity.whattodo = "editset";
                mListener.openFragment();
                FullscreenActivity.abort = false;
                //Close this dialog
            }
            prog.dismiss();
        }
    }*/





 /*   public void sortFilteredSetLists() {
        // Change our sorting preference
        if (!FullscreenActivity.sortAlphabetically) {
            Collections.sort(setsFound);
            Collections.reverse(setsFound);
        } else {
            Collections.sort(setnames_ar);
        }

        // Sort the filtered set lists either alphabetically or reverse alphabetically
        ArrayList<String> setnames_ar = new ArrayList<>(Arrays.asList(filteredsetnames));



        setnames = new String[setnames_ar.size()];
        filteredsetnames = new String[setnames_ar.size()];
        setnames = setnames_ar.toArray(setnames);
        filteredsetnames = setnames_ar.toArray(filteredsetnames);

        if (adapter!=null) {
            setCorrectAdapter(filteredsetnames);
        }

        // Need to recheck any ones that were checked before.
        for (int i=0;i<filteredsetnames.length;i++) {
            if (FullscreenActivity.setnamechosen.contains(filteredsetnames[i])) {
                setListView1.setItemChecked(i,true);
            } else {
                setListView1.setItemChecked(i,false);
            }
        }
    }*/

    void doRenameSet() {
        /*// Get the values from the page
        String newcat_edittext = newCategory_EditText.getText().toString();
        String newcat_spinner = catsFound.get(setCategory_Spinner.getSelectedItemPosition());

        String newsettitle = setListName.getText().toString();

        String newsetname;
        if (!newcat_edittext.equals("")) {
            newsetname = newcat_edittext + "__" + newsettitle;
        } else {
            if (newcat_spinner.equals(FullscreenActivity.mainfoldername)) {
                newsetname = newsettitle;
            } else {
                newsetname = newcat_spinner + "__" + newsettitle;
            }
        }

        DocumentFile newsetfile = storageAccess.getFileLocationAsDocumentFile(getActivity(),homeFolder,
                "Sets","", newsetname);
        boolean success;

        // Check the new song doesn't exist already
        if (newsetfile.exists() && !overWrite_CheckBox.isChecked()) {
            success = false;
        } else {
            success = storageAccess.renameAFile(getActivity(),homeFolder,"Sets","",
                    FullscreenActivity.setnamechosen,newsetname);
            if (!success) {
                Log.d("d","error renaming");
            }
        }

        if (success) {
            FullscreenActivity.myToastMessage = getActivity().getString(R.string.renametitle) + " - " +
                    getActivity().getString(R.string.success);
        } else {
            FullscreenActivity.myToastMessage = getActivity().getString(R.string.renametitle) + " - " +
                    getActivity().getString(R.string.file_exists);
        }
        ShowToast.showToast(getActivity());

        setListName.setText("");
        FullscreenActivity.setnamechosen="";

        // Refresh the category spinners
        setActions.updateOptionListSets(getActivity(), homeFolder);
        sortSetLists();
        setCategory_Spinner.setAdapter(setCategories());
        oldCategory_Spinner.setAdapter(setCategories());
        setCorrectAdapter(setnames);
        whichSetCategory();
*/
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (dataTask!=null) {
            dataTask.cancel(true);
        }
        this.dismiss();
    }

}