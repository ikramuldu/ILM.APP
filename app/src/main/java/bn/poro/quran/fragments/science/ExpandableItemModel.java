package bn.poro.quran.fragments.science;

public class ExpandableItemModel {
    public final int level, id;
    public final String title;
    public boolean isExpanded;

    public ExpandableItemModel(int id, String title, int level) {
        this.id = id;
        this.title = title;
        this.level = level;
    }
}
