package jacz.peerengine.util.data_synchronization.premade_lists.old;

/**
 *
*/
public class SimplePersonalDataWithAvatar /*implements ListAccessor*/ {

//    private PeerID peerID;
//
//    private String nick;
//
//    private SimplePersonalData.State state;
//
//    private String message;
//
//    private String pictureFilePath;
//
//    private String pictureFileHash;
//
//    private FileHashDatabase pictureFileHashDatabase;
//
//    boolean usesPictureFileHashDatabase;
//
//    private ForeignPeerDataActionWithAvatar foreignPeerDataActionWithAvatar;
//
//    public SimplePersonalDataWithAvatar(PeerID peerID, String pictureFilePath, ForeignPeerDataActionWithAvatar foreignPeerDataActionWithAvatar) {
//        this(peerID, "", SimplePersonalData.State.UNDEFINED, "", pictureFilePath, foreignPeerDataActionWithAvatar);
//    }
//
//    public SimplePersonalDataWithAvatar(PeerID peerID, String pictureFileHash, FileHashDatabase pictureFileHashDatabase, ForeignPeerDataActionWithAvatar foreignPeerDataActionWithAvatar) {
//        this(peerID, "", SimplePersonalData.State.UNDEFINED, "", pictureFileHash, pictureFileHashDatabase, foreignPeerDataActionWithAvatar);
//    }
//
//    public SimplePersonalDataWithAvatar(PeerID peerID, String nick, SimplePersonalData.State state, String message, String pictureFilePath) {
//        this(peerID, nick, state, message, pictureFilePath, (ForeignPeerDataActionWithAvatar) null);
//    }
//
//    public SimplePersonalDataWithAvatar(PeerID peerID, String nick, SimplePersonalData.State state, String message, String pictureFileHash, FileHashDatabase pictureFileHashDatabase) {
//        this(peerID, nick, state, message, pictureFileHash, pictureFileHashDatabase, null);
//    }
//
//    public SimplePersonalDataWithAvatar(PeerID peerID, String nick, SimplePersonalData.State state, String message, String pictureFilePath, ForeignPeerDataActionWithAvatar foreignPeerDataActionWithAvatar) {
//        this.peerID = peerID;
//        this.nick = nick;
//        this.state = state;
//        this.message = message;
//        this.pictureFilePath = pictureFilePath;
//        usesPictureFileHashDatabase = false;
//        this.foreignPeerDataActionWithAvatar = foreignPeerDataActionWithAvatar;
//    }
//
//    public SimplePersonalDataWithAvatar(PeerID peerID, String nick, SimplePersonalData.State state, String message, String pictureFileHash, FileHashDatabase pictureFileHashDatabase, ForeignPeerDataActionWithAvatar foreignPeerDataActionWithAvatar) {
//        this.peerID = peerID;
//        this.nick = nick;
//        this.state = state;
//        this.message = message;
//        this.pictureFileHash = pictureFileHash;
//        this.pictureFileHashDatabase = pictureFileHashDatabase;
//        usesPictureFileHashDatabase = true;
//        this.foreignPeerDataActionWithAvatar = foreignPeerDataActionWithAvatar;
//    }
//
//    public static String getListName() {
//        return "SimplePersonalDataWithAvatar";
//    }
//
//    public String getNick() {
//        return nick;
//    }
//
//    public void setNick(String nick) {
//        this.nick = nick;
//    }
//
//    public SimplePersonalData.State getState() {
//        return state;
//    }
//
//    public void setState(SimplePersonalData.State state) {
//        this.state = state;
//    }
//
//    public String getMessage() {
//        return message;
//    }
//
//    public void setMessage(String message) {
//        this.message = message;
//    }
//
//    public String getPictureFilePath() {
//        return pictureFilePath;
//    }
//
//    public void setPictureFilePath(String pictureFilePath) {
//        this.pictureFilePath = pictureFilePath;
//    }
//
//    public String getPictureFileHash() {
//        return pictureFileHash;
//    }
//
//    public void setPictureFileHash(String pictureFileHash) {
//        this.pictureFileHash = pictureFileHash;
//    }
//
//    public FileHashDatabase getPictureFileHashDatabase() {
//        return pictureFileHashDatabase;
//    }
//
//    public void setPictureFileHashDatabase(FileHashDatabase pictureFileHashDatabase) {
//        this.pictureFileHashDatabase = pictureFileHashDatabase;
//    }
//
//    private String getNickHash() {
//        if (nick != null) {
//            return nick;
//        } else {
//            return "";
//        }
//    }
//
//    private String getStateHash() {
//        if (state != null) {
//            // return the State ordinal, it is smaller than a complete hash
//            return String.valueOf(state.ordinal());
//        } else {
//            return "";
//        }
//    }
//
//    private String getMessageHash() {
//        if (message != null) {
//            return new MD5().digestAsHex(message);
//        } else {
//            return "";
//        }
//    }
//
//    private String getPictureHash() {
//        try {
//            if (usesPictureFileHashDatabase) {
//                return pictureFileHash;
//            } else {
//                return new MD5().digestAsHex((new File(pictureFilePath)));
//            }
//        } catch (Exception e) {
//            // file not found or not accessible or null path -> return the "" hash
//            return "";
//        }
//    }
//
//    @Override
//    public int getLevelCount() {
//        return 4;
//    }
//
//    @Override
//    public List<Integer> levelExplodesInMoreLevels(int level) {
//        return null;
//    }
//
//    @Override
//    public Collection<String> getHashList(int level) {
//        List<String> hashes = new ArrayList<String>(1);
//        if (level == 0) {
//            hashes.add(getNickHash());
//        } else if (level == 1) {
//            hashes.add(getStateHash());
//        } else if (level == 2) {
//            hashes.add(getMessageHash());
//        } else {
//            hashes.add(getPictureHash());
//        }
//        return hashes;
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
//            return ListAccessor.TransmissionType.OBJECT;
//        } else {
//            return ListAccessor.TransmissionType.BYTE_ARRAY;
//        }
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
//    @Override
//    public boolean containsElement(int level, String hash) {
//        if (level == 0) {
//            return hash.equals(getNickHash());
//        } else if (level == 1) {
//            return hash.equals(getStateHash());
//        } else if (level == 2) {
//            return hash.equals(getMessageHash());
//        } else {
//            if (usesPictureFileHashDatabase) {
//                if (hash.equals(getPictureHash())) {
//                    return true;
//                } else {
//                    // check if we can get it from the file database
//                    if (pictureFileHashDatabase.containsKey(hash)) {
//                        pictureFileHash = hash;
//                        if (foreignPeerDataActionWithAvatar != null) {
//                            foreignPeerDataActionWithAvatar.newPeerPicture(peerID, pictureFileHashDatabase.getPath(pictureFileHash));
//                        }
//                        return true;
//                    } else {
//                        return false;
//                    }
//                }
//            } else {
//                return hash.equals(getPictureHash());
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
//        if (hash.equals(getNickHash())) {
//            return nick;
//        } else {
//            throw new ElementNotFoundException();
//        }
//    }
//
//    @Override
//    public byte[] getElementByteArray(int level, String hash) throws ElementNotFoundException {
//        if (level == 1) {
//            if (hash.equals(getStateHash())) {
//                return Serializer.serialize(state);
//            } else {
//                throw new ElementNotFoundException();
//            }
//        } else {
//            if (hash.equals(getMessageHash())) {
//                return Serializer.serialize(message);
//            } else {
//                throw new ElementNotFoundException();
//            }
//        }
//    }
//
//    @Override
//    public void addElementAsObject(int level, Object element) {
//        if (level == 0) {
//            nick = (String) element;
//            if (foreignPeerDataActionWithAvatar != null) {
//                foreignPeerDataActionWithAvatar.newPeerNick(peerID, nick);
//            }
//        }
//    }
//
//    @Override
//    public void addElementAsByteArray(int level, byte[] data) {
//        if (level == 1) {
//            state = Serializer.deserializeEnum(SimplePersonalData.State.class, data, new MutableOffset());
//            if (foreignPeerDataActionWithAvatar != null) {
//                foreignPeerDataActionWithAvatar.newPeerState(peerID, state);
//            }
//        } else if (level == 2) {
//            message = Serializer.deserializeString(data, new MutableOffset());
//            if (foreignPeerDataActionWithAvatar != null) {
//                foreignPeerDataActionWithAvatar.newPeerMessage(peerID, message);
//            }
//        } else if (level == 3) {
//            try {
//                if (usesPictureFileHashDatabase) {
//                    pictureFileHash = pictureFileHashDatabase.put(data);
//                    if (foreignPeerDataActionWithAvatar != null) {
//                        foreignPeerDataActionWithAvatar.newPeerPicture(peerID, pictureFileHash, pictureFileHashDatabase);
//                    }
//                } else {
//                    RandomAccess.write(pictureFilePath, 0, data);
//                    if (foreignPeerDataActionWithAvatar != null) {
//                        foreignPeerDataActionWithAvatar.newPeerPicture(peerID, pictureFilePath);
//                    }
//                }
//            } catch (IOException e) {
//                if (foreignPeerDataActionWithAvatar != null) {
//                    foreignPeerDataActionWithAvatar.errorWritingNewPictureFile(peerID, data);
//                }
//            }
//        }
//    }
//
//    @Override
//    public void eraseElements(int level, Collection<String> hashes) {
//        // ignore. Even being clients, we do not really need to erase elements, as they will be overwritten later
//    }
//
//    @Override
//    public ProgressNotificationWithError<Integer, String> initiateListSynchronizationAsServer(PeerID clientPeerID, int level) {
//        return null;
//    }


}
