package fi.aalto.cs.e4100.g09.project2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

public class HistoryAdapter extends ArrayAdapter<HistoryAdapter.OcrResult> {
    private static final String LOG_TAG = HistoryAdapter.class.getSimpleName();

    public HistoryAdapter(Activity context, List<OcrResult> history) {
        super(context, R.layout.history_item, history);

    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        View v = view;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.history_item, null);
        }

        OcrResult r = getItem(position);

        if (r != null) {
            ImageView iv = (ImageView) v.findViewById(R.id.thumbnail);
            TextView tv = (TextView) v.findViewById(R.id.text);

            if (iv != null) {
                iv.setImageBitmap(r.thumbnail);
            }

            if (tv != null) {
                tv.setText(r.text);
            }
        }

        return v;
    }

    public static class OcrResult {
        public final Bitmap thumbnail;
        public final String text;
        public final String id;
        public final Date timestamp;

        public OcrResult(Bitmap thumbnail, String text, String id, Date timestamp) {
            this.thumbnail = thumbnail;
            this.text = text.replace("\\n", "\n");
            this.id = id;
            this.timestamp = timestamp;
        }

        public OcrResult(String thumbnailB64, String text, String id, Date timestamp) {
            this(AndroidUtils.decodeB64Bitmap(thumbnailB64), text, id, timestamp);
        }
    }
}