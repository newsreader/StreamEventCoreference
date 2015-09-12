package eu.newsreader.eventcoreference.objects;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: kyoto
 * Date: 11/29/13
 * Time: 4:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class PhraseCount  implements Serializable {

    private String phrase;
    private Integer count;

    public PhraseCount(String phrase, Integer count) {
        this.phrase = phrase;
        this.count = count;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public void addCount(Integer count) {
        this.count += count;
    }

    public void incrementCount() {
        this.count++;
    }

    public String getPhrase() {
        return phrase;
    }
    public String getPhraseCount() {
        return phrase+":"+count;
    }
    public String toString () {
        return getPhraseCount();
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    static public class Compare implements Comparator {
        public int compare (Object aa, Object bb) {
            PhraseCount a = (PhraseCount) aa;
            PhraseCount b = (PhraseCount) bb;
            if (a.getCount() < b.getCount()) {
                return 1;
            }
            else if (a.getCount()> b.getCount()) {
                return -1;
            }
            else {
                return a.getPhrase().compareTo(b.getPhrase());
            }
        }
    }
}
