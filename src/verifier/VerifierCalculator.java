package verifier;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import static org.bouncycastle.crypto.agreement.srp.SRP6StandardGroups.rfc5054_1024;
import org.bouncycastle.crypto.agreement.srp.SRP6VerifierGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.SRP6GroupParameters;

/**
 *
 * @author Cristian Berner
 * 
 * Clase encargada de generar un verificador a partir del nickname, contraseña 
 * del usuario y la sal. Para ello utilizo los métodos disponibles
 * en las librerías de bouncycastle.
 */
public class VerifierCalculator {
    public byte[] getVerifier(final String nickname, final String password, final byte[] salt) throws NoSuchAlgorithmException{
        
        final SRP6GroupParameters params = rfc5054_1024;
        
        final byte[] I = nickname.getBytes();
        final byte[] P = password.getBytes();
        
        final SRP6VerifierGenerator gen = new SRP6VerifierGenerator();
        
        gen.init(params, new SHA256Digest());
        
        final BigInteger verifier = gen.generateVerifier(salt, I, P);
        
        return verifier.toByteArray();
    }
}
