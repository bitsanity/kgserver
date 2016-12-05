package a.kgserver.util;

import java.awt.image.*;
import java.io.*;
import java.security.SecureRandom;
import javax.imageio.ImageIO;

public class Challenge
{
  private byte[] privKey_;
  private byte[] pubKey_;
  private byte[] sig_;

  public Challenge() throws Exception
  {
    privKey_ = new byte[32];
    SecureRandom.getInstance( "SHA1PRNG" ).nextBytes( privKey_ );

    Secp256k1 curve = new Secp256k1();

    if ( !curve.privateKeyIsValid(privKey_) )
      throw new Exception( "invalid private key" );

    pubKey_ = curve.publicKeyCreate( privKey_ );
    sig_ = curve.signECDSA( SHA256.hash(pubKey_), privKey_ );

    if ( !curve.verifyECDSA(sig_, SHA256.hash(pubKey_), pubKey_) )
      throw new Exception( "bad signature" );
  }

  public byte[] pubKey() { return pubKey_; }
  public byte[] privKey() { return privKey_; }

  public String toPNG() throws Exception
  {
    String chall = Base64.encode( ByteOps.concat(pubKey_, sig_) );
    BufferedImage bi = QR.encode( chall, 600 ); // size in pixels

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write( bi, "png", baos );
    byte[] raw = baos.toByteArray();

    return "data:image/png;base64," + Base64.encode( raw );
  }

  public String toString()
  {
    try {
      return new Message( new MessagePart(pubKey_, sig_) ).toString();
    }
    catch( Exception e ) {
      return null;
    }
  }
}

