package jacz.peerengineservice.util.data_synchronization.premade_lists.old;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.test.list_synch.ListSynchProgress;
import jacz.peerengineservice.util.data_synchronization.old.*;
import jacz.util.hash.SHA_256;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;

import java.io.Serializable;
import java.util.*;

/**
 * A test list with 4 levels:
 * <p/>
 * Level 0 is a String with the name of a movie, and which hashes are the names themselves
 * <p/>
 * Level 1 is the same movie name but transferred as object
 * <p/>
 * Level 2 is the same movie name but transferred as byte array
 * <p/>
 * Level 3 is an inner list with TestList_1
 * <p/>
 * The hash of an element is always the movie name, in every level
 */
public class TestList_0 implements ListAccessor {

    public static class Movie {

        private final String name;

        private final Set<String> actors;

        public Movie(String name) {
            this(name, new HashSet<String>());
        }

        public Movie(String name, Set<String> actors) {
            this.name = name;
            this.actors = actors;
        }

        @Override
        public String toString() {
            return "Movie{" +
                    "name='" + name + '\'' +
                    ", actors='" + actors.toString() + '\'' +
                    '}';
        }
    }

    private final Map<String, Movie> movies;

    public TestList_0() {
        this(new HashMap<String, Movie>());
    }

//    public TestList_0(Map<String, String> movies) {
//        this.movies = new HashMap<>();
//        for (Map.Entry<String, String> aMovie : movies.entrySet()) {
//            this.movies.put(aMovie.getKey(), new Movie(aMovie.getValue()));
//        }
//    }

    public TestList_0(Map<String, Movie> movies) {
        this.movies = movies;
    }

    @Override
    public int getLevelCount() {
        return 4;
    }

    @Override
    public void beginSynchProcess(Mode mode) {
    }

    @Override
    public Collection<IndexAndHash> getHashList(int level) throws DataAccessException {
        List<IndexAndHash> indexAndHashList = new ArrayList<>();
        for (Map.Entry<String, Movie> movieEntry : movies.entrySet()) {
            String index = movieEntry.getKey();
            Movie movie = movieEntry.getValue();
            if (level < 3) {
                indexAndHashList.add(new IndexAndHash(index, movie.name));
            } else {
                indexAndHashList.add(new IndexAndHash(index, Integer.toString(movie.actors.hashCode())));
            }
        }
        return indexAndHashList;
    }

    @Override
    public boolean hashEqualsElement(int level) {
        return level == 0;
    }

    @Override
    public TransmissionType getTransmissionType(int level) {
        if (level == 1) {
            return TransmissionType.OBJECT;
        } else if (level == 2) {
            return TransmissionType.BYTE_ARRAY;
        } else if (level == 3) {
            return TransmissionType.INNER_LISTS;
        }
        return null;
    }

    @Override
    public List<Integer> getInnerListLevels(int level) {
        List<Integer> levels = new ArrayList<>();
        levels.add(0);
        return levels;
    }

    @Override
    public ListAccessor getInnerList(String index, int level, boolean buildElementIfNeeded) throws ElementNotFoundException, DataAccessException {
        if (!movies.containsKey(index) && buildElementIfNeeded) {
            movies.put(index, new Movie(""));
        }
        if (movies.containsKey(index)) {
            return new TestList_1(movies.get(index).actors);
        } else {
            throw new ElementNotFoundException();
        }
    }

    @Override
    public boolean mustRequestElement(String index, int level, String hash) {
        if (index.equals("d")) {
            movies.put("d", new Movie("ddd"));
            return false;
        }
        return true;
    }

    @Override
    public String getElementHash(String index, int requestLevel) throws ElementNotFoundException {
        if (movies.containsKey(index)) {
            if (requestLevel < 3) {
                return movies.get(index).name;
            } else {
                // inner list
                return new SHA_256().digestAsHex(movies.get(index).actors);
            }
        } else {
            throw new ElementNotFoundException();
        }
    }

    @Override
    public Serializable getElementObject(String index, int level) throws ElementNotFoundException {
        if (movies.containsKey(index)) {
            return movies.get(index).name;
        } else {
            throw new ElementNotFoundException();
        }
    }

    @Override
    public byte[] getElementByteArray(String index, int level) throws ElementNotFoundException {
        if (movies.containsKey(index)) {
            return Serializer.serialize(movies.get(index).name);
        } else {
            throw new ElementNotFoundException();
        }
    }

    @Override
    public int getElementByteArrayLength(String index, int level) throws ElementNotFoundException {
        return getElementByteArray(index, level).length;
    }

    @Override
    public void addElementAsObject(String index, int level, Object element) {
        movies.put(index, new Movie((String) element));
    }

    @Override
    public void addElementAsByteArray(String index, int level, byte[] data) {
        System.out.println("adding " + index);
        movies.put(index, new Movie(Serializer.deserializeString(data, new MutableOffset())));
    }

    @Override
    public boolean mustEraseOldIndexes() {
        return true;
    }

    @Override
    public void eraseElements(Collection<String> indexes) {
        for (String index : indexes) {
            movies.remove(index);
        }
    }

    @Override
    public void endSynchProcess(Mode mode, boolean success) {
    }

    @Override
    public ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID, int level, boolean singleElement) {
//        return new ServerSynchRequestAnswer(ServerSynchRequestAnswer.Type.OK, new ListSynchProgress(clientPeerID, "TestList_0", false));
        return null;
    }
}
