package bn.poro.quran.fragments.name;

class NameItem {
    public final String name, source;
    public final boolean expandable;
    public boolean isExpanded;

    NameItem(String name, String source, boolean expandable) {
        this.name = name;
        this.source = source;
        this.expandable = expandable;
    }
}
