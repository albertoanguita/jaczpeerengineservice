package jacz.peerengineservice.test.list_synch;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

import java.io.Serializable;
import java.util.*;

/**
 * Created by Alberto on 18/09/2015.
 */
public class TestData_0 implements DataAccessor {

    public static class Movie implements Comparable<Movie>, Serializable {

        private final String id;

        private boolean deleted;

        private Long timestamp;

        private String name;

        private Set<String> actors;

        public Movie(String id, boolean deleted, Long timestamp, String name) {
            this(id, deleted, timestamp, name, new HashSet<String>());
        }

        public Movie(String id, boolean deleted, Long timestamp, String name, Set<String> actors) {
            this.id = id;
            this.deleted = deleted;
            this.timestamp = timestamp;
            this.name = name;
            this.actors = actors;
        }

        @Override
        public String toString() {
            return "Movie{" +
                    "id='" + id + '\'' +
                    ", timestamp='" + timestamp + '\'' +
                    ", name='" + name + '\'' +
                    ", actors='" + actors.toString() + '\'' +
                    '}';
        }

        @Override
        public int compareTo(Movie o) {
            return timestamp.compareTo(o.timestamp);
        }
    }


    private String databaseID;

    private Set<Movie> movies;

    public TestData_0(String databaseID, Set<Movie> movies) {
        this.databaseID = databaseID;
        this.movies = movies;
    }

    private List<Movie> getOrderedMovies() {
        List<Movie> orderedMovies = new ArrayList<>(movies);
        Collections.sort(orderedMovies);
        return orderedMovies;
    }

    @Override
    public String getName() {
        return "list0";
    }

    @Override
    public void beginSynchProcess(Mode mode) {
        System.out.println("TestData_0: begin");
    }

    @Override
    public String getDatabaseID() {
        return databaseID;
    }

    @Override
    public void setDatabaseID(String databaseID) {
        this.databaseID = databaseID;
    }

    @Override
    public Long getLastTimestamp() throws DataAccessException {
        List<Movie> orderedMovies = getOrderedMovies();
        return orderedMovies.get(orderedMovies.size() - 1).timestamp;
    }

    @Override
    public List<? extends Serializable> getElementsFrom(long fromTimestamp) throws DataAccessException {
        List<Movie> orderedMovies =  getOrderedMovies();
        while (!orderedMovies.isEmpty() && orderedMovies.get(0).timestamp < fromTimestamp) {
            orderedMovies.remove(0);
        }
        return orderedMovies;
    }

    @Override
    public int elementsPerMessage() {
        return 5;
    }

    @Override
    public int CRCBytes() {
        return 4;
    }

    @Override
    public void setElement(Object element) throws DataAccessException {
        Movie receivedMovie = (Movie) element;
        Iterator<Movie> i = movies.iterator();
        while (i.hasNext()) {
            Movie movie = i.next();
            if (movie.id.equals(receivedMovie.id)) {
                if (receivedMovie.deleted) {
                    i.remove();
                } else {
                    movie.timestamp = receivedMovie.timestamp;
                    movie.name = receivedMovie.name;
                    movie.actors = receivedMovie.actors;
                }
                return;
            }
        }
        // new movie
        movies.add(receivedMovie);
    }

    @Override
    public void endSynchProcess(Mode mode, boolean success) {
        System.out.println("TestData_0: end");
    }

    @Override
    public ProgressNotificationWithError<Integer, SynchError> getServerSynchProgress(PeerId clientPeerId) {
        return new ListSynchProgress(clientPeerId, "TestList_0", false);
    }
}
