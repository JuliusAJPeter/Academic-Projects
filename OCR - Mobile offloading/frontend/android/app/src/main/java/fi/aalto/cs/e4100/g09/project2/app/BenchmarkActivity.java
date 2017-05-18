package fi.aalto.cs.e4100.g09.project2.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import fi.aalto.cs.e4100.g09.project2.R;

public class BenchmarkActivity extends AppCompatActivity {
    private static final String LOG_TAG = BenchmarkActivity.class.getSimpleName();
    private static final String INTENT_DATA_BENCHMARK_STATS = "benchmark_stats";


    public static void start(Activity activity, BenchmarkStats stats) {
        Intent intent = new Intent(activity, BenchmarkActivity.class);
        intent.putExtra(INTENT_DATA_BENCHMARK_STATS, stats);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_benchmark);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();

        final BenchmarkStats stats = intent.getParcelableExtra(INTENT_DATA_BENCHMARK_STATS);
        displayStats(stats);

    }

    private void displayStats(BenchmarkStats stats) {
        Log.i(LOG_TAG, "Received stats: " + stats);

        // In case multiple files are selected, the processing time and the amount of data exchanged
        // should be calculated for each individual image and the frontend should show the
        // corresponding average and standard deviation, as well as the minimum and maximum values
        // along with the related image number (e.g., the index of the element in the selection that
        // resulted in the minimum and maximum values, respectively)

        // For the BENCHMARK operating mode, the frontend shows a screen (page) with
        // the following information:
        StringBuilder sb = new StringBuilder()

                // > the total number of processed images;
                .append("Total number of processed images: ")
                .append(stats.getTotalImagesCount())
                .append("\n\n")

                // > the time taken by local and remote processing in separate sections;
                .append("――――――――――\n")
                .append("LOCAL OCR STATS:\n")
                .append(" • Total time: ")
                .append(formatNiceTime(stats.getTotalProcessingTimeLocal()))
                .append("\n");

        if (stats.isMultipleImages()) {
            sb.append(" • Individual images:\n");
            for (int i = 0; i < stats.getProcessingTimeLocal().size(); i++) {
                sb
                        .append("    - image #").append(i + 1).append(": ")
                        .append(formatNiceTime(stats.getProcessingTimeLocal().get(i)))
                        .append("\n");
            }
            sb
                    .append(" • Mean average: ")
                    .append(formatNiceTime((long) stats.getProcessingTimeLocalAverage()))
                    .append("\n")
                    .append(" • Standard deviation: ")
                    .append(formatNiceTime((long) stats.getProcessingTimeLocalStdDev()))
                    .append("\n");

            int min = stats.findMinimumProcessingTimeLocal();
            int max = stats.findMaximumProcessingTimeLocal();
            if (min >= 0) {
                sb
                        .append(" • Minimum time - image #")
                        .append(min)
                        .append(", ")
                        .append(formatNiceTime(stats.getProcessingTimeLocal().get(min)))
                        .append("\n");
            }
            if (max >= 0) {
                sb
                        .append(" • Maximum time - image #")
                        .append(max)
                        .append(", ")
                        .append(formatNiceTime(stats.getProcessingTimeLocal().get(max)))
                        .append("\n");
            }
        }

        sb
                .append("\n")

                // > the time taken by local and remote processing in separate sections;
                // > the amount of data exchanged with the server as the total number of transferred bytes.
                .append("――――――――――\n")
                .append("REMOTE OCR STATS:\n")
                .append(" • Total time: ")
                .append(formatNiceTime(stats.getTotalProcessingTimeRemote()))
                .append("\n")
                .append(" • Total bytes transferred: ")
                .append(formatNiceBytes(stats.getTotalExchangedBytesRemote()))
                .append("\n");

        if (stats.isMultipleImages()) {
            sb.append(" • Individual images:\n");
            for (int i = 0; i < stats.getProcessingTimeRemote().size(); i++) {
                sb
                        .append("    - image #").append(i + 1).append(": ")
                        .append(formatNiceTime(stats.getProcessingTimeRemote().get(i)))
                        .append(" / ")
                        .append(formatNiceBytes(stats.getExchangedBytesRemote().get(i)))
                        .append("\n");
            }
            sb
                    .append(" • Mean average: ")
                    .append(formatNiceTime((long) stats.getProcessingTimeRemoteAverage()))
                    .append("\n")
                    .append(" • Standard deviation: ")
                    .append(formatNiceTime((long) stats.getProcessingTimeRemoteStdDev()))
                    .append("\n");

            int min = stats.findMinimumProcessingTimeRemote();
            int max = stats.findMaximumProcessingTimeRemote();
            if (min >= 0) {
                sb
                        .append(" • Minimum time - image #")
                        .append(min)
                        .append(", ")
                        .append(formatNiceTime(stats.getProcessingTimeRemote().get(min)))
                        .append("\n");
            }
            if (max >= 0) {
                sb
                        .append(" • Maximum time - image #")
                        .append(max)
                        .append(", ")
                        .append(formatNiceTime(stats.getProcessingTimeRemote().get(max)))
                        .append("\n");
            }

            min = stats.findMinimumExchangedBytesRemote();
            max = stats.findMaximumExchangedBytesRemote();
            if (min >= 0) {
                sb
                        .append(" • Minimum bytes - image #")
                        .append(min)
                        .append(", ")
                        .append(formatNiceBytes(stats.getExchangedBytesRemote().get(min)))
                        .append("\n");
            }
            if (max >= 0) {
                sb
                        .append(" • Maximum bytes - image #")
                        .append(max)
                        .append(", ")
                        .append(formatNiceBytes(stats.getExchangedBytesRemote().get(max)))
                        .append("\n");
            }
        }

        sb.append("\n");

        ((TextView) findViewById(R.id.text)).setText(sb.toString());
    }

    private static String formatNiceTime(long nanoseconds) {
        double seconds = ((double) nanoseconds) / 1e9;
        return String.format("%.3f s", seconds);
    }

    // from http://stackoverflow.com/a/5599842
    private static String formatNiceBytes(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @SuppressWarnings({"WeakerAccess", "unused"})
    public static class BenchmarkStats implements Parcelable {

        private List<Long> processingTimeLocal;
        private List<Long> processingTimeRemote;
        private List<Long> exchangedBytesRemote;
        private long totalProcessingTimeLocal;
        private long totalProcessingTimeRemote;
        private long totalExchangedBytesRemote;

        public BenchmarkStats() {
            this.processingTimeLocal = new ArrayList<>();
            this.processingTimeRemote = new ArrayList<>();
            this.exchangedBytesRemote = new ArrayList<>();
        }

        public int getTotalImagesCount() {
            if (this.processingTimeLocal.size() != this.processingTimeRemote.size()) {
                Log.e(LOG_TAG, "List sizes should match! Is the benchmarking finished?");
            }
            return this.processingTimeLocal.size();
        }

        public void addProcessingTimeLocal(long time) {
            processingTimeLocal.add(time);
        }

        public void addProcessingTimeRemote(long time) {
            processingTimeRemote.add(time);
        }

        public void addExchangedBytesRemote(long bytes) {
            exchangedBytesRemote.add(bytes);
        }

        public void setTotalProcessingTimeLocal(long time) {
            this.totalProcessingTimeLocal = time;
        }

        public void setTotalProcessingTimeRemote(long time) {
            this.totalProcessingTimeRemote = time;
        }

        public void setTotalExchangedBytesRemote(long bytes) {
            this.totalExchangedBytesRemote = bytes;
        }

        public boolean isMultipleImages() {
            return getTotalImagesCount() > 1;
        }

        public List<Long> getProcessingTimeLocal() {
            return processingTimeLocal;
        }

        public List<Long> getProcessingTimeRemote() {
            return processingTimeRemote;
        }

        public List<Long> getExchangedBytesRemote() {
            return exchangedBytesRemote;
        }

        public long getTotalProcessingTimeLocal() {
            return totalProcessingTimeLocal;
        }

        public long getTotalProcessingTimeRemote() {
            return totalProcessingTimeRemote;
        }

        public long getTotalExchangedBytesRemote() {
            return totalExchangedBytesRemote;
        }

        public double getProcessingTimeLocalAverage() {
            return getMeanAvg(processingTimeLocal, getTotalImagesCount());
        }

        public double getProcessingTimeLocalStdDev() {
            return getStdDev(processingTimeLocal, getTotalImagesCount());
        }

        public double getProcessingTimeRemoteAverage() {
            return getMeanAvg(processingTimeRemote, getTotalImagesCount());
        }

        public double getProcessingTimeRemoteStdDev() {
            return getStdDev(processingTimeRemote, getTotalImagesCount());
        }

        public int findMinimumProcessingTimeLocal() {
            return findMinimum(processingTimeLocal, processingTimeLocal.size());
        }

        public int findMaximumProcessingTimeLocal() {
            return findMaximum(processingTimeLocal, processingTimeLocal.size());
        }

        public int findMinimumProcessingTimeRemote() {
            return findMinimum(processingTimeRemote, processingTimeRemote.size());
        }

        public int findMaximumProcessingTimeRemote() {
            return findMaximum(processingTimeRemote, processingTimeRemote.size());
        }

        public int findMinimumExchangedBytesRemote() {
            return findMinimum(exchangedBytesRemote, exchangedBytesRemote.size());
        }

        public int findMaximumExchangedBytesRemote() {
            return findMaximum(exchangedBytesRemote, exchangedBytesRemote.size());
        }

        private double getMeanAvg(Iterable<? extends Number> data, int size) {
            double sum = 0.0;
            for (Number a : data)
                sum += a.doubleValue();
            return sum / size;
        }

        private double getVariance(Iterable<? extends Number> data, int size) {
            double mean = getMeanAvg(data, size);
            double temp = 0;
            for (Number a : data)
                temp += (a.doubleValue() - mean) * (a.doubleValue() - mean);
            return temp / size;
        }

        double getStdDev(Iterable<? extends Number> data, int size) {
            return Math.sqrt(getVariance(data, size));
        }

        private int findMinimum(List<? extends Number> data, int size) {
            int index = -1;
            long minimum = Long.MAX_VALUE;
            for (int i = 0; i < data.size(); i++) {
                Number a = data.get(i);
                if (a.longValue() < minimum) {
                    minimum = a.longValue();
                    index = i;
                }
            }
            return index;
        }

        private int findMaximum(List<? extends Number> data, int size) {
            int index = -1;
            long minimum = Long.MIN_VALUE;
            for (int i = 0; i < data.size(); i++) {
                Number a = data.get(i);
                if (a.longValue() > minimum) {
                    minimum = a.longValue();
                    index = i;
                }
            }
            return index;
        }


        // Implement parcelable
        protected BenchmarkStats(Parcel in) {
            processingTimeLocal = new ArrayList<>();
            in.readList(processingTimeLocal, null);
            processingTimeRemote = new ArrayList<>();
            in.readList(processingTimeRemote, null);
            exchangedBytesRemote = new ArrayList<>();
            in.readList(exchangedBytesRemote, null);
            totalProcessingTimeLocal = in.readLong();
            totalProcessingTimeRemote = in.readLong();
            totalExchangedBytesRemote = in.readLong();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeList(processingTimeLocal);
            dest.writeList(processingTimeRemote);
            dest.writeList(exchangedBytesRemote);
            dest.writeLong(totalProcessingTimeLocal);
            dest.writeLong(totalProcessingTimeRemote);
            dest.writeLong(totalExchangedBytesRemote);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<BenchmarkStats> CREATOR = new Creator<BenchmarkStats>() {
            @Override
            public BenchmarkStats createFromParcel(Parcel in) {
                return new BenchmarkStats(in);
            }

            @Override
            public BenchmarkStats[] newArray(int size) {
                return new BenchmarkStats[size];
            }
        };

        @Override
        public String toString() {
            return "BenchmarkStats{" +
                    "processingTimeLocal=" + processingTimeLocal +
                    ", processingTimeRemote=" + processingTimeRemote +
                    ", exchangedBytesRemote=" + exchangedBytesRemote +
                    ", totalProcessingTimeLocal=" + totalProcessingTimeLocal +
                    ", totalProcessingTimeRemote=" + totalProcessingTimeRemote +
                    ", totalExchangedBytesRemote=" + totalExchangedBytesRemote +
                    '}';
        }
    }

}
