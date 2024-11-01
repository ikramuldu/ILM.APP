package bn.poro.quran.fragments.audio_load;

public class IsolatedItem {
    public final String  fileName;
    public final int size, nameRes;
    public IsolatedItem(int name, String fileName, int size) {
        this.nameRes = name;
        this.fileName = fileName;
        this.size = size;
    }
}
