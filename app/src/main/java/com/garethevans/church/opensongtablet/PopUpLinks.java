package com.garethevans.church.opensongtablet;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.provider.DocumentFile;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

public class PopUpLinks extends DialogFragment {

    FloatingActionButton linkYouTube_ImageButton;
    FloatingActionButton linkWeb_ImageButton;
    FloatingActionButton linkAudio_ImageButton;
    FloatingActionButton linkOther_ImageButton;
    EditText linkYouTube_EditText;
    EditText linkWeb_EditText;
    EditText linkAudio_EditText;
    EditText linkOther_EditText;
    FloatingActionButton linkYouTubeClear_ImageButton;
    FloatingActionButton linkWebClear_ImageButton;
    FloatingActionButton linkAudioClear_ImageButton;
    FloatingActionButton linkOtherClear_ImageButton;

    StorageAccess storageAccess;
    DocumentFile homeFolder;

    static PopUpLinks newInstance() {
        PopUpLinks frag;
        frag = new PopUpLinks();
        return frag;
    }

    public interface MyInterface {
        void refreshAll();
        void pageButtonAlpha(String s);
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
        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() != null && getDialog() != null) {
            PopUpSizeAndAlpha.decoratePopUp(getActivity(), getDialog());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            this.dismiss();
        }
        if (getDialog()==null) {
            dismiss();
        }
        getDialog().setCanceledOnTouchOutside(true);
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (mListener!=null) {
            mListener.pageButtonAlpha("links");
        }

        final View V = inflater.inflate(R.layout.popup_links, container, false);

