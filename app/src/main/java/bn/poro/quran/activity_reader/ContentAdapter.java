package bn.poro.quran.activity_reader;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.shockwave.pdfium.PdfDocument;

import java.util.ArrayList;
import java.util.List;

import bn.poro.quran.R;
import bn.poro.quran.activity_reader.pdfviewer.PDFView;

class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.Holder> {
    final PDFActivity activity;
    final ArrayList<BookmarkWrap> bookmarkWraps;

    public ContentAdapter(PDFActivity activity, List<PdfDocument.Bookmark> bookmarks) {
        this.activity = activity;
        bookmarkWraps = new ArrayList<>();
        for (PdfDocument.Bookmark bookmark : bookmarks) {
            this.bookmarkWraps.add(new BookmarkWrap(bookmark, (byte) 0));
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(activity).inflate(R.layout.content_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        BookmarkWrap bookmarkWrap = bookmarkWraps.get(position);
        holder.itemView.setPadding((int) (bookmarkWrap.level * holder.textView.getPaddingBottom()*1.5), 0, 0, 0);
        holder.textView.setText(bookmarkWrap.bookmark.getTitle());
        if (bookmarkWrap.bookmark.hasChildren()) {
            holder.icon.setImageResource(R.drawable.expand);
            if (bookmarkWrap.isExpanded) holder.icon.setRotation(90);
            else holder.icon.setRotation(0);
        }else holder.icon.setImageResource(R.drawable.ic_dot);
    }

    @Override
    public int getItemCount() {
        return bookmarkWraps.size();
    }

    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView textView;
        final ImageView icon;

        public Holder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            icon = itemView.findViewById(R.id.icon);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getLayoutPosition();
            BookmarkWrap bookmarkWrap = bookmarkWraps.get(position);
            if (bookmarkWrap.bookmark.hasChildren()) {
                if (bookmarkWrap.isExpanded) {
                    icon.setAnimation(AnimationUtils.loadAnimation(activity,R.anim.rotate_back90));
                    int size = 0;
                    for (int i = position + 1; i < bookmarkWraps.size(); i++)
                        if (bookmarkWraps.get(i).level > bookmarkWrap.level) size++;
                        else break;
                    bookmarkWraps.subList(position + 1, position + size + 1).clear();
                    notifyItemRangeRemoved(position + 1, size);
                    bookmarkWrap.isExpanded = false;
                } else {
                    icon.setAnimation(AnimationUtils.loadAnimation(activity,R.anim.rotate90));
                    List<PdfDocument.Bookmark> childs = bookmarkWrap.bookmark.getChildren();
                    ArrayList<BookmarkWrap> subMenu = new ArrayList<>();
                    for (PdfDocument.Bookmark child : childs)
                        subMenu.add(new BookmarkWrap(child, (byte) (bookmarkWrap.level + 1)));
                    bookmarkWraps.addAll(position + 1, subMenu);
                    notifyItemRangeInserted(position + 1, childs.size());
                    bookmarkWrap.isExpanded = true;
                }
            } else {
                PDFView pdfView = activity.findViewById(R.id.pdf_view);
                pdfView.jumpTo((int) bookmarkWrap.bookmark.getPageIdx());
                DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        }
    }
}
