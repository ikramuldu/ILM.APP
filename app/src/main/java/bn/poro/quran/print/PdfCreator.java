package bn.poro.quran.print;


import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.view.View;
import android.widget.TextView;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;

import bn.poro.quran.L;
import bn.poro.quran.R;
import bn.poro.quran.views.WordGroup;

public class PdfCreator extends Thread {
    private static final int PAGE_PADDING = 20;
    final int WIDTH = 2480 / 3;
    final int HEIGHT = 3508 / 3;
    private final PrintQuranAdapter adapter;
    private final Listener listener;
    private boolean cancelled;

    public void cancel() {
        cancelled = true;
    }

    interface Listener {
        void onFinish();

        void onProgress(int progress);
    }

    PdfCreator(PrintQuranAdapter printQuranAdapter, Listener listener) {
        this.adapter = printQuranAdapter;
        this.listener = listener;
    }

    @Override
    public void run() {
        PrintQuranAdapter.Holder[] holder = new PrintQuranAdapter.Holder[3];
        holder[0] = adapter.onCreateViewHolder(null, 0);
        holder[1] = adapter.onCreateViewHolder(null, 1);
        holder[2] = adapter.onCreateViewHolder(null, 1);
        PdfDocument document = new PdfDocument();
        int pageNumber = 1;
        PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, pageNumber).create());
        Canvas canvas = page.getCanvas();
        int pageHeight = PAGE_PADDING;
        for (int i = 0; i < adapter.getItemCount() && !cancelled; i++)
            try {
                if (i % 10 == 0) listener.onProgress(i);
                int type = adapter.getItemViewType(i);
                adapter.bindViewHolder(holder[type], i);
                holder[type].itemView.measure(
                        View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                int viewHeight = holder[type].itemView.getMeasuredHeight();
                holder[type].itemView.layout(0, 0, WIDTH, viewHeight);
                int availableHeight = HEIGHT - pageHeight - PAGE_PADDING;
                if (availableHeight >= viewHeight) {
                    pageHeight = pageHeight + viewHeight;
                    holder[type].itemView.draw(canvas);
                    canvas.translate(0, viewHeight);
                } else {
                    int bottomType = type == 1 ? 2 : 1;
                    View topView = splitView(holder[type].itemView, holder[bottomType].itemView, availableHeight);
                    if (topView != null) topView.draw(canvas);
                    document.finishPage(page);
                    page = document.startPage(new PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, pageNumber++).create());
                    canvas = page.getCanvas();
                    if (topView == null) {
                        i--;
                        if (type != 0) {
                            pageHeight = PAGE_PADDING;
                            canvas.translate(0, PAGE_PADDING);
                        } else pageHeight = 0;
                    } else {
                        canvas.translate(0, PAGE_PADDING);
                        holder[bottomType].itemView.measure(
                                View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                        viewHeight = holder[bottomType].itemView.getMeasuredHeight();
                        holder[bottomType].itemView.layout(0, 0, WIDTH, viewHeight);
                        holder[bottomType].itemView.draw(canvas);
                        holder[bottomType].itemView.findViewById(R.id.num_bar).setVisibility(View.VISIBLE);
                        holder[bottomType].itemView.findViewById(R.id.group).setVisibility(View.VISIBLE);
                        View view = holder[type].itemView.findViewById(R.id.trans);
                        if (view != null) view.setVisibility(View.VISIBLE);
                        pageHeight = PAGE_PADDING + viewHeight;
                        canvas.translate(0, viewHeight);
                    }
                }
            } catch (Exception e) {
                L.d(e);
                break;
            }
        File file = new File(holder[0].itemView.getContext().getCacheDir() + "/quran.pdf");
        try {
            document.finishPage(page);
            OutputStream outputStream = Files.newOutputStream(file.toPath());
            document.writeTo(outputStream);
            document.close();
        } catch (Exception e) {
            L.d(e);
        }
        new Handler(Looper.getMainLooper()).post(listener::onFinish);
    }

    private @Nullable View splitView(View topView, View bottomView, int available) {
        if (topView.getId() == R.id.head) return null;
        WordGroup topGroup = topView.findViewById(R.id.group);
        if (topGroup == null) return null;
        available -= topGroup.getTop();
        if (topGroup.getChildCount() > 0 && available < topGroup.getChildAt(0).getBottom())
            return null;
        bottomView.findViewById(R.id.num_bar).setVisibility(View.GONE);
        WordGroup bottomGroup = bottomView.findViewById(R.id.group);
        TextView topTrans = topView.findViewById(R.id.trans);
        if (available >= topGroup.getHeight()) {//split translation
            if (topTrans == null) {
                bottomGroup.removeAllViews();
                return topView;
            }
            CharSequence text = topTrans.getText();
            TextView bottomTrans = bottomView.findViewById(R.id.trans);
            bottomTrans.setText(text);
            bottomGroup.setVisibility(View.GONE);
            available -= topGroup.getHeight();
            Layout layout = topTrans.getLayout();
            int lineCount = layout.getLineCount();
            int lineNum = 0;
            for (; lineNum < lineCount; lineNum++) {
                if (layout.getLineBottom(lineNum) > available) break;
            }
            lineNum--;
            if (lineNum < 0) {
                topTrans.setVisibility(View.GONE);
            } else {
                int offset = layout.getOffsetForHorizontal(lineNum, WIDTH);
                topTrans.setText(text.subSequence(0, offset));
                bottomTrans.setText(text.subSequence(offset, text.length()));
            }
            return topView;
        }
        bottomGroup.removeAllViews();
        if (topTrans != null)
            topTrans.setVisibility(View.GONE);
        for (int i = 0; i < topGroup.getChildCount(); i++) {
            View child = topGroup.getChildAt(i);
            if (child.getBottom() > available) {
                while (i < topGroup.getChildCount()) {
                    child = topGroup.getChildAt(i);
                    topGroup.removeViewAt(i);
                    bottomGroup.addView(child);
                }
                break;
            }
        }
        return topView;
    }
}
