package bn.poro.quran.fragments.dua;

class DuaItem {
    final String name, topic, source;
    boolean isExpanded;

    DuaItem(String name, String topic, String source) {
        this.name = name;
        this.topic = topic;
        this.source = source;
    }
}
