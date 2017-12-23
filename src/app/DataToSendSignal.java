/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package app;

/**
 *
 * @author Kryword
 */
public class DataToSendSignal {
    final String myNickname;
    final byte[] salt;
    final byte[] ip;

    public DataToSendSignal(final String myNickname, final byte[] salt, final byte[] ip) {
        this.myNickname = myNickname;
        this.salt = salt;
        this.ip = ip;
    }

    public String getMyNickname() {
        return myNickname;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getIp() {
        return ip;
    }
}