        TextView title = V.findViewById(R.id.dialogtitle);
        title.setText(getActivity().getResources().getString(R.string.link));
        final FloatingActionButton closeMe = V.findViewById(R.id.closeMe);
        closeMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomAnimations.animateFAB(closeMe,getActivity());
                closeMe.setEnabled(false);
                dismiss();
            }
        });
        final FloatingActionButton saveMe = V.findViewById(R.id.saveMe);
        saveMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomAnimations.animateFAB(saveMe,getActivity());
                saveMe.setEnabled(false);
                doSave();
            }
        });

        // Initialise the views
        linkYouTube_ImageButton = V.findViewById(R.id.linkYouTube_ImageButton);
        linkWeb_ImageButton = V.findViewById(R.id.linkWeb_ImageButton);
        linkAudio_ImageButton = V.findViewById(R.id.linkAudio_ImageButton);
        linkOther_ImageButton = V.findViewById(R.id.linkOther_ImageButton);
        linkYouTube_EditText = V.findViewById(R.id.linkYouTube_EditText);
        linkWeb_EditText = V.findViewById(R.id.linkWeb_EditText);
        linkAudio_EditText = V.findViewById(R.id.linkAudio_EditText);
        linkOther_EditText = V.findViewById(R.id.linkOther_EditText);
        linkYouTubeClear_ImageButton = V.findViewById(R.id.linkYouTubeClear_ImageButton);
        linkWebClear_ImageButton = V.findViewById(R.id.linkWebClear_ImageButton);
        linkAudioClear_ImageButton = V.findViewById(R.id.linkAudioClear_ImageButton);
        linkOtherClear_ImageButton = V.findViewById(R.id.linkOtherClear_ImageButton);

        // Put any links in to the text fields
        linkYouTube_EditText.setText(FullscreenActivity.mLinkYouTube);
        linkWeb_EditText.setText(FullscreenActivity.mLinkWeb);
        linkAudio_EditText.setText(FullscreenActivity.mLinkAudio);
        linkOther_EditText.setText(FullscreenActivity.mLinkOther);

        // Set listeners to clear the fields
        linkYouTubeClear_ImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linkYouTube_EditText.setText("");
            }
        });
        linkWebClear_ImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linkWeb_EditText.setText("");
            }
        });
        linkAudioClear_ImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linkAudio_EditText.setText("");
            }
        });
        linkOtherClear_ImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linkOther_EditText.setText("");
            }
        });

        // Listen for user clicking on EditText that shouldn't really be editable
        // This is because I want a file browser/picker to fill the text in
        linkAudio_EditText.setFocusable(false);
        linkAudio_EditText.setFocusableInTouchMode(false);
        linkOther_EditText.setFocusable(false);
        linkOther_EditText.setFocusableInTouchMode(false);

        storageAccess = new StorageAccess();
        homeFolder = storageAccess.getHomeFolder(getActivity());
        DocumentFile df = null;
        String link = "";
        // Get the file (localised or not)
        if (FullscreenActivity.filechosen!=null && !FullscreenActivity.filechosen.toString().equals("")) {
            link = FullscreenActivity.filechosen.getUri().toString();
            df = storageAccess.getLocalisedFile(getActivity(), homeFolder, link);
        }

        // If a filetoselect has been set, add this to the view
        if (FullscreenActivity.filetoselect.equals("audiolink") &&
                FullscreenActivity.filechosen!=null && !FullscreenActivity.filechosen.toString().equals("")) {
            linkAudio_EditText.setText(link);
            FullscreenActivity.mLinkAudio = link;
            // If this is a genuine audio file, give the user the option of setting the song duration to match this file
            if (df!=null && df.exists() && df.getType().contains("audio")) {
                MediaPlayer mediafile = new MediaPlayer();
                try {
                    mediafile.setDataSource(getActivity(), df.getUri());
                    mediafile.prepareAsync();
                    mediafile.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            FullscreenActivity.audiolength = (int) (mp.getDuration() / 1000.0f);
                            mp.release();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    linkAudio_EditText.setText("");
                    FullscreenActivity.myToastMessage = getString(R.string.not_allowed);
                    ShowToast.showToast(getActivity());
                    mediafile.release();
                }
            }

        } else if (FullscreenActivity.filetoselect.equals("otherlink") && FullscreenActivity.filechosen!=null) {
            // Get other link
            linkOther_EditText.setText(link);
            FullscreenActivity.mLinkOther = link;
        }

        FullscreenActivity.filechosen = null;
        FullscreenActivity.filetoselect = "";

        linkAudio_EditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FullscreenActivity.filetoselect = "audiolink";
                FullscreenActivity.whattodo = "filechooser";
                mListener.openFragment();
                dismiss();
            }
        });
        linkOther_EditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FullscreenActivity.filetoselect = "otherlink";
                FullscreenActivity.whattodo = "filechooser";
                mListener.openFragment();
                dismiss();
            }
        });

        // Set up button actions
        linkYouTube_ImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(linkYouTube_EditText.getText().toString())));
                } catch (Exception e) {
                    e.printStackTrace();
                    FullscreenActivity.myToastMessage = getResources().getString(R.string.error_notset);
                    ShowToast.showToast(getActivity());
                }
            }
        });
        linkWeb_ImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(linkWeb_EditText.getText().toString())));
                } catch (Exception e) {
                    e.printStackTrace();
                    FullscreenActivity.myToastMessage = getResources().getString(R.string.error_notset);
                    ShowToast.showToast(getActivity());
                }
            }
        });
        linkAudio_ImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mytext = linkAudio_EditText.getText().toString();
                if (!mytext.equals("")) {
                    StorageAccess sa = new StorageAccess();
                    DocumentFile getfile = sa.getLocalisedFile(getActivity(),homeFolder, mytext);
                    String myMime = getfile.getType();
                    Intent newIntent = new Intent(Intent.ACTION_VIEW);
                    newIntent.setDataAndType(getfile.getUri(), myMime);
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(newIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    FullscreenActivity.myToastMessage = getResources().getString(R.string.error_notset);
                    ShowToast.showToast(getActivity());
                }
            }
        });
        linkOther_ImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mytext = linkOther_EditText.getText().toString();
                if (!mytext.equals("")) {
                    StorageAccess sa = new StorageAccess();
                    DocumentFile getfile = sa.getLocalisedFile(getActivity(), homeFolder, mytext);
                    String myMime = getfile.getType();
                    Intent newIntent = new Intent(Intent.ACTION_VIEW);

                    newIntent.setDataAndType(getfile.getUri(), myMime);
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(newIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    FullscreenActivity.myToastMessage = getResources().getString(R.string.error_notset);
                    ShowToast.showToast(getActivity());
                }
            }
        });

        PopUpSizeAndAlpha.decoratePopUp(getActivity(),getDialog());

        return V;
    }

    public void doSave() {
        // Get the values from the page
        FullscreenActivity.mLinkYouTube = linkYouTube_EditText.getText().toString();
        FullscreenActivity.mLinkWeb = linkWeb_EditText.getText().toString();
        FullscreenActivity.mLinkAudio = linkAudio_EditText.getText().toString();
        FullscreenActivity.mLinkOther = linkOther_EditText.getText().toString();

        // Now resave the song with these new links
        PopUpEditSongFragment.prepareSongXML();
        try {
            PopUpEditSongFragment.justSaveSongXML(getActivity());
            mListener.refreshAll();
            dismiss();
        } catch (Exception e) {
            FullscreenActivity.myToastMessage = getActivity().getResources().getString(R.string.savesong) + " - " +
                    getActivity().getResources().getString(R.string.error);
            ShowToast.showToast(getActivity());
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        if (intent!=null) {
            Uri uri = intent.getData();
            if (requestCode==0) {
                // Audio
                if (uri!=null) {
                    linkAudio_EditText.setText(uri.toString());
                }
            } else if (requestCode==1) {
                // Document
                if (uri!=null) {
                    linkOther_EditText.setText(uri.toString());
                }
            }
        }
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        if (mListener!=null) {
            mListener.pageButtonAlpha("");
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        this.dismiss();
    }

}