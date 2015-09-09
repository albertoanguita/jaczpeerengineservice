package jacz.peerengine.util.data_synchronization.old;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A class representing the path to a specific list (from the main list) and a level. It always contains the main list name and its level, and then an
 * optional list of IndexAndLevel objects, which represent the element of the previous list and the level of the inner list, if any.
 * Objects of this class allow representing synchronization tasks. Together with a hash (stored externally) we can represent either a level or a
 * specific element to be synched
 * <p/>
 * For example, if we want to represent the path to level 1 of the inner list contained in the element with index "index2" and level 3 of
 * the main list called "mainList1", we would have:
 * - mainList = "mainList1"
 * - mainListLevel = 3
 * - innerLists = {{"index2", 1}}
 */
class ListPath implements Serializable {

    public static class IndexAndLevel implements Serializable {

        /**
         * Index of the previous list where this inner list is located
         */
        final String index;

        final int level;

        public IndexAndLevel(String index, int level) {
            this.index = index;
            this.level = level;
        }
    }

    String mainList;

    int mainListLevel;

    List<IndexAndLevel> innerLists;

    ListPath(String mainList, int mainListLevel) {
        this(mainList, mainListLevel, new ArrayList<IndexAndLevel>(0));
    }

    ListPath(String mainList, int mainListLevel, List<IndexAndLevel> innerLists) {
        this.mainList = mainList;
        this.mainListLevel = mainListLevel;
        this.innerLists = innerLists;
    }

    ListPath(ListPath listPath, IndexAndLevel additionalInnerList) {
        this(listPath.mainList, listPath.mainListLevel, addOneInnerList(listPath.innerLists, additionalInnerList));
    }

    private static List<IndexAndLevel> addOneInnerList(List<IndexAndLevel> innerLists, IndexAndLevel additionalInnerList) {
        ArrayList<IndexAndLevel> newInnerLists = new ArrayList<>(innerLists);
        newInnerLists.add(additionalInnerList);
        return newInnerLists;
    }

    boolean isMainList() {
        return innerLists.isEmpty();
    }
}
