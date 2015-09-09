package jacz.peerengine.util.data_synchronization.premade_lists.old;

/**
 * This list contains 5 levels.
 * <p/>
 * Level 0 is the index.
 * <p/>
 * Level 1 is a String with name of a movie
 * <p/>
 * Level 2 is a picture of the movie
 * <p/>
 * Level 3 is a SampleList_1 list with names of actors (level 1)
 * <p/>
 * Level 4 is a SampleList_1 list with all levels (level 5)
 */
public class SampleList_0 /*implements ListAccessor*/ {
//
//    public static class Datum {
//
//        public Datum() {
//            name = null;
//            picturePath = null;
//            actors = new SampleList_1();
//        }
//
//        public Datum(String name, String picturePath, SampleList_1 actors) {
//            this.name = name;
//            this.picturePath = picturePath;
//            this.actors = actors;
//        }
//
//        private String name;
//
//        private String picturePath;
//
//        private SampleList_1 actors;
//    }
//
//    private static String HASH_SEPARATOR = "/";
//
//    private Map<String, Datum> data;
//
//    private BasicServerSynchProgressFactoryInterface basicServerSynchProgressFactoryInterface;
//
//    public SampleList_0() {
//        this(null);
//    }
//
//    public SampleList_0(BasicServerSynchProgressFactoryInterface basicServerSynchProgressFactoryInterface) {
//        data = new HashMap<String, Datum>();
//        this.basicServerSynchProgressFactoryInterface = basicServerSynchProgressFactoryInterface;
//    }
//
//    private String getIdHash(String id) {
//        return id;
//    }
//
//    private String getNameHash(String id, String name) {
//        if (name == null) {
//            return getIdHash(id) + HASH_SEPARATOR;
//        } else {
//            return getIdHash(id) + HASH_SEPARATOR + new MD5().digestAsHex(name);
//        }
//    }
//
//    private String getPictureHash(String id, String picturePath) {
//        if (picturePath == null) {
//            return getIdHash(id) + HASH_SEPARATOR;
//        } else {
//            try {
//                return new MD5().digestAsHex(new File(picturePath));
//            } catch (IOException e) {
//                // file not found or not accessible -> return the "" hash
//                return "";
//            }
//        }
//    }
//
//    private String getActorsHash(String id, SampleList_1 actors, int level) {
//        if (actors == null) {
//            return getIdHash(id) + HASH_SEPARATOR;
//        } else {
//            return getIdHash(id) + HASH_SEPARATOR + new MD5().digestAsHex(actors.getHashList(level));
//        }
//    }
//
//    private ArrayList<String> separateHashes(String hash) {
//        int index = hash.indexOf(HASH_SEPARATOR);
//        String idHash = hash.substring(0, index);
//        String restHash = hash.substring(index + 1, hash.length());
//        ArrayList<String> res = new ArrayList<String>(2);
//        res.add(idHash);
//        res.add(restHash);
//        return res;
//    }
//
//
//    @Override
//    public int getLevelCount() {
//        return 5;
//    }
//
////    @Override
////    public List<Integer> levelExplodesInMoreLevels(int level) {
////        return null;
////    }
//
//    @Override
//    public Collection<String> getHashList(int level) {
//        ArrayList<String> hashList = new ArrayList<String>(data.size());
//        if (level == 0) {
//            for (String id : data.keySet()) {
//                hashList.add(getIdHash(id));
//            }
//        } else if (level == 1) {
//            for (String id : data.keySet()) {
//                hashList.add(getNameHash(id, data.get(id).name));
//            }
//        } else if (level == 2) {
//            for (String id : data.keySet()) {
//                hashList.add(getPictureHash(id, data.get(id).picturePath));
//            }
//        } else if (level == 3) {
//            for (String id : data.keySet()) {
//                hashList.add(getActorsHash(id, data.get(id).actors, 1));
//            }
//        } else {
//            for (String id : data.keySet()) {
//                hashList.add(getActorsHash(id, data.get(id).actors, 4));
//            }
//        }
//        return hashList;
//    }
//
//    @Override
//    public boolean hashEqualsElement(int level) {
//        return level == 0;
//    }
//
//    @Override
//    public TransmissionType getTransmissionType(int level) {
//        if (level == 0) {
//            return TransmissionType.OBJECT;
//        } else if (level == 1) {
//            return TransmissionType.BYTE_ARRAY;
//        } else {
//            return TransmissionType.INNER_LISTS;
//        }
//    }
//
//    @Override
//    public int getInnerListLevel(int level) {
//        if (level == 3) {
//            return 1;
//        } else {
//            return 5;
//        }
//    }
//
//    @Override
//    public ListAccessor getInnerList(int level, String hash) throws ElementNotFoundException {
//        ArrayList<String> separateHashes = separateHashes(hash);
//        String id = separateHashes.get(0);
//        if (data.containsKey(id)) {
//            return data.get(id).actors;
//        } else {
//            throw new ElementNotFoundException();
//        }
//    }
//
//    @Override
//    public boolean containsElement(int level, String hash) {
//        if (level == 0) {
//            return data.containsKey(hash);
//        } else {
//            ArrayList<String> separateHashes = separateHashes(hash);
//            String id = separateHashes.get(0);
//            if (data.containsKey(id)) {
//                Datum datum = data.get(id);
//                if (level == 1) {
//                    return hash.equals(getNameHash(id, datum.name));
//                } else if (level == 2) {
//                    return hash.equals(getPictureHash(id, datum.picturePath));
//                } else if (level == 3) {
//                    return hash.equals(getActorsHash(id, datum.actors, 1));
//                } else {
//                    return hash.equals(getActorsHash(id, datum.actors, 4));
//                }
//            } else {
//                return false;
//            }
//        }
//    }
//
//    @Override
//    public String getElementHash(String hash, int hashLevel, int requestLevel) throws ElementNotFoundException {
//        if (data.containsKey(hash)) {
//            if (requestLevel == 0) {
//                return hash;
//            } else if (requestLevel == 1) {
//                return getNameHash(hash, data.get(hash).name);
//            } else if (requestLevel == 2) {
//                return getPictureHash(hash, data.get(hash).picturePath);
//            } else if (requestLevel == 3) {
//                return getActorsHash(hash, data.get(hash).actors, 1);
//            } else {
//                return getActorsHash(hash, data.get(hash).actors, 4);
//            }
//        } else {
//            throw new ElementNotFoundException();
//        }
//    }
//
//    @Override
//    public Serializable getElementObject(int level, String hash) throws ElementNotFoundException {
//        // the id itself is the hash
//        return hash;
//    }
//
//    @Override
//    public byte[] getElementByteArray(int level, String hash) throws ElementNotFoundException {
//        ArrayList<String> separateHashes = separateHashes(hash);
//        String id = separateHashes.get(0);
//        if (data.containsKey(id)) {
//            Datum datum = data.get(id);
//            if (hash.equals(getNameHash(id, datum.name))) {
//                byte[] idData = Serializer.serialize(id);
//                byte[] nameData = Serializer.serialize(datum.name);
//                return Serializer.addArrays(idData, nameData);
//            } else {
//                throw new ElementNotFoundException();
//            }
//        } else {
//            throw new ElementNotFoundException();
//        }
//    }
//
//    @Override
//    public void addElementAsObject(int level, Object element) {
//        String id = (String) element;
//        if (!data.containsKey(id)) {
//            data.put(id, new Datum());
//        }
//    }
//
//    @Override
//    public void addElementAsByteArray(int level, byte[] data) {
//        MutableOffset offset = new MutableOffset();
//        String id = Serializer.deserializeString(data, offset);
//        String name = Serializer.deserializeString(data, offset);
//        if (!this.data.containsKey(id)) {
//            this.data.put(id, new Datum());
//        }
//        Datum datum = this.data.get(id);
//        datum.name = name;
//    }
//
//    @Override
//    public void eraseElements(int level, Collection<String> hashes) {
//        if (level == 0) {
//            for (String id : hashes) {
//                data.remove(id);
//            }
//        }
//        // we don't need to remove elements at other levels, because they will be overwritten anyway
//    }
//
//    @Override
//    public ProgressNotificationWithError<Integer, String> initiateListSynchronizationAsServer(PeerID clientPeerID, int level) {
//        if (basicServerSynchProgressFactoryInterface != null) {
//            return basicServerSynchProgressFactoryInterface.initialize(clientPeerID, "SampleList_0", level);
//        } else {
//            return null;
//        }
//    }
//
//    public void add(String id, Datum datum) {
//        data.put(id, datum);
//    }
}
