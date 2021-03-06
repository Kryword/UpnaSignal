/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package upnasignal;

import app.ContactsComponentInterface;
import app.DataToSendSignal;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.ParserConfigurationException;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.agreement.srp.SRP6Client;
import org.bouncycastle.crypto.agreement.srp.SRP6Server;
import static org.bouncycastle.crypto.agreement.srp.SRP6StandardGroups.rfc5054_1024;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.SRP6GroupParameters;

/**
 *
 * @author Kryword
 */
final class SignalTask {

    static final String INTERACTION = "signal";
    static final SRP6GroupParameters PARAMS = rfc5054_1024;
    InetAddress inetAddress;
    final int port;
    DataInputStream in;
    DataOutputStream out;
    final String nickname;
    final String message;
    
    
    SignalTask(final int port, final String nickname, final String message) {
        this.inetAddress = null;
        this.port = port;
        this.nickname = nickname;
        this.in = null;
        this.out = null;
        this.message = message;
    }

    SignalTask(final DataInputStream in, final DataOutputStream out, final String nickname) {
        this.in = in;
        this.out = out;
        this.nickname = nickname;
        this.inetAddress = null;
        this.port = -1;
        this.message = null;
    }

    /**
     * Información adicional: A la hora de generar los secretos se puede utilizar
     * el verificador del otro. Es decir, si tenemos nuestro verificador, hacemos
     * un init con el digest de nuestro verificador y a la hora de calcular el
     * secreto utlizamos calculateSecret con el verificador del otro como argumento
     * @return código ejecución (0 correcto, 1 error)
     */
    
    protected int init(){
        try {
            // Intento obtener los datos del cliente desde la base de datos
            ContactsComponentInterface contactsInterface = new ContactsComponentInterface();
            Future<DataToSendSignal> futureData = contactsInterface.getDataToSendSignal(nickname);
            byte[] saltForSignal = null;
            String myNickname = null;
            try {
                DataToSendSignal data = futureData.get();
                inetAddress = InetAddress.getByAddress(data.getIp());
                saltForSignal = data.getSalt();
                myNickname = data.getMyNickname();
            } catch (InterruptedException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
                return 1;
            } catch (ExecutionException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
                return 1;
            }
            
            
            Socket socket = new Socket(inetAddress, port);
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            
            Messages messages = new Messages();
            // Primera parte del protocolo
            out.writeUTF(messages.getHeadingMessage(nickname, INTERACTION));
            
            String response = in.readUTF();
            Boolean result = messages.parseResultStatusMessage(response);
            Boolean bye = messages.parseBYEMessage(in.readUTF());
            if (!result && bye){
                System.out.println("Cliente cierra conexión.");
                socket.close();
                return 1;
            }
            
            // Segunda parte del protocolo. Cálculo de A
            final SRP6Client client = new SRP6Client();
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            // TODO: Verificador dentro del digest. Obtener desde el otro lado resultados
            client.init(PARAMS, new SHA256Digest(), sr);
            // Estoy usando esa contraseña debido a que aún no hay ninguna implementación para obtener una
            // En el futuro se cambiará por la contraseña introducida por el cliente.
            // TODO: Modificar A para usar verificador. Sigo sin encontrar como se hace
            final BigInteger A = client.generateClientCredentials(saltForSignal, myNickname.getBytes(), "prueba123".getBytes());
            out.writeUTF(messages.getBytesMessage(A.toByteArray())); 
            result = messages.parseResultStatusMessage(response);
            bye = messages.parseBYEMessage(in.readUTF());
            if (!result && bye){
                System.out.println("Cliente cierra conexión.");
                socket.close();
                return 1;
            }
            
            // Tercer paso del protocolo, obtenemos B desde el servicio
            response = in.readUTF();
            final byte[] bBytes = messages.parseBytesMessage(response);
            final BigInteger B = new BigInteger(bBytes);
            // Verifico que B mod N != 0
            if (B.mod(PARAMS.getN()).signum() == 0){
                out.writeUTF(messages.getResultStatusMessage(false));
                out.writeUTF(messages.getBYEMessage());
                socket.close();
                return 1;
            }else{
                out.writeUTF(messages.getResultStatusMessage(true));
                out.writeUTF(messages.getACKMessage());
            }
            
            try {
                // Cuarto paso del protocolo, envío del texto encriptado de señal
                // TODO: Generación del secreto a partir de lo obtenido desde la DB
                final BigInteger clientS = client.calculateSecret(B);
                Cipher encrypter = Cipher.getInstance("AES/CFB/PKCS5Padding");
                final MessageDigest md = MessageDigest.getInstance("SHA-256");
                final byte[] bytes = md.digest(clientS.toByteArray());
                final SecretKeySpec key = new SecretKeySpec(bytes, 0, 1024, "AES");
                encrypter.init(Cipher.ENCRYPT_MODE, key);
                final String signalMessage = messages.getBytesMessage(encrypter.doFinal(message.getBytes(StandardCharsets.UTF_8)));
                out.writeUTF(signalMessage);
            } catch (CryptoException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchPaddingException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidKeyException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalBlockSizeException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BadPaddingException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                result = messages.parseResultStatusMessage(response);
                bye = messages.parseBYEMessage(in.readUTF());
                if (!result && bye){
                    System.out.println("Cliente cierra conexión.");
                    socket.close();
                    return 1;
                }
            }
            return 0;
        }catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 1;
    }
    
