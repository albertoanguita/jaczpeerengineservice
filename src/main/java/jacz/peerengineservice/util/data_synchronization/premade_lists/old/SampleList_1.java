package jacz.peerengineservice.util.data_synchronization.premade_lists.old;

/**
 * a List with 6 possible levels.
 * <p/>
 * Level 0 is a local identifier, always not null and not repeated.
 * <p/>
 * Level 1 are names of persons.
 * Level 2 is their last name.
 * Level 3 is age.
 * Level 4 is an inner list with the numbers of those persons.
 * Level 5 explodes in levels 3 and 4
 * <p/>
 * All fields except the id can be null
 * <p/>
 * Data are indexed by the hash of level 0. Hashes of levels 1 and 2 will include 0-level hash.
 */
public class SampleList_1 /*implements ListAccessor*/ {

//    public static class Datum {
//
//        public Datum() {
//            name = null;
//            lastName = null;
//            age = null;
//            numbers = new SampleList_2();
//        }
//
//        public Datum(String name, String lastName, Integer age, SampleList_2 numbers) {
//            this.name = name;
//            this.lastName = lastName;
//            this.age = age;
//            this.numbers = numbers;
//        }
//
//        private String name;
//
//        private String lastName;
//
//        private Integer age;
//
//        private SampleList_2 numbers;
//    }
//
//    private static String HASH_SEPARATOR = "/";
//
//    private Map<String, Datum> data;
//
//    private BasicServerSynchProgressFactoryInterface basicServerSynchProgressFactoryInterface;
//
//    public SampleList_1() {
//        data = new HashMap<String, Datum>();
//        basicServerSynchProgressFactoryInterface = null;
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
//    private String getAgeHash(String id, Integer age) {
//        if (age == null) {
//            return getIdHash(id) + HASH_SEPARATOR;
//        } else {
//            return getIdHash(id) + HASH_SEPARATOR + new MD5().digestAsHex(age);
//        }
//    }
//
//    private String getNumbersHash(String id, SampleList_2 numbers) {
//        if (numbers == null) {
//            return getIdHash(id) + HASH_SEPARATOR;
//        } else {
//            return getIdHash(id) + HASH_SEPARATOR + new MD5().digestAsHex(numbers.getHashList(0));
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
//        return 6;
//    }
//
//    @Override
//    public List<Integer> levelExplodesInMoreLevels(int level) {
//        if (level == 5) {
//            List<Integer> explodedLevels = new ArrayList<Integer>(2);
//            explodedLevels.add(0);
//            explodedLevels.add(1);
//            explodedLevels.add(2);
//            explodedLevels.add(3);
//            explodedLevels.add(4);
//            return explodedLevels;
//        } else {
//            return null;
//        }
//    }
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
//                hashList.add(getNameHash(id, data.get(id).lastName));
//            }
//        } else if (level == 3) {
//            for (String id : data.keySet()) {
//                hashList.add(getAgeHash(id, data.get(id).age));
//            }
//        } else {
//            for (String id : data.keySet()) {
//                hashList.add(getNumbersHash(id, data.get(id).numbers));
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
//            return TransmissionType.OBJECT;
//        } else if (level == 2) {
//            return TransmissionType.OBJECT;
//        } else if (level == 3) {
//            return TransmissionType.BYTE_ARRAY;
//        } else {
//            return TransmissionType.INNER_LISTS;
//        }
//    }
//
//    @Override
//    public int getInnerListLevel(int level) {
//        return 0;
//    }
//
//    /*@Override
//    public String getInnerListName(int level, String hash) {
//        checkCorrectLevel(level);
//        if (level == 4) {
//            return BasicListContainer.generateInnerListName(name, level, hash);
//        } else {
//        }
//    }*/
//
//    @Override
//    public ListAccessor getInnerList(int level, String hash) throws ElementNotFoundException {
//        ArrayList<String> separateHashes = separateHashes(hash);
//        String id = separateHashes.get(0);
//        if (data.containsKey(id)) {
//            return data.get(id).numbers;
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
//                    return hash.equals(getNameHash(id, datum.lastName));
//                } else if (level == 3) {
//                    return hash.equals(getAgeHash(id, datum.age));
//                } else {
//                    return hash.equals(getNumbersHash(id, datum.numbers));
//                }
//            } else {
//                return false;
//            }
//        }
//    }
//
//    @Override
//    public String getElementHash(String hash, int hashLevel, int requestLevel) throws ElementNotFoundException {
//        throw new ElementNotFoundException();
//    }
//
//    @Override
//    public Serializable getElementObject(int level, String hash) throws ElementNotFoundException {
//        if (level == 0) {
//            return hash;
//        } else {
//            ArrayList<String> separateHashes = separateHashes(hash);
//            String id = separateHashes.get(0);
//            if (data.containsKey(id)) {
//                Datum datum = data.get(id);
//                if (level == 1) {
//                    if (hash.equals(getNameHash(id, datum.name))) {
//                        return new ObjectListWrapper(id, datum.name);
//                    } else {
//                        throw new ElementNotFoundException();
//                    }
//                } else if (level == 2) {
//                    if (hash.equals(getNameHash(id, datum.lastName))) {
//                        return new ObjectListWrapper(id, datum.lastName);
//                    } else {
//                        throw new ElementNotFoundException();
//                    }
//                } else {
//                    throw new ElementNotFoundException();
//                }
//            } else {
//                throw new ElementNotFoundException();
//            }
//        }
//    }
//
//    @Override
//    public byte[] getElementByteArray(int level, String hash) throws ElementNotFoundException {
//        if (level == 3) {
//            ArrayList<String> separateHashes = separateHashes(hash);
//            String id = separateHashes.get(0);
//            if (data.containsKey(id)) {
//                Datum datum = data.get(id);
//                if (hash.equals(getAgeHash(id, datum.age))) {
//                    byte[] idData = Serializer.serialize(id);
//                    byte[] ageData = Serializer.serialize(datum.age);
//                    return Serializer.addArrays(idData, ageData);
//                } else {
//                    throw new ElementNotFoundException();
//                }
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
//        if (level == 0) {
//            String id = (String) element;
//            if (!data.containsKey(id)) {
//                data.put(id, new Datum());
//            }
//        } else {
//            ObjectListWrapper objects = (ObjectListWrapper) element;
//            String id = (String) objects.getObjects().get(0);
//            if (!data.containsKey(id)) {
//                data.put(id, new Datum());
//            }
//            Datum datum = data.get(id);
//            if (level == 1) {
//                datum.name = (String) objects.getObjects().get(1);
//            } else {
//                datum.lastName = (String) objects.getObjects().get(1);
//            }
//        }
//    }
//
//    @Override
//    public void addElementAsByteArray(int level, byte[] data) {
//        if (level == 3) {
//            MutableOffset offset = new MutableOffset();
//            String id = Serializer.deserializeString(data, offset);
//            Integer age = Serializer.deserializeInt(data, offset);
//            if (!this.data.containsKey(id)) {
//                this.data.put(id, new Datum());
//            }
//            Datum datum = this.data.get(id);
//            datum.age = age;
//        }
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
//            return basicServerSynchProgressFactoryInterface.initialize(clientPeerID, "SampleList_1", level);
//        } else {
//            return null;
//        }
//    }
//
//    public void add(String id, Datum datum) {
//        data.put(id, datum);
//    }
}
