/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verifier;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import static org.bouncycastle.crypto.agreement.srp.SRP6StandardGroups.rfc5054_1024;
import org.bouncycastle.crypto.agreement.srp.SRP6VerifierGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.SRP6GroupParameters;

/**
 *
 * @author Kryword
 */
public class VerifierCalculator {
    public byte[] getVerifier(final String nickname, final String password, final byte[] salt){
        
        final SRP6GroupParameters params = rfc5054_1024;
        
        final byte[] I = nickname.getBytes();
        final byte[] P = password.getBytes();
        
        final SRP6VerifierGenerator gen = new SRP6VerifierGenerator();
        
        gen.init(params, new SHA256Digest());
        
        final BigInteger verifier = gen.generateVerifier(salt, I, P);
        
        return verifier.toByteArray();
    }
}