    protected int accept() {
        try{
            // Primera parte del protocolo. Verifico si el usuario está en la base de datos y si no devuelvo falso
            Messages messages = new Messages();
            ContactsComponentInterface contactsInterface = new ContactsComponentInterface();
            Boolean result = null;
            try{
                Future<Boolean> future = contactsInterface.isInContacts(nickname);
                Boolean isInContacts = future.get();
                result = isInContacts;
            } catch (InterruptedException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            } catch (ExecutionException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            }finally{
                out.writeUTF(messages.getResultStatusMessage(result));
                if (result){
                    out.writeUTF(messages.getACKMessage());
                }else{
                    out.writeUTF(messages.getBYEMessage());
                    in.close();
                    out.close();
                    return 1;
                }
            }
            // Segunda parte del protocolo, recibo A desde el cliente
            final byte[] aBytes = messages.parseBytesMessage(in.readUTF());
            final BigInteger A = new BigInteger(aBytes);
            if (A.mod(PARAMS.getN()).signum() == 0){
                out.writeUTF(messages.getResultStatusMessage(false));
                out.writeUTF(messages.getBYEMessage());
                in.close();
                out.close();
                return 1;
            }else{
                out.writeUTF(messages.getResultStatusMessage(true));
                out.writeUTF(messages.getACKMessage());
            }
            
            // Tercera parte del protocolo, cálculo de B
            // Primero obtengo el verificador del cliente desde la base de datos
            // Creo un generador de números aleatorios seguro
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            final SRP6Server server = new SRP6Server();
            BigInteger B = null;
            try {
                Future<byte[]> futureVerifier = contactsInterface.getVerifier(nickname);
                byte[] verifier = futureVerifier.get();
                server.init(PARAMS, new BigInteger(verifier), new SHA256Digest(), sr);
                B = server.generateServerCredentials();
                // Envío B al cliente
                out.writeUTF(messages.getBytesMessage(B.toByteArray()));
            } catch (InterruptedException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                result = messages.parseResultStatusMessage(in.readUTF());
                Boolean bye = messages.parseBYEMessage(in.readUTF());
                if (!result && bye){
                    System.out.println("Servicio cierra conexión.");
                    in.close();
                    out.close();
                    return 1;
                }
            }
            
            // Cuarta parte del protocolo, creación de secreto y recepción de mensaje encriptado
            // Desencriptado del mensaje y muestro por consola el resultado
            try {
                // TODO: Generación del secreto a partir de lo obtenido desde la DB
                final BigInteger serverS = server.calculateSecret(B);
                Cipher decrypter = Cipher.getInstance("AES/CFB/PKCS5Padding");
                final MessageDigest md = MessageDigest.getInstance("SHA-256");
                final byte[] bytes = md.digest(serverS.toByteArray());
                final SecretKeySpec key = new SecretKeySpec(bytes, 0, 1024, "AES");
                decrypter.init(Cipher.DECRYPT_MODE, key);
                final String messageRecieved = in.readUTF();
                final byte[] encryptedMessage = messages.parseBytesMessage(messageRecieved);
                final byte[] decryptedMessage = decrypter.doFinal(encryptedMessage);
                final String messageText = new String(decryptedMessage, StandardCharsets.UTF_8);
                System.out.println("Mensaje recibido y desencryptado correctamente: " + messageText);
                result = true;
            } catch (CryptoException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            } catch (NoSuchPaddingException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            } catch (InvalidKeyException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            } catch (IllegalBlockSizeException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            } catch (BadPaddingException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            }finally{
                out.writeUTF(messages.getResultStatusMessage(result));
                out.writeUTF(messages.getBYEMessage());
                in.close();
                out.close();
            }
            return 0;
        } catch (IOException ex) {
            Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 1;
    }
    
}
