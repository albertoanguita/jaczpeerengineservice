package jacz.peerengineservice.test.list_synch;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.ServerSynchRequestAnswer;

import java.io.Serializable;
import java.util.*;

/**
 * Created by Alberto on 18/09/2015.
 */
public class TestData_0 implements DataAccessor {

    public static class Movie implements Comparable<Movie>, Serializable {

        private final String id;

        private boolean deleted;

        private Integer timestamp;

        private String name;

        private Set<String> actors;

        public Movie(String id, boolean deleted, Integer timestamp, String name) {
            this(id, deleted, timestamp, name, new HashSet<String>());
        }

        public Movie(String id, boolean deleted, Integer timestamp, String name, Set<String> actors) {
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


    private Set<Movie> movies;

    public TestData_0(Set<Movie> movies) {
        this.movies = movies;
    }

    private List<Movie> getOrderedMovies() {
        List<Movie> orderedMovies = new ArrayList<>(movies);
        Collections.sort(orderedMovies);
        return orderedMovies;
    }

    @Override
    public void beginSynchProcess(Mode mode) {
        System.out.println("TestData_0: begin");
    }

    @Override
    public Integer getLastTimestamp() throws DataAccessException {
        List<Movie> orderedMovies = getOrderedMovies();
        return orderedMovies.get(orderedMovies.size() - 1).timestamp;
    }

    @Override
    public List<? extends Serializable> getElements(Integer latestClientTimestamp) throws DataAccessException {
        List<Movie> orderedMovies =  getOrderedMovies();
        while (!orderedMovies.isEmpty() && orderedMovies.get(0).timestamp <= latestClientTimestamp) {
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
    public ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID) {
        return new ServerSynchRequestAnswer(ServerSynchRequestAnswer.Type.OK, new ListSynchProgress(clientPeerID, "TestList_0", false));
    }
}
