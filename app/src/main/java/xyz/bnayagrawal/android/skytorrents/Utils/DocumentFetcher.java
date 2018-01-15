package xyz.bnayagrawal.android.skytorrents.Utils;

import android.os.AsyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;

/**
 * Created by binay on 15/1/18.
 */

public class DocumentFetcher extends AsyncTask<URL,Void,Document>{
    private int REQUEST_TIME_OUT = 30000;
    private boolean exceptionOccurred = false;
    private IOException ioException;
    private Listener mListener;

    public interface Listener {
        void onDocumentFetchComplete(Document document);
        void onDocumentFetchError(IOException exception);
    }

    public DocumentFetcher(Listener mListener) {
        this.mListener = mListener;
    }

    @Override
    protected Document doInBackground(URL... urls) {
        Document document = null;
        URL url = urls[0];
        try {
            document = Jsoup.connect(url.toString()).timeout(REQUEST_TIME_OUT).get();
        } catch (IOException ioe) {
            exceptionOccurred = true;
            this.ioException = ioe;
            ioe.printStackTrace();
        }
        return document;
    }

    @Override
    protected void onPostExecute(Document document) {
        if(exceptionOccurred)
            mListener.onDocumentFetchError(ioException);
        else
            mListener.onDocumentFetchComplete(document);
    }
}
