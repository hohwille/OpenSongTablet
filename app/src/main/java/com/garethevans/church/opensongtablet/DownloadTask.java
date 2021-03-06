package com.garethevans.church.opensongtablet;

// This class is used to download my versions of the Church and Band songs as .osb files

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class DownloadTask extends AsyncTask<String, Integer, String> {

    public interface MyInterface {
        void openFragment();
    }

    private MyInterface mListener;
    private String address;
    private String filename;
    StorageAccess storageAccess;
    Preferences preferences;
    @SuppressLint("StaticFieldLeak")
    private Context c;
    private Uri uri;

        DownloadTask(Context context, String address) {
            this.address = address;
            c = context;
            mListener = (MyInterface) context;
            switch (FullscreenActivity.whattodo) {
                case "download_band":
                    filename = "Band.osb";
                    break;
                case "download_church":
                    filename = "Church.osb";
                    break;
                default:
                    filename = "Download.osb";
                    break;
            }
            storageAccess = new StorageAccess();
            preferences = new Preferences();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream outputStream = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(address);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                Log.d("d", "address=" + address);
                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                Log.d("d", "fileLength=" + fileLength);

                // download the file
                input = connection.getInputStream();
                Log.d("d", "input=" + input);

                if (input!=null) {

                    uri = storageAccess.getUriForItem(c, preferences, "", "", filename);

                    // Check the uri exists for the outputstream to be valid
                    storageAccess.lollipopCreateFileForOutputStream(c, preferences, uri, null, "", "", filename);

                    outputStream = storageAccess.getOutputStream(c, uri);

                    byte data[] = new byte[4096];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        // allow canceling with back button
                        if (isCancelled()) {
                            input.close();
                            return null;
                        }
                        total += count;
                        // publishing the progress....
                        if (fileLength > 0) // only if total length is known
                            publishProgress((int) (total * 100 / fileLength));
                        outputStream.write(data, 0, count);
                    }
                } else {
                    FullscreenActivity.myToastMessage = c.getResources().getString(R.string.network_error);
                }
            } catch (Exception e) {
                FullscreenActivity.myToastMessage = c.getResources().getString(R.string.network_error);
                e.printStackTrace();
            } finally {
                try {
                    if (outputStream != null)
                        outputStream.close();
                    if (input != null)
                        input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (FullscreenActivity.myToastMessage.equals(c.getResources().getString(R.string.network_error))) {
                ShowToast.showToast(c);
            } else {
                FullscreenActivity.whattodo = "processimportosb";
                FullscreenActivity.file_uri = uri;
                if (mListener != null) {
                    mListener.openFragment();
                }
            }
        }
}
