package jacz.peerengine.test;

/**
 * Test personal data
 */
public class PersonalData {

    private String nick;

    private String avatarPath;

    public PersonalData(String nick, String avatarPath) {
        this.nick = nick;
        this.avatarPath = avatarPath;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }
}
