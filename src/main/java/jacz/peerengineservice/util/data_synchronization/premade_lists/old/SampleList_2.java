package jacz.peerengineservice.util.data_synchronization.premade_lists.old;

/**
 * A list with some non-repeating non-null numbers at its only level...
 */
public class SampleList_2 /*implements ListAccessor*/ {

//    private Set<Integer> numbers;
//
//    private BasicServerSynchProgressFactoryInterface basicServerSynchProgressFactoryInterface;
//
//    public SampleList_2() {
//        numbers = new HashSet<Integer>();
//        this.basicServerSynchProgressFactoryInterface = null;
//    }
//
//    public SampleList_2(BasicServerSynchProgressFactoryInterface basicServerSynchProgressFactoryInterface) {
//        numbers = new HashSet<Integer>();
//        this.basicServerSynchProgressFactoryInterface = basicServerSynchProgressFactoryInterface;
//    }
//
//    public SampleList_2(Set<Integer> numbers) {
//        this.numbers = numbers;
//        this.basicServerSynchProgressFactoryInterface = null;
//    }
//
//    public SampleList_2(Set<Integer> numbers, BasicServerSynchProgressFactoryInterface basicServerSynchProgressFactoryInterface) {
//        this.numbers = numbers;
//        this.basicServerSynchProgressFactoryInterface = basicServerSynchProgressFactoryInterface;
//    }
//
//    @Override
//    public int getLevelCount() {
//        return 1;
//    }
//
//    @Override
//    public List<Integer> levelExplodesInMoreLevels(int level) {
//        return null;
//    }
//
//    @Override
//    public Collection<String> getHashList(int level) {
//        ArrayList<String> hashList = new ArrayList<String>(numbers.size());
//        for (Integer i : numbers) {
//            hashList.add(i.toString());
//        }
//        return hashList;
//    }
//
//    @Override
//    public boolean hashEqualsElement(int level) {
//        return false;
//    }
//
//    @Override
//    public TransmissionType getTransmissionType(int level) {
//        return TransmissionType.OBJECT;
//    }
//
//    @Override
//    public int getInnerListLevel(int level) {
//        // not used
//        return 0;
//    }
//
//    @Override
//    public ListAccessor getInnerList(int level, String hash) throws ElementNotFoundException {
//        // not used
//        return null;
//    }
//
//    private Integer hashToNumber(String hash) {
//        try {
//            return Integer.parseInt(hash);
//        } catch (NumberFormatException e) {
//            return null;
//        }
//    }
//
//    @Override
//    public boolean containsElement(int level, String hash) {
//        Integer value = hashToNumber(hash);
//        return value != null && numbers.contains(value);
//    }
//
//    @Override
//    public String getElementHash(String hash, int hashLevel, int requestLevel) throws ElementNotFoundException {
//        throw new ElementNotFoundException();
//    }
//
//    @Override
//    public Serializable getElementObject(int level, String hash) throws ElementNotFoundException {
//        Integer value = hashToNumber(hash);
//        if (value != null) {
//            return value;
//        } else {
//            throw new ElementNotFoundException();
//        }
//    }
//
//    @Override
//    public byte[] getElementByteArray(int level, String hash) throws ElementNotFoundException {
//        // not used
//        return null;
//    }
//
//    @Override
//    public void addElementAsObject(int level, Object element) {
//        if (element instanceof Integer) {
//            numbers.add((Integer) element);
//        }
//    }
//
//    @Override
//    public void addElementAsByteArray(int level, byte[] data) {
//        // not used
//    }
//
//    @Override
//    public void eraseElements(int level, Collection<String> hashes) {
//        for (String hash : hashes) {
//            Integer value = hashToNumber(hash);
//            if (value != null) {
//                numbers.remove(value);
//            }
//        }
//    }
//
//    @Override
//    public ProgressNotificationWithError<Integer, String> initiateListSynchronizationAsServer(PeerID clientPeerID, int level) {
//        if (basicServerSynchProgressFactoryInterface != null) {
//            return basicServerSynchProgressFactoryInterface.initialize(clientPeerID, "SampleList_2", level);
//        } else {
//            return null;
//        }
//    }
//
//    public void add(int... numbers) {
//        for (int number : numbers) {
//            this.numbers.add(number);
//        }
//    }

}
